package com.github.kk.thrift.client.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zhangkai
 *
 */
public class LogUtils {
    private static ThreadLocal<String> userIdLocal = new ThreadLocal<String>(){
        @Override
        public String initialValue() {  
            return null;  
        }  
    };

    private static Logger info = LoggerFactory.getLogger("info");

    private static Logger warn = LoggerFactory.getLogger("warn");

    private static Logger error = LoggerFactory.getLogger("error");
    
    private static Logger stat = LoggerFactory.getLogger("stat");
    
    public static void setContextUserId(String userId){
        userIdLocal.set(userId);
    }

    public static void info(String message) {
        info.info(contextMessage(message));
    }
    
    public static void stat(String message) {
        stat.info(contextMessage(message));
    }

    public static void warn(String message, Throwable e) {
        warn.warn(contextMessage(message), e);
    }
    
    public static void warn(String message) {
        warn.warn(contextMessage(message));
    }
    
    public static void error(String message) {
        error.error(contextMessage(message));
    }

    public static void error(String message, Throwable e) {
        error.error(contextMessage(message), e);
    }
    
    private static String contextMessage(String message){
        return "USERINFO: " + userIdLocal.get() + "|" + message;
    }
}
