package com.td.boot.starter.distributedid.generator.segment.provider;

import com.td.boot.starter.distributedid.generator.segment.buffer.IdSegment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.Assert;

/**
 * 基於 Redis 的 ID 號段提供者實現。
 * 利用 Redis 的原子性操作來獲取號段，性能較高。
 */
@Slf4j
public class RedisIdSegmentProvider implements IdSegmentProvider{

    private final StringRedisTemplate redisTemplate;
    private final int defaultStep; // 默認步長

    // Redis Key 前綴
    private static final String REDIS_KEY_PREFIX = "distributed_id:segment:";

    public RedisIdSegmentProvider(StringRedisTemplate redisTemplate, int defaultStep) {
        Assert.notNull(redisTemplate, "StringRedisTemplate 不能為空");
        Assert.isTrue(defaultStep > 0, "默認步長必須大於 0");
        this.redisTemplate = redisTemplate;
        this.defaultStep = defaultStep;
    }

    @Override
    public IdSegment getNextSegment(String bizKey) throws Exception {
        Assert.hasText(bizKey, "業務鍵不能為空");

        String redisKey = REDIS_KEY_PREFIX + bizKey;
        Long newMax = redisTemplate.opsForValue().increment(redisKey, defaultStep); // 原子性遞增

        if (newMax == null) {
            throw new RuntimeException("從 Redis 獲取號段失敗，Redis 返回 null。bizKey: " + bizKey);
        }

        long minId = newMax - defaultStep; // 號段起始值 (不包含舊值，從舊值+1開始)
        log.info("成功從 Redis 獲取號段: bizKey={}, minId={}, newMax={}, step={}",
                bizKey, minId, newMax, defaultStep);

        return new IdSegment(minId + 1, defaultStep); // 返回新的號段，min 是舊的 maxId + 1
    }

    /**
     * 初始化 Redis 中某個業務鍵的號段起始值。
     * 只有當 key 不存在時才會設置。
     */
    public void initSegment(String bizKey, long initialValue) {
        String redisKey = REDIS_KEY_PREFIX + bizKey;
        Boolean setSuccess = redisTemplate.opsForValue().setIfAbsent(redisKey, String.valueOf(initialValue));
        if (Boolean.TRUE.equals(setSuccess)) {
            log.info("Redis 號段 {} 初始化成功，初始值為 {}", bizKey, initialValue);
        } else {
            log.warn("Redis 號段 {} 已存在，無需初始化。", bizKey);
        }
    }
}
