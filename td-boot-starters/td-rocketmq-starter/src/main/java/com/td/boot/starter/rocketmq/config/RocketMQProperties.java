package com.td.boot.starter.rocketmq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "td.rocketmq")
public class RocketMQProperties {
    /**
     * 是否启用 RocketMQ 自动配置功能。
     * 默认为 true。如果设置为 false，则 RocketMQ 功能将不会自动配置。
     */
    private boolean enabled = true;

    /**
     * NameServer 地址，多个地址用分号 (;) 分隔。
     * 例如: "192.168.1.100:9876;192.168.1.101:9876"
     */
    private String nameServer;

    /**
     * Access Key，用于身份验证。
     * 如果您的 RocketMQ 服务启用了 ACL (Access Control List)，则需要配置此项。
     */
    private String accessKey;

    /**
     * Secret Key，用于身份验证。
     * 如果您的 RocketMQ 服务启用了 ACL，则需要配置此项。
     */
    private String secretKey;

    /**
     * 是否启用消息轨迹 (Trace) 功能。
     * 开启后，可以追踪消息的发送和消费路径。
     */
    private boolean enableMsgTrace = false;

    /**
     * 消息轨迹服务地址，通常不需要手动配置，由 Spring Boot Starter 自动处理。
     * 仅当 enableMsgTrace 为 true 且有特殊需求时才需要。
     */
    private String customizedTraceTopic;

    /**
     * 生产者配置
     */
    private Producer producer = new Producer();

    /**
     * 消费者配置
     */
    private Consumer consumer = new Consumer();

    /**
     * 生产者配置内部类
     */
    @Data
    public static class Producer {
        /**
         * 生产者组名。
         * 必须在应用中唯一。
         */
        private String group;

        /**
         * 消息发送超时时间 (毫秒)。
         * 默认为 3000 (3秒)。
         */
        private Integer sendMsgTimeout = 3000;

        /**
         * 消息发送失败后的重试次数。
         * 默认：同步发送 retryTimesWhenSendFailed = 2；异步发送 retryTimesWhenSendFailed = 0。
         */
        private Integer retryTimesWhenSendFailed = 2;

        /**
         * 消息体超过多少字节会进行压缩 (kb)。
         * 默认 4KB。当消息体大于此值时，会进行 GZIP 压缩。
         */
        private Integer compressMsgBodyOverHowmuch = 4; // 单位 KB

        /**
         * 是否在发送消息时使用 VIP 通道。
         * 默认为 false。
         */
        private Boolean enableSrvaddrAutoSelect = false;

        /**
         * 发送消息时是否允许重定向。
         * 默认为 true。
         */
        private Boolean sendMessageWithVIPChannel = true;

        /**
         * 同步发送时是否使用批量发送，默认为 false。
         */
        private boolean useNewGeneraterBatchSend = false;

        /**
         * 异步发送时是否使用批量发送，默认为 false。
         */
        private boolean useNewAsyncBatchSend = false;

        /**
         * 是否开启批量发送。
         * 默认为 false。
         */
        private boolean autoBatch = false;

        /**
         * 批量发送的消息大小，默认为 1024 * 10。
         */
        private int batchMessageSize = 1024 * 10;
        /**
         * 批量发送的超时时间，默认为 3000。
         */
        private int batchTimeout = 3000;
        /**
         * 批量发送的线程池大小，默认为 1。
         */
        private int batchThreadPoolSize = 1;

        /**
         * 批量发送的消息条数，默认为 100。
         */
        private int batchSize = 100;

        /**
         * 设置消息体的最大长度，默认为 100KB。
         */
        private Integer maxMessageSize = 100;

    }

    /**
     * 消费者配置内部类
     */
    @Data
    public static class Consumer {
        /**
         * 消费者组名。
         * 必须在应用中唯一，不同的消费者组可以消费同一 Topic 的不同副本。
         */
        private String group;

        /**
         * 消费者最小线程数。
         * 默认 20。
         */
        private Integer consumeThreadMin = 20;

        /**
         * 消费者最大线程数。
         * 默认 64。
         */
        private Integer consumeThreadMax = 64;

        /**
         * 消息模型，包括 CLUSTERING (集群消费) 和 BROADCASTING (广播消费)。
         * 默认为 CLUSTERING。
         */
        private String messageModel = "CLUSTERING"; // CLUSTERING 或 BROADCASTING

        /**
         * 第一次启动时，从哪里開始消費。
         * 可选值: CONSUME_FROM_LAST_OFFSET (從上次消費位點開始，默認),
         * CONSUME_FROM_FIRST_OFFSET (從最早的位點開始),
         * CONSUME_FROM_TIMESTAMP (從指定時間戳開始)。
         */
        private String consumeFromWhere = "CONSUME_FROM_LAST_OFFSET";

        /**
         * 如果 consumeFromWhere 是 CONSUME_FROM_TIMESTAMP，則需要指定此時間戳。
         * 格式例如: "20181111224500" (YYYYMMDDHHmmss)。
         */
        private String consumeTimestamp;

        /**
         * 消息消费失败后，最大重试次数。
         * 默认 16 次。超过此次数后，消息将进入死信队列。
         */
        private Integer maxReconsumeTimes = 16;

        /**
         * 拉取消息时，一次最多拉取的消息条数。
         * 默认 32 条。
         */
        private Integer pullBatchSize = 32;

        /**
         * 最小拉取间隔时间（毫秒），默认为 0。
         */
        private long pullInterval = 0;

        /**
         * 是否按照消息的 key 进行局部顺序消费，默认为 false。
         * 如果设置为 true，则需要确保生产者发送消息时设置了 messageKey。
         */
        private boolean fifo = false;

    }
}
