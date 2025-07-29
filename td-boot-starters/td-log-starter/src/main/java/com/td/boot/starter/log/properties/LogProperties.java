package com.td.boot.starter.log.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "td.log")
public class LogProperties {
    /**
     * 是否啟用日誌自動配置。
     * 默認為 true。
     */
    private boolean enabled = true;

    /**
     * 是否啟用 MDC (Mapped Diagnostic Context) 功能，用於日誌追踪。
     * 默認為 true。
     */
    private boolean mdcEnabled = true;

    /**
     * 是否啟用 HTTP 請求響應日誌。
     * 默認為 true。
     */
    private boolean httpRequestLogEnabled = true;

    /**
     * HTTP 請求日誌中需要排除的 URL 列表（支持 Ant 模式）。
     * 例如：/actuator/**, /swagger-ui.html
     */
    private List<String> httpRequestLogExcludes;

    /**
     * HTTP 請求日誌中是否打印請求體內容。
     * 默認為 false，因為請求體可能很大，或包含敏感信息。
     */
    private boolean logRequestBody = false;

    /**
     * HTTP 請求日誌中是否打印響應體內容。
     * 默認為 false，原因同上。
     */
    private boolean logResponseBody = false;

    /**
     * 是否啟用 AOP 方式的接口調用日誌（記錄方法入參、出參、耗時等）。
     * 默認為 true。
     */
    private boolean apiLogEnabled = true;

    /**
     * AOP 接口調用日誌切點表達式。
     * 默認攔截控制器和服務層方法。
     * 示例：execution(public * com.td.cloud.mall..*.service..*.*(..))
     */
    private String apiLogPointcut = "execution(public * com.td.cloud.mall..*.controller..*.*(..)) || " +
            "execution(public * com.td.cloud.mall..*.service..*.*(..))";

    /**
     * 是否啟用 JSON 格式日誌輸出。
     * 默認為 true。如果為 true，則需要引入 logstash-logback-encoder 依賴。
     */
    private boolean jsonLogEnabled = true;

    /**
     * JSON 日誌中是否包含完整的棧追踪信息（僅在錯誤日誌中生效）。
     * 默認為 false，避免過長的日誌。
     */
    private boolean jsonLogFullStackTrace = false;

    /**
     * JSON 日誌服務名稱字段。
     * 默認取 spring.application.name。
     */
    private String jsonLogServiceName;

    /**
     * 請求 ID 的 Header 名稱。
     * 默認為 X-Request-Id。
     */
    private String requestIdHeaderName = "X-Request-Id";
}
