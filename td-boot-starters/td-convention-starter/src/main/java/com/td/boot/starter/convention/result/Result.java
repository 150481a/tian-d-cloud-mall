package com.td.boot.starter.convention.result;

import com.td.boot.starter.convention.error.BaseErrorCode;
import com.td.boot.starter.convention.error.IErrorCode;
import lombok.Data;

import java.io.Serializable;
import java.text.MessageFormat;

/**
 * 統一響應格式。
 * 用於封裝所有 API 請求的結果，提供統一的成功、失敗及數據結構。
 *
 * @param <T> 響應數據的泛型類型
 */
@Data
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 請求是否成功。
     */
    private boolean success;

    /**
     * 響應碼。遵循 CCC-TT-SSS 格式。
     */
    private String code;

    /**
     * 響應消息。
     */
    private String message;

    /**
     * 響應數據。
     */
    private T data;

    /**
     * 響應時間戳。
     */
    private long timestamp;

    // 私有構造函數，強制使用靜態工廠方法
    private Result() {
        this.timestamp = System.currentTimeMillis();
    }

    private Result(boolean success, String code, String message, T data) {
        this(); // 調用私有無參構造函數設置時間戳
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // --- 靜態工廠方法用於創建 CommonResult 實例 ---

    /**
     * 創建一個成功的響應，不帶數據。
     * 狀態碼為 CommonErrorCode.SUCCESS。
     *
     * @return 成功的 CommonResult
     */
    public static <T> Result<T> success() {
        return new Result<>(true, BaseErrorCode.SUCCESS.getCode(), BaseErrorCode.SUCCESS.getMessage(), null);
    }

    /**
     * 創建一個成功的響應，帶有數據。
     * 狀態碼為 CommonErrorCode.SUCCESS。
     *
     * @param data 響應數據
     * @param <T>  數據類型
     * @return 成功的 CommonResult
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(true, BaseErrorCode.SUCCESS.getCode(), BaseErrorCode.SUCCESS.getMessage(), data);
    }

    /**
     * 創建一個失敗的響應，基於 IErrorCode。
     *
     * @param errorCode 錯誤碼實現
     * @param <T>       數據類型
     * @return 失敗的 CommonResult
     */
    public static <T> Result<T> failed(IErrorCode errorCode) {
        return new Result<>(false, errorCode.getCode(), errorCode.getMessage(), null);
    }

    /**
     * 創建一個失敗的響應，基於 IErrorCode 和自定義消息。
     *
     * @param errorCode 錯誤碼實現
     * @param message   自定義錯誤消息
     * @param <T>       數據類型
     * @return 失敗的 CommonResult
     */
    public static <T> Result<T> failed(IErrorCode errorCode, String message) {
        return new Result<>(false, errorCode.getCode(), message, null);
    }

    /**
     * 創建一個失敗的響應，基於 IErrorCode 和格式化消息參數。
     * 錯誤消息將根據 MessageFormat 進行格式化。
     *
     * @param errorCode 錯誤碼實現
     * @param args      用於格式化錯誤消息的參數
     * @param <T>       數據類型
     * @return 失敗的 CommonResult
     */
    public static <T> Result<T> failed(IErrorCode errorCode, Object... args) {
        // 使用 MessageFormat 格式化消息
        String formattedMessage = MessageFormat.format(errorCode.getMessage(), args);
        return new Result<>(false, errorCode.getCode(), formattedMessage, null);
    }

    /**
     * 創建一個失敗的響應，基於 IErrorCode、自定義消息和格式化參數。
     * 錯誤消息將根據 MessageFormat 進行格式化。
     *
     * @param errorCode 錯誤碼實現
     * @param message   自定義錯誤消息（可包含佔位符）
     * @param args      用於格式化錯誤消息的參數
     * @param <T>       數據類型
     * @return 失敗的 CommonResult
     */
    public static <T> Result<T> failed(IErrorCode errorCode, String message, Object... args) {
        // 使用 MessageFormat 格式化自定義消息
        String formattedMessage = MessageFormat.format(message, args);
        return new Result<>(false, errorCode.getCode(), formattedMessage, null);
    }



    // 可選：為了方便單元測試或特定場景的構造，提供全參數的 Builder 模式
    // 在實際應用中，通常推薦使用上面的靜態工廠方法
    public static class Builder<T> {
        private boolean success;
        private String code;
        private String message;
        private T data;

        public Builder<T> success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder<T> code(String code) {
            this.code = code;
            return this;
        }

        public Builder<T> message(String message) {
            this.message = message;
            return this;
        }

        public Builder<T> data(T data) {
            this.data = data;
            return this;
        }

        public Result<T> build() {
            return new Result<>(success, code, message, data);
        }
    }
}
