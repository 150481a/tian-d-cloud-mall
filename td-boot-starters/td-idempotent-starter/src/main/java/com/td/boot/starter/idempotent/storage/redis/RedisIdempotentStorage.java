package com.td.boot.starter.idempotent.storage.redis;

import com.td.boot.starter.idempotent.properties.IdempotentProperties;
import com.td.boot.starter.idempotent.storage.IdempotentStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.util.Assert;

import java.util.concurrent.TimeUnit;

/**
 * 基於 Redis 的冪等性存儲實現。
 * 使用 Redis 的 SETNX (SET if Not eXists) 命令實現鎖定，確保原子性。
 */
@Slf4j
public class RedisIdempotentStorage implements IdempotentStorage {

    private final StringRedisTemplate redisTemplate;
    // 使用 Lua 腳本來保證原子性地設置 Key 和過期時間
    private static final String SET_IF_ABSENT_SCRIPT =
            "if redis.call('setnx', KEYS[1], ARGV[1]) == 1 then redis.call('expire', KEYS[1], ARGV[2]) return 1 else return 0 end";
    private static final RedisScript<Long> SET_IF_ABSENT_REDIS_SCRIPT = new DefaultRedisScript<>(SET_IF_ABSENT_SCRIPT, Long.class);


    public RedisIdempotentStorage(StringRedisTemplate redisTemplate) {
        Assert.notNull(redisTemplate, "StringRedisTemplate 不能為空");
        this.redisTemplate = redisTemplate;
        log.info("RedisIdempotentStorage 初始化成功。");
    }

    @Override
    public boolean tryLock(String key, long expireTime, TimeUnit unit) {
        Assert.hasText(key, "冪等 Key 不能為空");
        Assert.isTrue(expireTime > 0, "過期時間必須大於0");

        // 使用 RedisScript 執行 SETNX 和 EXPIRE，保證原子性
        // value 可以是任意值，這裡用 '1' 表示已鎖定
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "1", expireTime, unit);

        // 如果要使用更複雜的邏輯，例如 SET value EX PX/EX NX
        // 或者使用 Lua 腳本
        // Long result = redisTemplate.execute(SET_IF_ABSENT_REDIS_SCRIPT,
        //         Collections.singletonList(key), "1", String.valueOf(unit.toSeconds(expireTime)));
        // boolean success = result != null && result == 1L;

        if (Boolean.TRUE.equals(success)) {
            log.debug("成功鎖定冪等 Key: {}, 過期時間: {} {}", key, expireTime, unit.name());
            return true;
        } else {
            log.warn("冪等 Key {} 已存在，重複請求。", key);
            return false;
        }
    }

    @Override
    public void releaseLock(String key) {
        Assert.hasText(key, "冪等 Key 不能為空");
        Boolean deleted = redisTemplate.delete(key);
        if (Boolean.TRUE.equals(deleted)) {
            log.debug("成功釋放冪等 Key: {}", key);
        } else {
            log.warn("冪等 Key {} 釋放失敗或 Key 不存在。", key);
        }
    }

    @Override
    public boolean isLocked(String key) {
        Assert.hasText(key, "冪等 Key 不能為空");
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public IdempotentProperties.StoreType getStoreType() {
        return IdempotentProperties.StoreType.REDIS;
    }

}
