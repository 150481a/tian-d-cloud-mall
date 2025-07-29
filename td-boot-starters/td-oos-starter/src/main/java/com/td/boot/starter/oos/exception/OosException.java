package com.td.boot.starter.oos.exception;

/**
 * OOS 服務相關的運行時異常。
 */
public class OosException  extends RuntimeException{
    public OosException() {
        super();
    }

    public OosException(String message) {
        super(message);
    }

    public OosException(String message, Throwable cause) {
        super(message, cause);
    }

    public OosException(Throwable cause) {
        super(cause);
    }
}
