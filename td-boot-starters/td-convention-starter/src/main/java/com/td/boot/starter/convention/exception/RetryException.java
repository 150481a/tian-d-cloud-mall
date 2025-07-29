package com.td.boot.starter.convention.exception;

import com.td.boot.starter.convention.error.BaseErrorCode;
import com.td.boot.starter.convention.error.IErrorCode;
import lombok.EqualsAndHashCode;

/**
 * 如需异常重试，则抛出此异常
 *
 * @author paulG
 * @since 2022/4/26
 **/
@EqualsAndHashCode(callSuper = true)
public class RetryException extends BizException {

    private static final long serialVersionUID = 7886918292771470846L;

    public RetryException(IErrorCode errorCode) {
        this(null, null, errorCode);
    }

    public RetryException(String message) {
        this(message, null, BaseErrorCode.UNKNOWN_ERROR);
    }

    public RetryException(String message, IErrorCode errorCode) {
        this(message, null, errorCode);
    }


    public RetryException(String message, Throwable throwable, IErrorCode errorCode) {
        super(errorCode, message, throwable);
    }


}
