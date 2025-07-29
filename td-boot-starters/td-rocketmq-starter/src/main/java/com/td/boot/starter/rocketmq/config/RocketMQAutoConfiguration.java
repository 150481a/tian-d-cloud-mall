package com.td.boot.starter.rocketmq.config;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.util.StringUtils;

/**
 * RocketMQ 核心自动配置类。
 * 负责根据 td.rocketmq 配置创建 RocketMQTemplate 等核心 Bean。
 * 不包含具体的业务消息生产者或消费者实现。
 */
@Configuration
@ConditionalOnClass({RocketMQTemplate.class, DefaultMQProducer.class, DefaultMQPushConsumer.class}) // 只有当 RocketMQTemplate 在类路径上时才启用
@EnableConfigurationProperties(RocketMQProperties.class) // 启用 RocketMQProperties
@ConditionalOnProperty(prefix = "td.rocketmq", name = "enabled", havingValue = "true", matchIfMissing = true) // 默认启用，除非 td.rocketmq.enabled=false
// 导入 RocketMQ Spring Boot 官方自动配置，在此基础上进行增强或覆盖
// 注意：如果官方 starter 已经满足大部分需求，你也可以选择不导入或只导入部分
// 通常情况下，org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration 会自动处理大部分核心 Bean 的创建
// 你可以在这里定义 @ConditionalOnMissingBean 的 Bean 来覆盖或补充官方的配置
@Import(RocketMQAutoConfiguration.class) // 导入 RocketMQ 官方的自动配置类
public class RocketMQAutoConfiguration {

    private final RocketMQProperties rocketMQProperties;

    @Autowired
    public RocketMQAutoConfiguration(RocketMQProperties rocketMQProperties) {
        this.rocketMQProperties = rocketMQProperties;
    }

