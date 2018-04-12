package com.github.kk.thrift.client;

import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.thrift.transport.TSocket;

import com.github.kk.thrift.client.utils.LogUtils;
/**
 * @author zhangkai
 *
 */
public class TConnectionPool {
    /** 连接对象缓存池 */  
    private ObjectPool<TSocket> objectPool = null;  
  
    public TConnectionPool(TServerInfo server){
        this(new GenericObjectPoolConfig(), new TPoolObjectFactory(server));
    }
    
    public TConnectionPool(GenericObjectPoolConfig config, PooledObjectFactory<TSocket> factory){
        config.setTestOnBorrow(true);
        objectPool = new GenericObjectPool<TSocket>(factory, config);
    }
    
    public TSocket getSocket() {  
        try {  
            TSocket socket = objectPool.borrowObject();              
            return socket;  
        } catch (Exception e) {  
            throw new RuntimeException("error getSocket()", e);  
        }  
    }  
  
    public void returnSocket(TSocket socket) {  
        try {  
            objectPool.returnObject(socket);  
        } catch (Exception e) {  
            throw new RuntimeException("error returnSocket()", e);  
        }  
    }
    
    public void removeSocket(TSocket socket){
        try {
            objectPool.invalidateObject(socket);
        } catch (Exception e) {
            LogUtils.error("removeSocket failed", e);
        }
    }

    public void destroy() {  
        try {  
            objectPool.close();  
        } catch (Exception e) {  
            LogUtils.error("error destory" ,e);
        }  
    }  

}
