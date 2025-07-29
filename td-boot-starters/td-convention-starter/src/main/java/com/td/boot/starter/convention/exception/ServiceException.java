package com.td.boot.starter.convention.exception;


import com.td.boot.starter.convention.error.BaseErrorCode;
import com.td.boot.starter.convention.error.IErrorCode;

import java.util.Optional;

/**
 * 服务端错误异常
 */

public class ServiceException extends BizException {

    public static final String DEFAULT_MESSAGE = "网络错误，请稍后重试！";


    /**
     * 异常消息
     */
    private String msg = DEFAULT_MESSAGE;
    /**
     * 错误码
     */
    private IErrorCode resultCode;

    public String getMsg() {
        return msg;
    }

    public IErrorCode getResultCode() {
        return resultCode;
    }

    public ServiceException(String message) {
        this(message, null, BaseErrorCode.UNKNOWN_ERROR);
    }

    public ServiceException(IErrorCode errorCode) {
        this(null, errorCode);
    }

    public ServiceException(String message, IErrorCode errorCode) {
        this(message, null, errorCode);
    }

    public ServiceException(String message, Throwable throwable) {
        this(message, throwable,BaseErrorCode.UNKNOWN_ERROR);
    }

    public ServiceException(String message, Throwable throwable, IErrorCode errorCode) {
        super(errorCode, Optional.ofNullable(message).orElse(errorCode.getMessage()), throwable);
    }


}
