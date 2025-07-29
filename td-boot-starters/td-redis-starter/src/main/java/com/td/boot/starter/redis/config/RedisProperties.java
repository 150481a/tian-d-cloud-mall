package com.td.boot.starter.redis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "td.redis")
public class RedisProperties {

    /**
     * Redis 服務器地址 (單點模式)。
     * 示例：localhost
     */
    private String host = "localhost";

    /**
     * Redis 服務器端口 (單點模式)。
     * 示例：6379
     */
    private int port = 6379;

    /**
     * Redis 數據庫索引。
     * 默認為 0。
     */
    private int database = 0;

    /**
     * Redis 服務器密碼。
     * 默認無密碼。
     */
    private String password;

    /**
     * 連接超時時間。
     * 示例：5000ms (5 秒)。
     * 默認為 5 秒。
     */
    private Duration timeout = Duration.ofMillis(5000);

    /**
     * Redis Sentinel (哨兵) 配置。
     */
    private Sentinel sentinel;

    /**
     * Redis Cluster (集群) 配置。
     */
    private Cluster cluster;

    /**
     * Lettuce 客戶端連接池配置。
     */
    private LettucePool lettucePool;

    @Data
    public static class Sentinel {
        /**
         * Sentinel 監控的主節點名稱。
         * 示例：mymaster
         */
        private String master;

        /**
         * Sentinel 節點地址列表。
         * 示例：192.168.1.10:26379,192.168.1.11:26379,192.168.1.12:26379
         */
        private List<String> nodes;
    }

    @Data
    public static class Cluster {
        /**
         * Cluster 節點地址列表。
         * 示例：192.168.1.10:6379,192.168.1.11:6379,192.168.1.12:6379
         */
        private List<String> nodes;

        /**
         * 請求重定向的最大次數。
         * 默認為 3。
         */
        private int maxRedirects = 3;
    }

    @Data
    public static class LettucePool {
        /**
         * 最大連接數 (負數表示無限制)。
         */
        private int maxActive = 8;

        /**
         * 最大空閒連接數。
         */
        private int maxIdle = 8;

        /**
         * 最小空閒連接數。
         */
        private int minIdle = 0;

        /**
         * 連接等待時間 (負數表示無限等待)。
         */
        private Duration maxWait = Duration.ofMillis(-1);

        /**
         * 在從連接池獲取連接時是否進行有效性檢查。
         */
        private boolean testOnBorrow = false;

        /**
         * 在將連接返回到連接池時是否進行有效性檢查。
         */
        private boolean testOnReturn = false;

        /**
         * 在空閒連接驅逐時是否進行有效性檢查。
         */
        private boolean testWhileIdle = false;

        /**
         * 空閒連接驅逐週期 (毫秒)。
         */
        private Duration timeBetweenEvictionRuns = Duration.ofMillis(-1);
    }

}
