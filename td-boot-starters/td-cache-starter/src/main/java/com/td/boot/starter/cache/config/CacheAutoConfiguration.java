package com.td.boot.starter.cache.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * td-cache-starter 的自動配置類。
 * 配置基於 Redis 的 CacheManager。
 */
@Configuration
// 只有當 CacheManager 和 RedisConnectionFactory 存在於 classpath 時才啟用此配置
@ConditionalOnClass({CacheManager.class, RedisConnectionFactory.class})
// 啟用 Spring Cache 功能
@EnableCaching
// 綁定 TdCacheProperties 類和 Spring Boot 內置的 CacheProperties
@EnableConfigurationProperties({TdCacheProperties.class, CacheProperties.class})
// 在 td-redis-starter 自動配置之後執行，確保 RedisConnectionFactory 已準備好
@AutoConfigureAfter(name = {"com.td.cloud.mall.td.redis.starter.config.RedisAutoConfiguration"})
// 只有當 spring.cache.type=redis 或 td.cache 屬性存在時才啟用此配置
@ConditionalOnProperty(prefix = "spring.cache", name = "type", havingValue = "redis", matchIfMissing = true)
public class CacheAutoConfiguration {

    private final TdCacheProperties tdCacheProperties;
    private final CacheProperties cacheProperties;

