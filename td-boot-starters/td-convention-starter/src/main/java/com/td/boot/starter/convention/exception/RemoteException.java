package com.td.boot.starter.convention.exception;


import com.td.boot.starter.convention.error.BaseErrorCode;
import com.td.boot.starter.convention.error.IErrorCode;

/**
 * 远程服务调用异常
 */
public class RemoteException extends BizException{

    public RemoteException(String message) {
        this(message, null,  BaseErrorCode.UNKNOWN_ERROR);
    }

    public RemoteException(String message, IErrorCode errorCode) {
        this(message, null, errorCode);
    }

    public RemoteException(String message, Throwable throwable, IErrorCode errorCode) {
        super(errorCode, message, throwable);
    }


}
