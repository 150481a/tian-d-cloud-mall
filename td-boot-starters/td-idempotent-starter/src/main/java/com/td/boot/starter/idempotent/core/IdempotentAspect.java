package com.td.boot.starter.idempotent.core;

import com.td.boot.starter.idempotent.annotation.Idempotent;
import com.td.boot.starter.idempotent.core.exception.IdempotentException;
import com.td.boot.starter.idempotent.expression.SpelKeyGenerator;
import com.td.boot.starter.idempotent.properties.IdempotentProperties;
import com.td.boot.starter.idempotent.storage.IdempotentStorage;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 冪等性切面。
 * 攔截被 @Idempotent 註解的方法，實現冪等性檢查和處理。
 */
@Aspect
@Component
@Slf4j
public class IdempotentAspect {

    private final IdempotentStorage idempotentStorage;
    private final SpelKeyGenerator spelKeyGenerator;
    private final IdempotentProperties properties;
    // 使用 Spring 提供的默認參數名發現器
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();


    public IdempotentAspect(IdempotentStorage idempotentStorage,
                            SpelKeyGenerator spelKeyGenerator,
                            IdempotentProperties properties) {
        this.idempotentStorage = idempotentStorage;
        this.spelKeyGenerator = spelKeyGenerator;
        this.properties = properties;
        log.info("IdempotentAspect 初始化成功，使用存儲類型: {}", idempotentStorage.getStoreType().name());
    }

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        // 1. 生成冪等 Key
        String idempotentKey = generateIdempotentKey(joinPoint, idempotent);

        // 2. 嘗試鎖定 Key
        boolean acquiredLock = idempotentStorage.tryLock(idempotentKey, idempotent.expireTime(), idempotent.unit());

        if (!acquiredLock) {
            // 2.1. 未成功獲取鎖，說明是重複請求
            log.warn("檢測到重複請求，冪等 Key: {}", idempotentKey);
            throw new IdempotentException(idempotent.message());
        }

        // 2.2. 成功獲取鎖，執行原方法
        try {
            log.debug("成功獲取冪等鎖，執行業務邏輯。冪等 Key: {}", idempotentKey);
            return joinPoint.proceed();
        } catch (Throwable e) {
            // 2.3. 業務邏輯執行異常，釋放鎖
            log.error("業務邏輯執行異常，釋放冪等鎖。冪等 Key: {}", idempotentKey, e);
            idempotentStorage.releaseLock(idempotentKey);
            throw e; // 重新拋出異常
        }
    }

    /**
     * 生成冪等 Key。
     * 優先使用註解中指定的 SpEL 表達式，如果未指定則嘗試從請求頭或參數中獲取默認 Key。
     *
     * @param joinPoint 切點
     * @param idempotent 冪等註解
     * @return 生成的冪等 Key
     */
    private String generateIdempotentKey(ProceedingJoinPoint joinPoint, Idempotent idempotent) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        String keyExpression = idempotent.key();
        String prefix = idempotent.prefix();

        String generatedKey;

        if (StringUtils.hasText(keyExpression)) {
            // 使用 SpEL 表達式生成 Key
            // 創建 MethodBasedEvaluationContext，並傳入 parameterNameDiscoverer
            MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                    new Object(), method, args, parameterNameDiscoverer);

            // 將 HttpServletRequest 加入 SpEL 上下文，方便訪問請求頭/參數
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                context.setVariable("request", request); // 將 request 對象放入 SpEL 上下文
            }

            generatedKey = spelKeyGenerator.generateKey(keyExpression, context); // 傳遞上下文給 SpelKeyGenerator

        } else {
            // 如果沒有指定 SpEL 表達式，則嘗試從請求頭獲取默認 Key
            generatedKey = getDefaultKeyFromRequest();
            if (!StringUtils.hasText(generatedKey)) {
                // 如果請求頭沒有，則使用方法簽名和參數 Hash 作為默認 Key
                generatedKey = generateDefaultKeyFromMethod(method, args);
            }
        }

        Assert.hasText(generatedKey, "無法生成冪等 Key，請檢查 @Idempotent 註解的 Key 配置或請求信息。");

        // 確保前綴和生成的 Key 之間有分隔符
        return StringUtils.hasText(prefix) ? prefix + ":" + generatedKey : generatedKey;
    }

    /**
     * 嘗試從 HTTP 請求頭中獲取默認的冪等 Key。
     * 優先獲取 'X-Idempotent-Key'，其次 'X-Request-Id'。
     * @return 獲取到的 Key，如果不存在則為 null
     */
    private String getDefaultKeyFromRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String idempotentKey = request.getHeader("X-Idempotent-Key");
        if (!StringUtils.hasText(idempotentKey)) {
            idempotentKey = request.getHeader("X-Request-Id"); // 常用的請求唯一 ID
        }
        return idempotentKey;
    }

    /**
     * 從方法簽名和參數生成一個默認的 Key。
     * 適用於沒有顯式提供冪等 Key 的情況，但這種方式生成的 Key 不具備業務可讀性。
     * @param method 方法
     * @param args 參數
     * @return 生成的默認 Key
     */
    private String generateDefaultKeyFromMethod(Method method, Object[] args) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.getDeclaringClass().getName())
                .append(".").append(method.getName()).append("(");
        for (Object arg : args) {
            sb.append(arg != null ? arg.hashCode() : "null").append(",");
        }
        if (args.length > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append(")");
        return String.valueOf(sb.toString().hashCode()); // 簡單哈希，可能碰撞
    }
}
