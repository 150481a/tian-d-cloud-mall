package com.td.boot.starter.convention.exception;


import com.td.boot.starter.convention.error.BaseErrorCode;
import com.td.boot.starter.convention.error.IErrorCode;

/**
 * 客户端异常
 *
 */
public class ClientException extends BizException {
    public ClientException(IErrorCode errorCode) {
        this(null, null, errorCode);
    }

    public ClientException(String message) {
        this(message, null, BaseErrorCode.UNKNOWN_ERROR);
    }

    public ClientException(String message, IErrorCode errorCode) {
        this(message, null, errorCode);
    }

    public ClientException(String message, Throwable throwable, IErrorCode errorCode) {
        super(errorCode, message, throwable);
    }

  
}
