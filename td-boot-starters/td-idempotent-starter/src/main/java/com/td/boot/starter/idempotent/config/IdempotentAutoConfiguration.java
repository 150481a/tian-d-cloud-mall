package com.td.boot.starter.idempotent.config;

import com.td.boot.starter.idempotent.core.IdempotentAspect;
import com.td.boot.starter.idempotent.expression.SpelKeyGenerator;
import com.td.boot.starter.idempotent.properties.IdempotentProperties;
import com.td.boot.starter.idempotent.storage.IdempotentStorage;
import com.td.boot.starter.idempotent.storage.local.LocalIdempotentStorage;
import com.td.boot.starter.idempotent.storage.redis.RedisIdempotentStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 冪等性 Starter 的自動配置類。
 */
@Configuration
@EnableConfigurationProperties(IdempotentProperties.class) // 啟用配置屬性綁定
@ConditionalOnProperty(prefix = "td.idempotent", name = "enabled", havingValue = "true", matchIfMissing = true) // 只有啟用時才生效
@EnableAspectJAutoProxy // 啟用 Spring AOP 自動代理
public class IdempotentAutoConfiguration {
    private final IdempotentProperties properties;

    public IdempotentAutoConfiguration(IdempotentProperties properties) {
        this.properties = properties;
    }

    /**
     * 配置 SpEL Key 生成器。
     */
    @Bean
    @ConditionalOnMissingBean(SpelKeyGenerator.class)
    public SpelKeyGenerator spelKeyGenerator() {
        return new SpelKeyGenerator();
    }

    /**
     * 配置 Redis 冪等存儲。
     * 只有當 storeType 為 REDIS 且 StringRedisTemplate 類存在時才生效。
     */
    @Bean
    @ConditionalOnMissingBean(IdempotentStorage.class)
    @ConditionalOnExpression("'${td.idempotent.store-type}'.equalsIgnoreCase('REDIS')")
    @ConditionalOnClass(StringRedisTemplate.class)
    public IdempotentStorage redisIdempotentStorage(StringRedisTemplate redisTemplate) {
        return new RedisIdempotentStorage(redisTemplate);
    }

    /**
     * 配置本地緩存冪等存儲 (Caffeine)。
     * 只有當 storeType 為 LOCAL 且 Caffeine 類存在時才生效。
     */
    @Bean
    @ConditionalOnMissingBean(IdempotentStorage.class)
    @ConditionalOnExpression("'${td.idempotent.store-type}'.equalsIgnoreCase('LOCAL')")
    @ConditionalOnClass(name = "com.github.benmanes.caffeine.cache.Caffeine") // 檢查 Caffeine 類
    public IdempotentStorage localIdempotentStorage() {
        return new LocalIdempotentStorage(
                properties.getLocalCache().getInitialCapacity(),
                properties.getLocalCache().getMaximumSize()
        );
    }

    /**
     * 配置冪等性切面。
     * 只有當 IdempotentStorage 和 SpelKeyGenerator 都存在時才生效。
     */
    @Bean
    @ConditionalOnMissingBean(IdempotentAspect.class)
    @ConditionalOnClass(name = "org.aspectj.lang.ProceedingJoinPoint") // 確保 AOP 相關類存在
    public IdempotentAspect idempotentAspect(IdempotentStorage idempotentStorage, SpelKeyGenerator spelKeyGenerator) {
        return new IdempotentAspect(idempotentStorage, spelKeyGenerator, properties);
    }
}
