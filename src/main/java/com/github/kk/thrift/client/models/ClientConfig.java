package com.github.kk.thrift.client.models;

/**
 * @author zhangkai
 *
 */
public class ClientConfig {
    private String zkNamespace;//注册到zookeeper节点的路径
    private String zkAddrs;//zookeeper节点地址
    private int requestTimeout; //毫秒
    
    public String getZkNamespace() {
        return zkNamespace;
    }
    public void setZkNamespace(String zkNamespace) {
        this.zkNamespace = zkNamespace;
    }
    public String getZkAddrs() {
        return zkAddrs;
    }
    public void setZkAddrs(String zkAddrs) {
        this.zkAddrs = zkAddrs;
    }
    public int getRequestTimeout() {
        return requestTimeout;
    }
    public void setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }
}
