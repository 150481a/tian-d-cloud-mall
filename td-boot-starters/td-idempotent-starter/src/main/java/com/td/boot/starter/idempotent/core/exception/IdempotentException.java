package com.td.boot.starter.idempotent.core.exception;

/**
 * 冪等性異常。
 * 當檢測到重複請求時拋出。
 */
public class IdempotentException  extends RuntimeException {

    public IdempotentException(String message) {
        super(message);
    }

    public IdempotentException(String message, Throwable cause) {
        super(message, cause);
    }
}
