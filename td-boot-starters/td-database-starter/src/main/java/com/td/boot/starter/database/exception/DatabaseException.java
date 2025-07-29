package com.td.boot.starter.database.exception;

/**
 * 數據庫相關的運行時異常。
 */
public class DatabaseException extends RuntimeException {

    public DatabaseException() {
        super();
    }

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }

    public DatabaseException(Throwable cause) {
        super(cause);
    }
}
