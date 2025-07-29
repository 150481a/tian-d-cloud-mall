package com.td.boot.starter.redis.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * td-redis-starter 的自動配置類。
 * 根據配置屬性配置 Redis 連接工廠和 RedisTemplate。
 */
@Configuration
// 只有當 RedisTemplate 和 RedisConnectionFactory 存在於 classpath 時才啟用此配置
@ConditionalOnClass({RedisTemplate.class, RedisConnectionFactory.class})
// 綁定 RedisProperties 類
@EnableConfigurationProperties(RedisProperties.class)
// 只有當 td.redis.host 存在或 td.redis.cluster.nodes 或 td.redis.sentinel.nodes 存在時才啟用
@ConditionalOnProperty(prefix = "td.redis", name = "host")
public class RedisAutoConfiguration {

    private final RedisProperties redisProperties;

    @Autowired
    public RedisAutoConfiguration(RedisProperties redisProperties) {
        this.redisProperties = redisProperties;
    }

    /**
     * 創建 LettuceClientConfiguration。
     * 配置連接池和客戶端選項。
     * @return LettuceClientConfiguration
     */
    @Bean
    @ConditionalOnMissingBean(LettuceClientConfiguration.class)
    public LettuceClientConfiguration lettuceClientConfiguration() {
        // 配置連接池
        RedisProperties.LettucePool pool = redisProperties.getLettucePool();
        LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder builder = LettucePoolingClientConfiguration.builder();

        if (pool != null) {
            builder.poolConfig(new GenericObjectPoolConfig(){{
                setMaxIdle(pool.getMaxIdle());
                setMinIdle(pool.getMinIdle());
                setMaxTotal(pool.getMaxActive());
                setMaxWait(pool.getMaxWait());
                setTestOnBorrow(pool.isTestOnBorrow());
                setTestOnReturn(pool.isTestOnReturn());
                setTestWhileIdle(pool.isTestWhileIdle());
                setTimeBetweenEvictionRuns(pool.getTimeBetweenEvictionRuns());
            }});
        }

        // 配置客戶端選項
        // 針對 Redis Cluster 的拓撲刷新
        if (redisProperties.getCluster() != null && redisProperties.getCluster().getNodes() != null && !redisProperties.getCluster().getNodes().isEmpty()) {
            ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
                    .enablePeriodicRefresh(Duration.ofSeconds(30)) // 每 30 秒刷新一次拓撲
                    .enableAllAdaptiveRefreshTriggers() // 啟用所有自適應刷新觸發器
                    .build();
            builder.clientOptions(ClusterClientOptions.builder()
                    .topologyRefreshOptions(topologyRefreshOptions)
                    .build());
        } else {
            builder.clientOptions(ClientOptions.builder().build());
        }

        builder.commandTimeout(redisProperties.getTimeout());

        return builder.build();
    }

    /**
     * 創建 RedisConnectionFactory。
     * 根據配置選擇單點、Sentinel 或 Cluster 模式。
     * @param lettuceClientConfiguration Lettuce 客戶端配置
     * @return RedisConnectionFactory
     */
    @Bean
    @ConditionalOnMissingBean(RedisConnectionFactory.class)
    public RedisConnectionFactory redisConnectionFactory(LettuceClientConfiguration lettuceClientConfiguration) {
        if (redisProperties.getCluster() != null && !redisProperties.getCluster().getNodes().isEmpty()) {
            // Cluster 模式
            List<String> nodes = redisProperties.getCluster().getNodes();
            RedisClusterConfiguration clusterConfiguration = new RedisClusterConfiguration(nodes);
            clusterConfiguration.setMaxRedirects(redisProperties.getCluster().getMaxRedirects());
            if (StringUtils.hasText(redisProperties.getPassword())) {
                clusterConfiguration.setPassword(redisProperties.getPassword());
            }
            return new LettuceConnectionFactory(clusterConfiguration, lettuceClientConfiguration);
        } else if (redisProperties.getSentinel() != null && !redisProperties.getSentinel().getNodes().isEmpty()) {
            // Sentinel 模式
            List<String> nodes = redisProperties.getSentinel().getNodes();
            Set<String> sentinelNodes = new HashSet<>(nodes);
            RedisSentinelConfiguration sentinelConfiguration = new RedisSentinelConfiguration(redisProperties.getSentinel().getMaster(), sentinelNodes);
            sentinelConfiguration.setDatabase(redisProperties.getDatabase());
            if (StringUtils.hasText(redisProperties.getPassword())) {
                sentinelConfiguration.setPassword(redisProperties.getPassword());
            }
            return new LettuceConnectionFactory(sentinelConfiguration, lettuceClientConfiguration);
        } else {
            // 單點模式 (Standalone)
            RedisStandaloneConfiguration standaloneConfiguration = new RedisStandaloneConfiguration();
            standaloneConfiguration.setHostName(redisProperties.getHost());
            standaloneConfiguration.setPort(redisProperties.getPort());
            standaloneConfiguration.setDatabase(redisProperties.getDatabase());
            if (StringUtils.hasText(redisProperties.getPassword())) {
                standaloneConfiguration.setPassword(redisProperties.getPassword());
            }
            return new LettuceConnectionFactory(standaloneConfiguration, lettuceClientConfiguration);
        }
    }

    /**
     * 創建 RedisTemplate Bean。
     * 統一使用 StringRedisSerializer 和 Jackson2JsonRedisSerializer 進行序列化。
     * @param redisConnectionFactory Redis 連接工廠
     * @return RedisTemplate
     */
    @Bean(name = "redisTemplate")
    @ConditionalOnMissingBean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // 配置鍵的序列化器
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);

        // 配置值的序列化器為 Jackson JSON
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 這行是關鍵，用於在序列化和反序列化時保留類型信息，特別是處理泛型和多態
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL
        );
        GenericJackson2JsonRedisSerializer jsonRedisSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        template.setValueSerializer(jsonRedisSerializer);
        template.setHashValueSerializer(jsonRedisSerializer);

        template.afterPropertiesSet(); // 初始化序列化器
        return template;
    }

}
