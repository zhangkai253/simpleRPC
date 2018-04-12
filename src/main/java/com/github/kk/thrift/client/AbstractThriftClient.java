package com.github.kk.thrift.client;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.shaded.com.google.common.collect.Lists;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFastFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import com.github.kk.thrift.client.models.ClientConfig;
import com.github.kk.thrift.client.models.RPCRequest;
import com.github.kk.thrift.client.models.RPCResponse;
import com.github.kk.thrift.client.models.RegistryInfo;
import com.github.kk.thrift.client.utils.JsonUtils;
import com.github.kk.thrift.client.utils.LogUtils;
/**
 * @author zhangkai
 * 抽象的thrift client,内置socket连接池以及线程池，提供同步阻塞式调用和超时调用
 * 具体thrift client需要继承该类并实现其中的抽象方法并按照需要重写相关方法
 */
public abstract class AbstractThriftClient {
    private final static int MAX_FRAME_SIZE = 1024 * 1024 * 1024;
    private final static int MIN_FRAME_SIZE = 1024;
    
    protected ThreadPoolExecutor executor;
    protected AbstractThriftClient client = this;
    protected ClientConfig clientConfig;
    protected CuratorFramework zkClient;
    protected List<TConnectionPool> shardInfos = Lists.newArrayList();
    
    /**
     * AbstractThriftClient的构造函数
     * 初始化线程池、连接池以及服务发现机制
     */
    protected AbstractThriftClient(ClientConfig clientConfig) {        
        int processors = Runtime.getRuntime().availableProcessors(); 
        this.executor = new ThreadPoolExecutor(processors * 5, processors * 10, 60L, TimeUnit.SECONDS, 
                new ArrayBlockingQueue<Runnable>(processors * 100),
                Executors.defaultThreadFactory(), new ThreadPoolExecutor.CallerRunsPolicy());
        this.clientConfig = clientConfig;
        this.zkClient = CuratorFrameworkFactory.builder()
                .connectString(clientConfig.getZkAddrs())
                .retryPolicy(new ExponentialBackoffRetry(500, 4)).build();
        this.zkClient.start();
        buildConnPool();
    }
    
    /**
     * 唯一需要上层实现的抽象类
     * 该方法接收封装好的RPCRequest
     * 调用真实的RPC请求
     * 将RPC服务返回的结果打包成RPCResponse
     * 上层的具体thrift client实例需要实现该方法
     */
    protected abstract RPCResponse doService(RPCRequest rpcRequest, TProtocol protocol) throws Exception;
    
    /**
     * 从连接池中选择连接的方法，
     * 上层可以重写该方法，实现自己的hash规则
     */
    protected int hashRule(RPCRequest request){
        Random rand = new Random();
        return rand.nextInt(shardInfos.size());
    }
    
    /**
     * processRequest方法处理流程：
     * 1、从连接池中获取连接
     * 2、创建相应的Transport协议结构
     * 3、调用doService方法获取RPC的返回结果
     * @param rpcRequest
     * @return
     */
    protected RPCResponse processRequest(RPCRequest rpcRequest){
        String serviceName = rpcRequest.getServiceName();
        RPCResponse response = new RPCResponse();
        if(serviceName == null){
            LogUtils.warn("serviceName can not be null");
            response.setCode(RPCResponse.FAILED);
            return response;
        }
        TConnectionPool connPool = getConnPool(rpcRequest);
        if(connPool == null){
            response.setCode(RPCResponse.FAILED);
            return response;
        }
        TSocket socket = connPool.getSocket();
        try {
            TTransport transport = new TFastFramedTransport(socket, MIN_FRAME_SIZE, MAX_FRAME_SIZE);
            if (!transport.isOpen()) {
                transport.open();
            }
            TProtocol protocol = new TBinaryProtocol(transport);
            return this.doService(rpcRequest, protocol);
        } catch (Exception e) {
            LogUtils.error("", e);
            connPool.removeSocket(socket);
            response.setCode(RPCResponse.FAILED);
            return response;
        } finally {
            if (socket.isOpen()) {
                connPool.returnSocket(socket);
            }
        }
    }
    
    protected RPCResponse sendRequest(RPCRequest request){
        if(clientConfig.getRequestTimeout() <= 0){
            return this.processRequest(request);
        }else{
            return this.processRequestTimeout(request, clientConfig.getRequestTimeout());
        }
    }
    
    private TConnectionPool getConnPool(RPCRequest request){
        if(shardInfos.size() <= 0){
            LogUtils.warn("no valid node available");
            return null;
        }
        int index = hashRule(request);
        return shardInfos.get(index % shardInfos.size());
    }
    
    private RPCResponse processRequestTimeout(RPCRequest request, int timeout){
        RPCRequestTask rpcRequestTask = new RPCRequestTask(request);
        Future<RPCResponse> future = executor.submit(rpcRequestTask);
        
        try {
            RPCResponse response = future.get(clientConfig.getRequestTimeout(), TimeUnit.MILLISECONDS);
            return response;
        } catch (InterruptedException e) {
            LogUtils.warn("[ExecutorService]The current thread was interrupted while waiting: ", e);
            RPCResponse response = new RPCResponse();
            response.setCode(RPCResponse.FAILED);
            return response;
        } catch (ExecutionException e) {
            LogUtils.warn("[ExecutorService]The computation threw an exception: ", e);
            RPCResponse response = new RPCResponse();
            response.setCode(RPCResponse.FAILED);
            return response;
        } catch (TimeoutException e) {
            LogUtils.warn("[ExecutorService]The wait " + this.clientConfig.getRequestTimeout() + " timed out: ", e);
            RPCResponse response = new RPCResponse();
            response.setCode(RPCResponse.FAILED);
            return response;
        } catch(Exception e){
            LogUtils.warn("[ExecutorService] failed", e);
            RPCResponse response = new RPCResponse();
            response.setCode(RPCResponse.FAILED);
            return response;
        }
    }
        
    private class RPCRequestTask implements Callable<RPCResponse> {
        private RPCRequest rpcRequest;
        
        public RPCRequestTask(RPCRequest request) {
            this.rpcRequest = request;
        }
        
        @Override
        public RPCResponse call() {
            return client.processRequest(rpcRequest);
        }
    };
    
    private void buildConnPool(){
        try{
            List<String> nodes = zkClient
                    .getChildren()
                    .usingWatcher(new Watcher(){
                        @Override
                        public void process(WatchedEvent event) {
                            if(event.getType() == EventType.NodeChildrenChanged){
                                buildConnPool();
                            }
                        }})
                    .forPath(clientConfig.getZkNamespace());
            List<TConnectionPool> currShardInfos = Lists.newArrayList();
            for(String node : nodes){
                String path = clientConfig.getZkNamespace() + "/" + node;
                byte[] dataArray = zkClient.getData().forPath(path);
                String dataStr = new String(dataArray);
                RegistryInfo info = JsonUtils.fromJson(dataStr, RegistryInfo.class);
                TServerInfo server = new TServerInfo(info.getIp(), info.getPort());
                currShardInfos.add(new TConnectionPool(server));
            }
            this.shardInfos = currShardInfos;
        }catch(Exception e){
            LogUtils.error("build conn pool failed", e);
        }
    }
}
