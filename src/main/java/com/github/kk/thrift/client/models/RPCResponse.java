package com.github.kk.thrift.client.models;

/**
 * @author zhangkai
 *
 */
public class RPCResponse {
    
    public static final int SUCCEED = 1;
    public static final int FAILED = -1;

    private int code = SUCCEED;
    private String msg;
    private Object data;
    
    public int getCode() {
        return code;
    }
    public void setCode(int code) {
        this.code = code;
    }
    public String getMsg() {
        return msg;
    }
    public void setMsg(String msg) {
        this.msg = msg;
    }
    public Object getData() {
        return data;
    }
    public void setData(Object data) {
        this.data = data;
    }
}
