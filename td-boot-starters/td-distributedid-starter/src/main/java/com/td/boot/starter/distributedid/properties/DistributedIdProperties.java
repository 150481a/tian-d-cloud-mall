package com.td.boot.starter.distributedid.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 分佈式 ID 生成器配置屬性。
 * 前綴為 "td.distributed-id"。
 */
@Data
@Component
@ConfigurationProperties(prefix = "td.distributed-id")
public class DistributedIdProperties {
    /**
     * 默認使用的 ID 生成策略類型。
     * 支持 "snowflake", "uuid", "segment"。
     */
    private IdStrategyType defaultStrategy = IdStrategyType.SNOWFLAKE;

    /**
     * Snowflake 算法相關配置。
     */
    private SnowflakeProperties snowflake = new SnowflakeProperties();

    /**
     * Segment (號段模式) 相關配置。
     */
    private SegmentProperties segment = new SegmentProperties();

    /**
     * ID 生成策略類型枚舉。
     */
    public enum IdStrategyType {
        SNOWFLAKE,
        UUID,
        SEGMENT
    }

    /**
     * Snowflake 算法的具體配置屬性。
     */
    @Data
    public static class SnowflakeProperties {
        /**
         * 數據中心 ID (0-31)。
         * 需要保證在同一集群內唯一。
         */
        private long datacenterId = 0L;

        /**
         * 工作節點 ID (0-31)。
         * 需要保證在同一數據中心內唯一。
         */
        private long workerId = 0L;
    }

    /**
     * Segment (號段模式) 的具體配置屬性。
     */
    @Data
    public static class SegmentProperties {
        /**
         * 號段模式使用的業務鍵列表。
         * 例如：["order_id", "user_id"]。每個業務鍵會維護獨立的號段。
         */
        private List<String> bizKeys;

        /**
         * 默認的業務鍵，當不指定 bizKey 調用 generateLongId() 時使用。
         */
        private String defaultBizKey = "default";

        /**
         * 號段提供者類型。
         * 支持 "db", "redis"。
         */
        private SegmentProviderType providerType = SegmentProviderType.DB;

        /**
         * 號段的步長 (一次從數據源獲取多少個 ID)。
         */
        private int step = 1000;

        /**
         * 異步預加載下一個號段的時間間隔 (毫秒)。
         * 當號段剩餘數量達到一定閾值時，觸發異步加載。
         */
        private long preloadIntervalMillis = 3000; // 默認 3 秒

        /**
         * 號段填充異步線程池核心大小。
         */
        private int corePoolSize = 1;

        /**
         * 號段填充異步線程池最大大小。
         */
        private int maxPoolSize = 2;

        /**
         * 號段填充異步線程池線程存活時間 (秒)。
         */
        private int keepAliveTimeSeconds = 60;

        /**
         * 號段填充異步線程池隊列容量。
         */
        private int queueCapacity = 100;

        /**
         * 號段提供者類型枚舉。
         */
        public enum SegmentProviderType {
            DB,
            REDIS
        }
    }

}
