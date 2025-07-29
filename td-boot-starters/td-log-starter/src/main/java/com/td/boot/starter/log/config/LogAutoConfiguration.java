package com.td.boot.starter.log.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.td.boot.starter.log.aspect.ApiLogAspect;
import com.td.boot.starter.log.properties.LogProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 日誌 Starter 的自動配置類。
 */
@Configuration
@EnableConfigurationProperties(LogProperties.class)
@ConditionalOnProperty(prefix = "td.log", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableAspectJAutoProxy // 啟用 Spring AOP 自動代理
public class LogAutoConfiguration {

    private final LogProperties properties;

    public LogAutoConfiguration(LogProperties properties) {
        this.properties = properties;
    }

    /**
     * 配置 API 日誌切面。
     */
    @Bean
    @ConditionalOnMissingBean(ApiLogAspect.class)
    @ConditionalOnProperty(prefix = "td.log", name = "api-log-enabled", havingValue = "true", matchIfMissing = true)
    public ApiLogAspect apiLogAspect() {
        return new ApiLogAspect(properties);
    }

    /**
     * 確保 ObjectMapper 存在，用於 JSON 序列化。
     * 通常由 Spring Boot Web 自動配置，但為了安全起見再次檢查。
     */
    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    @ConditionalOnClass(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
