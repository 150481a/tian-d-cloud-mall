package com.td.boot.starter.distributedid.config;

import com.td.boot.starter.distributedid.generator.IIdGenerator;
import com.td.boot.starter.distributedid.generator.segment.SegmentIdGenerator;
import com.td.boot.starter.distributedid.generator.segment.provider.DbIdSegmentProvider;
import com.td.boot.starter.distributedid.generator.segment.provider.IdSegmentProvider;
import com.td.boot.starter.distributedid.generator.segment.provider.RedisIdSegmentProvider;
import com.td.boot.starter.distributedid.generator.snowflake.SnowflakeIdGenerator;
import com.td.boot.starter.distributedid.generator.snowflake.worker.DefaultWorkerIdAssigner;
import com.td.boot.starter.distributedid.generator.snowflake.worker.WorkerIdAssigner;
import com.td.boot.starter.distributedid.generator.uuid.UuidGenerator;
import com.td.boot.starter.distributedid.properties.DistributedIdProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

/**
 * 分佈式 ID 生成器 Starter 的自動配置類。
 */
@Configuration
@EnableConfigurationProperties(DistributedIdProperties.class) // 啟用配置屬性綁定
@ConditionalOnProperty(prefix = "td.distributed-id", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DistributedIdAutoConfiguration {

    private final DistributedIdProperties properties;

    @Autowired
    public DistributedIdAutoConfiguration(DistributedIdProperties properties) {
        this.properties = properties;
    }

    /**
     * 配置 WorkerIdAssigner。
     * 如果用戶沒有自定義 WorkerIdAssigner，則使用默認的基於配置的實現。
     */
    @Bean
    @ConditionalOnMissingBean(WorkerIdAssigner.class)
    public WorkerIdAssigner defaultWorkerIdAssigner() {
        return new DefaultWorkerIdAssigner(properties);
    }

    /**
     * 配置 SnowflakeIdGenerator 作為默認的 IIdGenerator 實現。
     * 只有在 IIdGenerator 類路徑存在且沒有其他 IIdGenerator Bean 時才生效。
     * 並且默認策略配置為 SNOWFLAKE。
     */
    @Bean
    @ConditionalOnClass(IIdGenerator.class) // 只有 IIdGenerator 存在於 classpath 時才自動配置
    @ConditionalOnMissingBean(IIdGenerator.class) // 避免用戶重複定義
    @ConditionalOnProperty(prefix = "td.distributed-id", name = "default-strategy", havingValue = "SNOWFLAKE", matchIfMissing = true)
    public IIdGenerator snowflakeIdGenerator(WorkerIdAssigner workerIdAssigner) {
        return new SnowflakeIdGenerator(workerIdAssigner);
    }



    // --- UUID 策略配置 ---
    @Bean
    @ConditionalOnMissingBean(IIdGenerator.class)
    @ConditionalOnExpression("'${td.distributed-id.default-strategy}'.equalsIgnoreCase('UUID')")
    public IIdGenerator uuidGenerator() {
        return new UuidGenerator();
    }

    // --- Segment 策略配置 ---

    /**
     * 條件化創建基於數據庫的 IdSegmentProvider。
     * 只有當策略為 SEGMENT 且 providerType 為 DB，且存在 JdbcTemplate 和 DataSourceTransactionManager 時才生效。
     */
    @Bean
    @ConditionalOnMissingBean(IdSegmentProvider.class)
    @ConditionalOnExpression("'${td.distributed-id.default-strategy}'.equalsIgnoreCase('SEGMENT') && '${td.distributed-id.segment.provider-type}'.equalsIgnoreCase('DB')")
    @ConditionalOnClass({JdbcTemplate.class, DataSource.class, DataSourceTransactionManager.class}) // 判斷類是否存在
    @ConditionalOnBean({JdbcTemplate.class, DataSourceTransactionManager.class}) // 判斷 Spring Bean 是否存在
    public IdSegmentProvider dbIdSegmentProvider(JdbcTemplate jdbcTemplate, DataSourceTransactionManager transactionManager) {
        return new DbIdSegmentProvider(jdbcTemplate, transactionManager);
    }

    /**
     * 條件化創建基於 Redis 的 IdSegmentProvider。
     * 只有當策略為 SEGMENT 且 providerType 為 REDIS，且存在 StringRedisTemplate 時才生效。
     */
    @Bean
    @ConditionalOnMissingBean(IdSegmentProvider.class)
    @ConditionalOnExpression("'${td.distributed-id.default-strategy}'.equalsIgnoreCase('SEGMENT') && '${td.distributed-id.segment.provider-type}'.equalsIgnoreCase('REDIS')")
    @ConditionalOnClass(StringRedisTemplate.class) // 判斷類是否存在
    @ConditionalOnBean(StringRedisTemplate.class) // 判斷 Spring Bean 是否存在
    public IdSegmentProvider redisIdSegmentProvider(StringRedisTemplate redisTemplate) {
        return new RedisIdSegmentProvider(redisTemplate, properties.getSegment().getStep());
    }

    /**
     * 配置 SegmentIdGenerator 作為默認的 IIdGenerator 實現。
     * 只有當策略為 SEGMENT 且存在 IdSegmentProvider 時才生效。
     */
    @Bean
    @ConditionalOnMissingBean(IIdGenerator.class)
    @ConditionalOnExpression("'${td.distributed-id.default-strategy}'.equalsIgnoreCase('SEGMENT')")
    @ConditionalOnBean(IdSegmentProvider.class) // 確保 IdSegmentProvider 已經被創建
    public IIdGenerator segmentIdGenerator(IdSegmentProvider idSegmentProvider) {
        return new SegmentIdGenerator(idSegmentProvider, properties);
    }
}
