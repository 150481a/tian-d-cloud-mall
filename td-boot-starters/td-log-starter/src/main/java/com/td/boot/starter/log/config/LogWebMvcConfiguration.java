package com.td.boot.starter.log.config;

import com.td.boot.starter.log.filter.HttpRequestLogFilter;
import com.td.boot.starter.log.filter.TraceIdFilter;
import com.td.boot.starter.log.properties.LogProperties;
import com.td.boot.starter.log.web.RequestBodyCachingFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * 針對 Web 環境的日誌配置。
 * 註冊 TraceIdFilter 和 HttpRequestLogFilter。
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET) // 僅在 Servlet Web 應用中生效
@ConditionalOnProperty(prefix = "td.log", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LogWebMvcConfiguration {

    private final LogProperties properties;

    public LogWebMvcConfiguration(LogProperties properties) {
        this.properties = properties;
    }

    /**
     * 註冊 TraceIdFilter。
     */
    @Bean
    @ConditionalOnProperty(prefix = "td.log", name = "mdc-enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration() {
        FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TraceIdFilter(properties));
        registration.addUrlPatterns("/*"); // 攔截所有 URL
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE); // 確保 TraceIdFilter 最先執行
        registration.setName("traceIdFilter");
        return registration;
    }

    /**
     * 註冊 RequestBodyCachingFilter。
     * 確保 HttpRequestLogFilter 能夠多次讀取請求體。
     * 該過濾器應在 HttpRequestLogFilter 之前執行。
     */
    @Bean
    @ConditionalOnProperty(prefix = "td.log", name = "http-request-log-enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<RequestBodyCachingFilter> requestBodyCachingFilterRegistration() {
        FilterRegistrationBean<RequestBodyCachingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RequestBodyCachingFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1); // 在 traceIdFilter 之後，httpRequestLogFilter 之前
        registration.setName("requestBodyCachingFilter");
        return registration;
    }

    /**
     * 註冊 HttpRequestLogFilter。
     */
    @Bean
    @ConditionalOnProperty(prefix = "td.log", name = "http-request-log-enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<HttpRequestLogFilter> httpRequestLogFilterRegistration() {
        FilterRegistrationBean<HttpRequestLogFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new HttpRequestLogFilter(properties));
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 2); // 在 RequestBodyCachingFilter 之後
        registration.setName("httpRequestLogFilter");
        return registration;
    }
}
