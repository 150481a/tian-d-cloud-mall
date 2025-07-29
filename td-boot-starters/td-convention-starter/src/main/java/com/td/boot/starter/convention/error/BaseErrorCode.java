package com.td.boot.starter.convention.error;

import lombok.AllArgsConstructor;

/**
 * 通用錯誤碼枚舉。
 * 模塊代碼為 100 (通用/系統基礎模塊)。
 */
@AllArgsConstructor
public enum BaseErrorCode implements IErrorCode {

    // --- 通用系統異常 (100-01-xxx) ---
    SUCCESS("000-00-000", "操作成功", 200), // 特殊碼，表示成功
    INTERNAL_SERVER_ERROR("100-01-001", "系統繁忙，請稍後再試", 500),
    SERVICE_UNAVAILABLE("100-01-002", "服務暫時不可用，請稍後再試", 503),
    REQUEST_TOO_FREQUENT("100-01-003", "請求過於頻繁，請稍後再試", 429),
    UNKNOWN_ERROR("100-01-004", "未知系統錯誤", 500),

    // --- 參數校驗失敗 (100-02-xxx) ---
    INVALID_PARAMETER("100-02-001", "請求參數無效", 400),
    MISSING_PARAMETER("100-02-002", "缺少必要的請求參數", 400),
    PARAMETER_FORMAT_ERROR("100-02-003", "請求參數格式錯誤", 400),
    ILLEGAL_ARGUMENT("100-02-004", "非法參數", 400),

    // --- 權限/認證失敗 (100-05-xxx) ---
    UNAUTHORIZED("100-05-001", "未經授權，請登錄", 401),
    FORBIDDEN("100-05-002", "權限不足，禁止訪問", 403),
    INVALID_TOKEN("100-05-003", "無效的認證憑證", 401),
    TOKEN_EXPIRED("100-05-004", "認證憑證已過期", 401),

    // --- 資源未找到 (100-04-xxx) ---
    RESOURCE_NOT_FOUND("100-04-001", "請求的資源不存在", 404),

    // --- 重複提交 (100-10-xxx) ---
    DUPLICATE_SUBMISSION("100-10-001", "請勿重複提交", 409)
    ;

    private final String code;
    private final String message;
    private final Integer httpStatus;

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public Integer getHttpStatus() {
        return this.httpStatus;
    }

    // 根據 Code 查找錯誤碼，方便後續擴展和應用
    public static BaseErrorCode fromCode(String code) {
        for (BaseErrorCode errorCode : BaseErrorCode.values()) {
            if (errorCode.getCode().equals(code)) {
                return errorCode;
            }
        }
        return UNKNOWN_ERROR; // 如果找不到，返回未知錯誤
    }
}
