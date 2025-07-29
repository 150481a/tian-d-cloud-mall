package com.td.boot.starter.convention.error;

/**
 * 錯誤碼接口。
 * 所有業務或通用錯誤碼枚舉都應實現此接口。
 */
public interface  IErrorCode {

    /**
     * 獲取錯誤碼字符串。
     * 格式應遵循 CCC-TT-SSS (模塊碼-類型碼-具體碼)。
     *
     * @return 錯誤碼字符串
     */
    String getCode();

    /**
     * 獲取錯誤信息描述。
     *
     * @return 錯誤信息
     */
    String getMessage();

    /**
     * 獲取對應的 HTTP 狀態碼 (可選)。
     * 如果不需要特定的 HTTP 狀態碼，可以返回 null 或 200。
     *
     * @return HTTP 狀態碼
     */
    Integer getHttpStatus();
}
