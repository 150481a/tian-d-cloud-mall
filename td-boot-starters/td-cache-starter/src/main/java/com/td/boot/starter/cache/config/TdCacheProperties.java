package com.td.boot.starter.cache.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "td.cache")
public class TdCacheProperties {
    /**
     * 是否啟用 td-cache-starter。
     * 默認為 true。
     */
    private boolean enabled = true;

    /**
     * 緩存的默認過期時間 (秒)。
     * 默認為 600 秒 (10 分鐘)。
     */
    private Duration defaultExpire = Duration.ofSeconds(600);

    /**
     * 是否允許緩存空值，防止緩存穿透。
     * 默認為 true。
     */
    private boolean cacheNullValues = true;

    /**
     * 是否使用 Caffeine 作為二級緩存。
     * 默認為 false。
     */
    private boolean useCaffeine = false;

    /**
     * 是否在啟動時預加載緩存。
     * 默認為 false。
     */
    private boolean preloadCache = false;

    /**
     * 按緩存名稱配置不同的過期時間 (秒)。
     * Key 為緩存名稱，Value 為過期時間 (秒)。
     */
    private Map<String, Duration> expires;

    /**
     * 按緩存名稱配置 Caffeine 的相關屬性。
     */
    private Map<String, CaffeineProperties> caffeine;

    @Data
    public static class CaffeineProperties {
        /**
         * 緩存的最大條目數。
         */
        private long maximumSize = 10000;

        /**
         * 緩存條目的過期時間 (寫入後)。
         */
        private Duration expireAfterWrite;

        /**
         * 緩存條目的過期時間 (訪問後)。
         */
        private Duration expireAfterAccess;
    }
}
