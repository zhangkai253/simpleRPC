package com.github.kk.thrift.client.demo;

import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.protocol.TProtocol;

import com.github.kk.thrift.client.AbstractThriftClient;
import com.github.kk.thrift.client.models.ClientConfig;
import com.github.kk.thrift.client.models.RPCRequest;
import com.github.kk.thrift.client.models.RPCResponse;

/**
 * @author zhangkai
 *
 */
public class DemoClient extends AbstractThriftClient{
    private final static String ID_SERVICE = "id_service";

    public DemoClient(ClientConfig clientConfig) {
        super(clientConfig);
    }

    public long genId() {
        RPCRequest rpcRequest = new RPCRequest();
        rpcRequest.setServiceName(ID_SERVICE);
        RPCResponse response = this.sendRequest(rpcRequest);
        if(response.getCode() == RPCResponse.FAILED){
            return -1;
        }else{
            return (Long)response.getData();
        }
    }

    @Override
    protected RPCResponse doService(RPCRequest request, TProtocol protocol) throws Exception {
        RPCResponse response = new RPCResponse();
        String serviceName = request.getServiceName();
        if(serviceName.equals(ID_SERVICE)){
            IdGenerator.Client client = new IdGenerator.Client(new TMultiplexedProtocol(protocol, "idprocessor"));
            long id = client.genId();
            response.setData(id);
            return response;
        }else{
            response.setCode(RPCResponse.FAILED);
            return response;
        }
    }
}
