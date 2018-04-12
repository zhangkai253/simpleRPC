package com.github.kk.thrift.client;
/**
 * @author zhangkai
 *
 */
public class TServerInfo {
    private String host;
    private int port;
    
    public TServerInfo(String host, int port){
        this.host = host;
        this.port = port;
    }
    public String getHost() {
        return host;
    }
    public void setHost(String host) {
        this.host = host;
    }
    public int getPort() {
        return port;
    }
    public void setPort(int port) {
        this.port = port;
    }
}
