package com.td.boot.starter.idempotent.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 冪等性 Starter 的配置屬性。
 * 前綴為 "td.idempotent"。
 */
@Data
@ConfigurationProperties(prefix = "td.idempotent")
public class IdempotentProperties {
    /**
     * 是否啟用冪等性自動配置。
     * 默認為 true。
     */
    private boolean enabled = true;

    /**
     * 冪等 Key 的默認存儲類型。
     * 默認為 REDIS。
     */
    private StoreType storeType = StoreType.REDIS;

    /**
     * 冪等 Key 的默認過期時間，單位秒。
     * 默認 5 分鐘。
     */
    private long defaultExpireTimeSeconds = 5 * 60;

    /**
     * 當檢測到重複請求時，拋出的默認異常消息。
     */
    private String defaultErrorMessage = "重複提交，請勿重複操作。";

    /**
     * 本地緩存 (Caffeine) 的配置屬性。
     * 只有當 storeType 為 LOCAL 時生效。
     */
    private LocalCacheProperties localCache = new LocalCacheProperties();

    /**
     * 冪等 Key 存儲類型枚舉。
     */
    public enum StoreType {
        REDIS,
        LOCAL
        // DB 可以在未來擴展
    }

    /**
     * 本地緩存配置屬性。
     */
    @Data
    public static class LocalCacheProperties {
        /**
         * 本地緩存的初始容量。
         */
        private int initialCapacity = 1000;
        /**
         * 本地緩存的最大容量。
         * 當緩存達到此容量時，會根據淘汰策略移除舊條目。
         */
        private long maximumSize = 10000;
        /**
         * 本地緩存條目的寫入後過期時間，單位秒。
         * 如果沒有在 @Idempotent 註解中指定 expireTime，則使用這個默認值。
         */
        private long expireAfterWriteSeconds = 5 * 60; // 默認 5 分鐘
    }

}
