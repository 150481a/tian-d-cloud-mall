package com.td.boot.starter.log.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.td.boot.starter.log.properties.LogProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * API 接口日誌切面。
 * 攔截控制器和服務層方法，記錄方法入參、出參、執行耗時和異常。
 */
@Aspect
@Component
@Slf4j
@Order(1)
public class ApiLogAspect {

    private final LogProperties logProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApiLogAspect(LogProperties logProperties) {
        this.logProperties = logProperties;
    }

    /**
     * 定義 API 日誌的切點。
     * 這裡硬編碼為攔截 `com.td.cloud.mall` 包下所有 `controller` 和 `service` 子包中的公共方法。
     * 你可以根據實際項目結構調整這個包名。
     */
    @Pointcut("execution(public * com.td.cloud.mall..*.controller..*.*(..)) || " +
            "execution(public * com.td.cloud.mall..*.service..*.*(..))")
    public void apiLogPointcut() {}

    @Around("apiLogPointcut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        // 檢查 td.log.api-log-enabled 配置，如果為 false 則直接跳過日誌邏輯
        if (!logProperties.isApiLogEnabled()) {
            return joinPoint.proceed();
        }

        long startTime = System.currentTimeMillis();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getDeclaringClass().getName() + "." + method.getName();

        Object result = null;
        Throwable exception = null;

        try {
            log.info("API 請求開始: {} | Arguments: {}", methodName, getArgsJson(joinPoint.getArgs()));
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            exception = e;
            throw e; // 重新拋出異常，確保上層邏輯能捕獲到
        } finally {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            if (exception != null) {
                log.error("API 請求異常: {} | 耗時: {} ms | Exception: {}", methodName, duration, exception.getMessage(), exception);
            } else {
                log.info("API 請求結束: {} | 耗時: {} ms | 返回值: {}", methodName, duration, getResultJson(result));
            }
        }
    }

    /**
     * 將方法參數轉換為 JSON 字符串。
     * 排除敏感對象如 HttpServletRequest/Response 和 MultipartFile。
     */
    private String getArgsJson(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        List<Object> filteredArgs = Arrays.stream(args)
                .filter(arg -> !(arg instanceof HttpServletRequest) &&
                        !(arg instanceof HttpServletResponse) &&
                        !(arg instanceof MultipartFile))
                .collect(Collectors.toList());
        try {
            return objectMapper.writeValueAsString(filteredArgs);
        } catch (Exception e) {
            log.warn("轉換方法參數為 JSON 失敗: {}", e.getMessage());
            return Arrays.toString(args); // 轉換失敗則返回原始字符串
        }
    }

    /**
     * 將方法返回值轉換為 JSON 字符串。
     */
    private String getResultJson(Object result) {
        if (result == null) {
            return "null";
        }
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.warn("轉換方法返回值為 JSON 失敗: {}", e.getMessage());
            return result.toString(); // 轉換失敗則返回原始字符串
        }
    }

}