    @Autowired
    public CacheAutoConfiguration(TdCacheProperties tdCacheProperties, CacheProperties cacheProperties) {
        this.tdCacheProperties = tdCacheProperties;
        this.cacheProperties = cacheProperties;
    }
    /**
     * 配置 RedisCacheManager。
     * @param redisConnectionFactory Redis 連接工廠
     * @return CacheManager
     */
    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        // 1. 從 Spring Boot 內置的 CacheProperties 獲取默認配置
        RedisCacheConfiguration defaultCacheConfiguration = RedisCacheConfiguration
                .defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonRedisSerializer()));

        // 應用 Spring Boot 內置的 Redis 緩存屬性
        // 設置 TTL
        if (this.cacheProperties.getRedis().getTimeToLive() != null) {
            defaultCacheConfiguration = defaultCacheConfiguration.entryTtl(this.cacheProperties.getRedis().getTimeToLive());
        } else {
            // 如果 Spring Boot 內置屬性沒有設置 TTL，則使用 tdCacheProperties 的默認 TTL
            defaultCacheConfiguration = defaultCacheConfiguration.entryTtl(tdCacheProperties.getDefaultExpire());
        }

        // 處理是否緩存空值
        defaultCacheConfiguration = defaultCacheConfiguration.disableCachingNullValues();

        // 處理是否使用前綴
        if (!this.cacheProperties.getRedis().isUseKeyPrefix()) {
            defaultCacheConfiguration = defaultCacheConfiguration.disableKeyPrefix();
        }

        // 2. 應用 tdCacheProperties 中按緩存名稱的過期時間
        Map<String, RedisCacheConfiguration> initialCacheConfigurations = new HashMap<>();
        if (tdCacheProperties.getExpires() != null && !tdCacheProperties.getExpires().isEmpty()) {
            RedisCacheConfiguration finalDefaultCacheConfiguration = defaultCacheConfiguration;
            tdCacheProperties.getExpires().forEach((cacheName, expireTime) ->
                    initialCacheConfigurations.put(cacheName, finalDefaultCacheConfiguration.entryTtl(expireTime))
            );
        }

        // 構建 RedisCacheWriter，用於傳遞給 RedisCacheManager 的構造函數
        RedisCacheWriter redisCacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory);


        // 如果配置了二級緩存 (Caffeine)，則返回自定義的 CaffeineRedisCacheManager
        if (tdCacheProperties.isUseCaffeine()) {
            // 將所有計算好的配置以及 RedisCacheWriter 直接傳遞給 CaffeineRedisCacheManager
            return new CaffeineRedisCacheManager(redisCacheWriter, defaultCacheConfiguration, initialCacheConfigurations, tdCacheProperties);
        }

        // 否則，正常構建並返回 RedisCacheManager
        // 注意：這裡直接使用 RedisCacheManager.builder() 因為它隱式處理了 RedisCacheWriter 的創建
        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultCacheConfiguration)
                .withInitialCacheConfigurations(initialCacheConfigurations)
                .build();
    }

    /**
     * 創建一個 Jackson JSON 序列化器。
     * 與 td-redis-starter 中 RedisTemplate 的值序列化器保持一致。
     */
    @Bean
    @ConditionalOnMissingBean(GenericJackson2JsonRedisSerializer.class)
    protected GenericJackson2JsonRedisSerializer jsonRedisSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 這行是關鍵，用於在序列化和反序列化時保留類型信息，特別是處理泛型和多態
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL
        );
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

    /**
     * 內部類：支持 Redis + Caffeine 的二級緩存。
     * 這裡僅為示意，實際的二級緩存集成可能需要更複雜的邏輯。
     * 可以考慮使用 spring-cache-redis-caffeine 這樣的第三方庫來實現更健壯的二級緩存。
     */
    protected static class CaffeineRedisCacheManager extends RedisCacheManager {
        private final TdCacheProperties tdCacheProperties;
        // CaffeineCacheManager 負責創建 Spring 的 CaffeineCache 實例
        private final CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();

        public CaffeineRedisCacheManager(RedisCacheWriter redisCacheWriter,
                                         RedisCacheConfiguration defaultCacheConfiguration,
                                         Map<String, RedisCacheConfiguration> initialCacheConfigurations,
                                         TdCacheProperties tdCacheProperties) {
            super(redisCacheWriter, defaultCacheConfiguration, initialCacheConfigurations);
            this.tdCacheProperties = tdCacheProperties;

            // 初始化 CaffeineCacheManager 的配置
            if (tdCacheProperties.getCaffeine() != null) {
                // 為每個緩存名稱應用 Caffeine 配置
                tdCacheProperties.getCaffeine().forEach((cacheName, caffeineProps) -> {
                    Caffeine<Object, Object> caffeineBuilder = Caffeine.newBuilder();
                    if (caffeineProps.getMaximumSize() > 0) {
                        caffeineBuilder.maximumSize(caffeineProps.getMaximumSize());
                    }
                    if (caffeineProps.getExpireAfterWrite() != null) {
                        caffeineBuilder.expireAfterWrite(caffeineProps.getExpireAfterWrite());
                    }
                    if (caffeineProps.getExpireAfterAccess() != null) {
                        caffeineBuilder.expireAfterAccess(caffeineProps.getExpireAfterAccess());
                    }
                    // 將構建好的 Caffeine 實例設置給 CaffeineCacheManager
                    caffeineCacheManager.setCaffeine(caffeineBuilder);
                    // 如果每個緩存名有獨立的 Caffeine 配置，這裡需要更精細的控制
                    // 目前的 CaffeineCacheManager 只能設置一個通用的 Caffeine 實例
                    // 如果需要為不同 cacheName 設置不同的 Caffeine 策略，則需要修改 CaffeineCacheManager 的行為，
                    // 或手動為每個 cacheName 創建並註冊 CaffeineCache 實例
                });
            }
        }

        @Override
        public Cache getCache(String name) {
            // 從父類（RedisCacheManager）獲取 Redis 緩存
            Cache redisCache = super.getCache(name);

            // 如果啟用 Caffeine，並且該緩存名有對應的 Caffeine 配置（或者通用的 Caffeine 配置）
            if (tdCacheProperties.isUseCaffeine()) {
                // 從 CaffeineCacheManager 獲取 Caffeine 緩存
                // 注意：這裡假設 CaffeineCacheManager 能夠基於 cacheName 返回正確的 CaffeineCache
                // 否則，需要手動管理不同 cacheName 對應的 Caffeine 實例
                Cache caffeineCache = caffeineCacheManager.getCache(name);

                if (redisCache != null && caffeineCache != null) {
                    // 返回一個兩級緩存的代理實現
                    return new TwoLevelCache(name, caffeineCache, redisCache);
                }
            }
            return redisCache; // 如果不使用兩級緩存，或 Caffeine 緩存不存在，則返回 Redis 緩存
        }

        @Override
        public Set<String> getCacheNames() {
            Set<String> names = new HashSet<>(super.getCacheNames()) ;
            if (tdCacheProperties.isUseCaffeine()) {
                // 將 Caffeine 緩存的名稱也加入
                names.addAll(caffeineCacheManager.getCacheNames());
            }
            return names;
        }
    }

    /**
     * 內部類：實現兩級緩存的具體邏輯 (Caffeine L1 + Redis L2)。
     * 負責協調讀寫操作。
     */
    protected static class TwoLevelCache implements Cache {

        private final String name;
        private final Cache caffeineCache; // L1 緩存
        private final Cache redisCache;    // L2 緩存

        public TwoLevelCache(String name, Cache caffeineCache, Cache redisCache) {
            this.name = name;
            this.caffeineCache = caffeineCache;
            this.redisCache = redisCache;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object getNativeCache() {
            // 返回原生 Caffeine 緩存，或者您可以選擇返回一個包裝了兩者的對象
            return caffeineCache.getNativeCache();
        }

        @Override
        public ValueWrapper get(Object key) {
            // 1. 先從 L1 Caffeine 緩存讀取
            ValueWrapper valueWrapper = caffeineCache.get(key);
            if (valueWrapper != null) {
                // System.out.println("Cache Hit (L1-Caffeine): " + name + " -> " + key); // 可選：添加日誌
                return valueWrapper;
            }

            // 2. L1 未命中，從 L2 Redis 緩存讀取
            // System.out.println("Cache Miss (L1-Caffeine), trying L2-Redis: " + name + " -> " + key); // 可選：添加日誌
            valueWrapper = redisCache.get(key);
            if (valueWrapper != null) {
                // 3. L2 命中，回寫到 L1
                caffeineCache.put(key, valueWrapper.get());
                // System.out.println("Cache Hit (L2-Redis), put to L1: " + name + " -> " + key); // 可選：添加日誌
            } else {
                // System.out.println("Cache Miss (L2-Redis): " + name + " -> " + key); // 可選：添加日誌
            }
            return valueWrapper;
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            // 由於 get(key) 方法已經包含了 L1 和 L2 的邏輯，這裡直接調用並轉換
            ValueWrapper valueWrapper = get(key);
            if (valueWrapper == null) {
                return null;
            }
            return (T) valueWrapper.get();
        }

        @Override
        public <T> T get(Object key, Callable<T> valueLoader) {
            // 1. 先從 L1 Caffeine 緩存讀取
            T value = caffeineCache.get(key, valueLoader); // Caffeine 的 get(key, loader) 會處理回寫

            if (value != null) {
                // 如果 Caffeine 命中或通過 loader 加載了，這裡需要同步到 Redis
                // 注意：這裡假設 Caffeine 的 valueLoader 會在不命中時調用，且其結果就是最終結果
                // 如果 valueLoader 是從數據庫加載，那麼這個值也應該寫入 Redis
                // 簡單起見，這裡假設 get(key, loader) 已經處理了從數據源獲取
                // 如果 L1 命中了，就不會調用 valueLoader，直接返回
                // 如果 L1 不命中，會調用 valueLoader，並將結果存入 L1。
                // 此時需要將這個新加載的值也存入 L2
                if (caffeineCache.get(key) != null && redisCache.get(key) == null) {
                    // 只有當 Caffeine 中有而 Redis 中沒有時才往 Redis 中放
                    // 這裡的邏輯需要更嚴謹，通常是從數據源加載後，先存 L2，再存 L1
                    // 為了簡化，目前假設 Caffeine 的 loader 處理了數據源獲取，
                    // 並且我們需要確保數據源加載的數據也同步到 L2。
                    // 更好的方式是將 valueLoader 傳遞給 Redis 緩存，然後再回寫到 Caffeine。
                    // 這裡先簡單同步：
                    // System.out.println("Put to L2 after L1 get/load: " + name + " -> " + key); // 可選：添加日誌
                    redisCache.put(key, value);
                }
                return value;
            }

            // 如果 L1 也沒有，則嘗試從 L2 獲取
            T redisValue = redisCache.get(key, valueLoader); // Redis 的 get(key, loader)
            if (redisValue != null) {
                // 從 L2 獲取到後，回寫到 L1
                caffeineCache.put(key, redisValue);
            }
            return redisValue;
        }

        @Override
        public void put(Object key, Object value) {
            // 寫入操作：同時更新 L1 和 L2
            // System.out.println("Put to L1 & L2: " + name + " -> " + key); // 可選：添加日誌
            redisCache.put(key, value);
            caffeineCache.put(key, value);

            // TODO: 在分佈式環境下，當數據通過此服務寫入 Redis 後，
            //  需要通知其他服務實例的 Caffeine 緩存將此 key 失效。
            //  這通常需要通過 Redis Pub/Sub 機制來實現。
            //  例如：redisTemplate.convertAndSend("cache:invalidate", key);
        }

        @Override
        public void evict(Object key) {
            // 驅逐操作：同時從 L1 和 L2 移除
            // System.out.println("Evict from L1 & L2: " + name + " -> " + key); // 可選：添加日誌
            redisCache.evict(key);
            caffeineCache.evict(key);
            // TODO: 類似 put 操作，也需要發送消息通知其他節點失效此 key
        }

        @Override
        public void clear() {
            // 清除操作：同時清除 L1 和 L2
            // System.out.println("Clear L1 & L2: " + name); // 可選：添加日誌
            redisCache.clear();
            caffeineCache.clear();
            // TODO: 類似 put 操作，也需要發送消息通知其他節點清除整個緩存
        }
    }

}
