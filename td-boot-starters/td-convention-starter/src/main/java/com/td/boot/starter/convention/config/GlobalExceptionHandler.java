package com.td.boot.starter.convention.config;

import com.td.boot.starter.convention.error.BaseErrorCode;
import com.td.boot.starter.convention.exception.BizException;
import com.td.boot.starter.convention.result.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * 處理業務異常 BizException。
     * 這是我們自定義的業務錯誤。
     */
    @ExceptionHandler(BizException.class)
    public Result<?> handleBizException(BizException e) {
        // 如果 BizException 攜帶了格式化參數，這裡可以再次格式化消息
        String message = e.getMessage();
        if (e.getArgs() != null && e.getArgs().length > 0) {
            // 注意：這裡假設 BizException 的 message 已經是適合 MessageFormat 的格式
            // 否則需要在 BizException 內部處理格式化
        }
        return Result.failed(e.getErrorCode(), message);
    }

    /**
     * 處理 @RequestBody 參數校驗失敗的異常 (MethodArgumentNotValidException)。
     * 通常用於 POST/PUT 請求中的 JSON 體校驗。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return Result.failed(BaseErrorCode.INVALID_PARAMETER, errorMessage);
    }

    /**
     * 處理 @ModelAttribute 參數校驗失敗的異常 (BindException)。
     * 通常用於 GET 請求中的表單參數校驗。
     */
    @ExceptionHandler(BindException.class)
    public Result<?> handleBindException(BindException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return Result.failed(BaseErrorCode.INVALID_PARAMETER, errorMessage);
    }

    /**
     * 處理 @RequestParam, @PathVariable 或服務層 @Validated 方法參數校驗失敗的異常 (ConstraintViolationException)。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<?> handleConstraintViolationException(ConstraintViolationException e) {
        String errorMessage = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        return Result.failed(BaseErrorCode.INVALID_PARAMETER, errorMessage);
    }

    /**
     * 處理方法參數類型轉換失敗的異常 (MethodArgumentTypeMismatchException)。
     * 例如，期望一個數字但傳遞了字符串。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<?> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String errorMessage = String.format("參數 '%s' 類型錯誤，期望類型為 %s",
                e.getName(), e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知類型");
        return Result.failed(BaseErrorCode.PARAMETER_FORMAT_ERROR, errorMessage);
    }

    /**
     * 處理所有其他未捕獲的 RuntimeException。
     * 作為最終的備用異常處理器。
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e) {
        // 打印堆棧信息，以便調試和追溯
        e.printStackTrace();
        return Result.failed(BaseErrorCode.INTERNAL_SERVER_ERROR, "服務器內部錯誤: " + e.getMessage());
    }

    /**
     * 處理所有未捕獲的頂層 Exception。
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        e.printStackTrace();
        return Result.failed(BaseErrorCode.UNKNOWN_ERROR, "發生未知錯誤: " + e.getMessage());
    }
}