    /**
     * 配置并创建 DefaultMQProducer Bean。
     * 这是 RocketMQ 客户端底层的生产者实例。
     *
     * 使用 @ConditionalOnMissingBean 确保只有在 Spring 上下文中没有其他 DefaultMQProducer Bean 时才创建此 Bean。
     * 使用 @ConditionalOnProperty 确保只有配置了生产者组时才创建此生产者。
     *
     * @return 配置好的 DefaultMQProducer 实例。
     */
    @Bean(destroyMethod = "shutdown") // 添加 destroyMethod 以确保生产者在应用关闭时正确关闭
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "td.rocketmq.producer", name = "group")
    public DefaultMQProducer defaultMQProducer() {
        if (!StringUtils.hasText(rocketMQProperties.getProducer().getGroup())) {
            throw new IllegalArgumentException("RocketMQ producer group must be configured when creating DefaultMQProducer!");
        }

        DefaultMQProducer producer = new DefaultMQProducer(rocketMQProperties.getProducer().getGroup());
        // 设置 NameServer 地址
        if (StringUtils.hasText(rocketMQProperties.getNameServer())) {
            producer.setNamesrvAddr(rocketMQProperties.getNameServer());
        }

        // 设置 ACL 凭证
//        if (StringUtils.hasText(rocketMQProperties.getAccessKey()) && StringUtils.hasText(rocketMQProperties.getSecretKey())) {
//            producer.setAccessKey(rocketMQProperties.getAccessKey());
//            producer.setSecretKey(rocketMQProperties.getSecretKey());
//        }

        // 设置消息发送超时时间
        if (rocketMQProperties.getProducer().getSendMsgTimeout() != null) {
            producer.setSendMsgTimeout(rocketMQProperties.getProducer().getSendMsgTimeout());
        }

        // 设置消息发送失败后的重试次数
        if (rocketMQProperties.getProducer().getRetryTimesWhenSendFailed() != null) {
            producer.setRetryTimesWhenSendFailed(rocketMQProperties.getProducer().getRetryTimesWhenSendFailed());
        }

        // 设置消息体压缩阈值 (单位：字节)
        if (rocketMQProperties.getProducer().getCompressMsgBodyOverHowmuch() != null) {
            producer.setCompressMsgBodyOverHowmuch(rocketMQProperties.getProducer().getCompressMsgBodyOverHowmuch() * 1024); // KB 转 Byte
        }

        // 设置消息最大大小 (单位：字节)
        if (rocketMQProperties.getProducer().getMaxMessageSize() != null) {
            producer.setMaxMessageSize(rocketMQProperties.getProducer().getMaxMessageSize());
        }

        // 是否在发送消息时使用 VIP 通道
        if (rocketMQProperties.getProducer().getSendMessageWithVIPChannel() != null) {
            producer.setSendMessageWithVIPChannel(rocketMQProperties.getProducer().getSendMessageWithVIPChannel());
        }



        // 同步发送时是否使用新的批量发送器
//        if (rocketMQProperties.getProducer().getUseNewGeneraterBatchSend() != null) {
//            producer.setUseNewGeneraterBatchSend(rocketMQProperties.getProducer().getUseNewGeneraterBatchSend());
//        }

        // 注意：DefaultMQProducer 需要在被使用前调用 start() 方法，
        // RocketMQTemplate 在设置了 producer 后，其 afterPropertiesSet() 方法会自动调用 start()。
        // 所以这里不需要手动 start()。
        return producer;
    }

    /**
     * 配置 RocketMQTemplate Bean。
     * 此 Bean 是 Spring 提供的高级接口，用于方便地发送 RocketMQ 消息。
     * 它将使用上面定义的 DefaultMQProducer Bean。
     *
     * 使用 @ConditionalOnMissingBean 確保只有在 Spring 上下文中沒有其他 RocketMQTemplate Bean 時才創建此 Bean。
     *
     * @param defaultMQProducer 注入的 DefaultMQProducer 实例。
     * @return 配置好的 RocketMQTemplate 实例。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "td.rocketmq.producer", name = "group") // 只有当生产者被创建时，才创建 RocketMQTemplate
    public RocketMQTemplate rocketMQTemplate(DefaultMQProducer defaultMQProducer) {
        RocketMQTemplate rocketMQTemplate = new RocketMQTemplate();
        // 将自定义配置的 DefaultMQProducer 设置到 RocketMQTemplate 中
        rocketMQTemplate.setProducer(defaultMQProducer);

//        // RocketMQTemplate 还会使用一些全局属性，例如消息轨迹
//        rocketMQTemplate.setEnableMsgTrace(rocketMQProperties.isEnableMsgTrace());
//        if (rocketMQProperties.isEnableMsgTrace() && StringUtils.hasText(rocketMQProperties.getCustomizedTraceTopic())) {
//            rocketMQTemplate.setCustomizedTraceTopic(rocketMQProperties.getCustomizedTraceTopic());
//        }

        // ACL 凭证通常由 DefaultMQProducer 设置，但 RocketMQTemplate 也有自己的设置，
        // 如果这里不设置，会沿用 DefaultMQProducer 的设置。
        // 如果需要覆盖 DefaultMQProducer 的凭证，可以在这里再次设置：
        // if (StringUtils.hasText(rocketMQProperties.getAccessKey()) && StringUtils.hasText(rocketMQProperties.getSecretKey())) {
        //    rocketMQTemplate.setAccessKey(rocketMQProperties.getAccessKey());
        //    rocketMQTemplate.setSecretKey(rocketMQProperties.getSecretKey());
        // }

        return rocketMQTemplate;
    }

    // --- 关于消费者配置的说明 ---
    // RocketMQ Spring Boot Starter 官方自动配置 (org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration)
    // 负责处理 DefaultMQPushConsumer 的基础配置（如 nameServer），
    // 以及扫描带有 @RocketMQMessageListener 标记的类，并为每个监听器创建和管理其对应的消费者实例。
    //
    // 因此，通常您不需要在此 TdRocketMQAutoConfiguration 中手动创建 DefaultMQPushConsumer 的 Bean。
    // RocketMQProperties 中关于消费者的配置（如 group, consumeThreadMin/Max, messageModel 等）
    // 会被官方 Starter 内部用于配置它所管理的 DefaultMQPushConsumer 实例，或者用于 @RocketMQMessageListener 的解析。
    // 如果您需要更深度的消费者基础设施定制，可能需要编写自定义的 ConsumerFactory 或 MessageListenerContainerPostProcessor，

}
