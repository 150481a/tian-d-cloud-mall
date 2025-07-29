package com.td.boot.starter.convention.exception;

import com.td.boot.starter.convention.error.BaseErrorCode;
import com.td.boot.starter.convention.error.IErrorCode;

public  class BizException extends RuntimeException {
    public BizException(String message, IErrorCode errorCode, Object[] args) {
        super(message);
        this.errorCode = errorCode;
        this.args = args;
    }

    private final IErrorCode errorCode;
    private final Object[] args; // 用於攜帶錯誤消息中的參數

    /**
     * 使用 IErrorCode 創建業務異常。
     *
     * @param errorCode 錯誤碼實現
     */
    public BizException(IErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.args = null;
    }

    /**
     * 使用 IErrorCode 和具體消息創建業務異常。
     * 適用於需要覆蓋 IErrorCode 默認消息的情況。
     *
     * @param errorCode 錯誤碼實現
     * @param message   具體錯誤信息
     */
    public BizException(IErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.args = null;
    }

    /**
     * 使用 IErrorCode 和參數創建業務異常。
     * 消息可以使用佔位符，例如 "參數 {0} 無效"。
     *
     * @param errorCode 錯誤碼實現
     * @param args      用於格式化錯誤消息的參數
     */
    public BizException(IErrorCode errorCode, Object... args) {
        super(errorCode.getMessage()); // 這裡先用原始消息，後續可在響應處理中格式化
        this.errorCode = errorCode;
        this.args = args;
    }

    /**
     * 使用 IErrorCode、具體消息和參數創建業務異常。
     *
     * @param errorCode 錯誤碼實現
     * @param message   具體錯誤信息（可包含佔位符）
     * @param args      用於格式化錯誤消息的參數
     */
    public BizException(IErrorCode errorCode, String message, Object... args) {
        super(message);
        this.errorCode = errorCode;
        this.args = args;
    }

    /**
     * 獲取業務異常的錯誤碼。
     *
     * @return 實現 IErrorCode 接口的錯誤碼對象
     */
    public IErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * 獲取用於格式化錯誤消息的參數。
     *
     * @return 參數數組
     */
    public Object[] getArgs() {
        return args;
    }

    /**
     * 方便地拋出一個通用錯誤碼的 BizException。
     *
     * @param errorCode 通用錯誤碼
     * @return BizException 實例
     */
    public static BizException of(BaseErrorCode errorCode) {
        return new BizException(errorCode);
    }

    /**
     * 方便地拋出一個帶有自定義消息的通用錯誤碼 BizException。
     *
     * @param errorCode 通用錯誤碼
     * @param message   自定義錯誤消息
     * @return BizException 實例
     */
    public static BizException of(BaseErrorCode errorCode, String message) {
        return new BizException(errorCode, message);
    }

    /**
     * 方便地拋出一個帶有參數的通用錯誤碼 BizException。
     *
     * @param errorCode 通用錯誤碼
     * @param args      參數
     * @return BizException 實例
     */
    public static BizException of(BaseErrorCode errorCode, Object... args) {
        return new BizException(errorCode, args);
    }
}
