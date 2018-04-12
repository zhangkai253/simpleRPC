package com.github.kk.thrift.client;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.thrift.transport.TSocket;

import com.github.kk.thrift.client.utils.LogUtils;
/**
 * @author zhangkai
 *
 */
public class TPoolObjectFactory extends BasePooledObjectFactory<TSocket> {
    private final static int DEFAULT_TIMEOUT = 20000;
    
    private TServerInfo serverInfo;
    private int timeOut;

    public TPoolObjectFactory(TServerInfo server, int timeOut) {
            super();
            this.timeOut = timeOut;
    }
    
    public TPoolObjectFactory(TServerInfo server) {
        super();
        this.timeOut = DEFAULT_TIMEOUT;
        this.serverInfo = server;
    }
    @Override
    public TSocket create() throws Exception {
        try {
            TSocket socket = new TSocket(serverInfo.getHost(), serverInfo.getPort(), this.timeOut);
            socket.open();
            return socket;
        } catch (Exception e) {
            LogUtils.error("error ThriftPoolableObjectFactory()", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public PooledObject<TSocket> wrap(TSocket obj) {
        return new DefaultPooledObject<TSocket>(obj);
    }

    @Override
    public void destroyObject(PooledObject<TSocket> p) throws Exception {
        TSocket socket = p.getObject();
        if (socket.isOpen()) {
            socket.close();
        }
    }

    @Override
    public boolean validateObject(PooledObject<TSocket> p) {
        Boolean isAvailable = false;
        TSocket socket = p.getObject();
        try {  
            if (socket.isOpen()) {  
                isAvailable =  true;  
            }  
        } catch (Exception e) {  
            return isAvailable;  
        }          
        return isAvailable;  
    }

    @Override
    public void activateObject(PooledObject<TSocket> p) throws Exception {
        TSocket socket = p.getObject();
        if(!socket.isOpen()){
            socket.open();
        }
    }

    @Override
    public void passivateObject(PooledObject<TSocket> p) throws Exception {
        //TTransport tTransport = p.getObject();
    }
}