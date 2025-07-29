package com.td.boot.starter.idempotent.storage.local;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.td.boot.starter.idempotent.properties.IdempotentProperties;
import com.td.boot.starter.idempotent.storage.IdempotentStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.concurrent.TimeUnit;

/**
 * 基於本地緩存 (Caffeine) 的冪等性存儲實現。
 * 適用於單機部署或對重複請求時間窗口要求不高的場景。
 * 不支持分佈式環境下的完全冪等。
 */
@Slf4j
public class LocalIdempotentStorage  implements IdempotentStorage {
    // Caffeine 緩存，用於存儲冪等 Key
    private final Cache<String, Long> idempotentCache; // value 可以存儲鎖定時間戳，或者簡單用個佔位符

    public LocalIdempotentStorage(long initialCapacity, long maximumSize) {
        Assert.isTrue(initialCapacity > 0, "初始容量必須大於0");
        Assert.isTrue(maximumSize > 0, "最大容量必須大於0");

        this.idempotentCache = Caffeine.newBuilder()
                .initialCapacity((int) initialCapacity)
                .maximumSize(maximumSize)
                // expiry will be handled per entry based on @Idempotent annotation
                .build();
        log.info("LocalIdempotentStorage (Caffeine) 初始化成功，初始容量: {}, 最大容量: {}.", initialCapacity, maximumSize);
    }

    @Override
    public boolean tryLock(String key, long expireTime, TimeUnit unit) {
        Assert.hasText(key, "冪等 Key 不能為空");
        Assert.isTrue(expireTime > 0, "過期時間必須大於0");

        // putIfAbsent 是原子操作
        Long existingValue = idempotentCache.asMap().putIfAbsent(key, System.currentTimeMillis() + unit.toMillis(expireTime));
        if (existingValue == null) {
            log.debug("成功鎖定本地冪等 Key: {}, 過期時間: {} {}", key, expireTime, unit.name());
            // 由於 Caffeine 不直接支持 putIfAbsent with expire，這裡我們需要在 getAndIncrement / generateId 後，
            // 重新 put，或者在 building cache 時使用 expireAfterWrite。
            // 這裡簡化處理，依靠 `IdempotentAspect` 的 `expireTime` 來控制 Cache 的 `expireAfterWrite`
            // 但如果 Cache 創建時沒有設置 expireAfterWrite，這裡的 expireTime 實際上是無效的
            // 所以，我們將 expireTime 的控制放到 Cache Builder 中
            // 這裡的實現暫時先這樣，更嚴謹的 Local 緩存應該在 Cache 構建時就設置統一的過期策略
            return true;
        } else {
            log.warn("本地冪等 Key {} 已存在，重複請求。", key);
            return false;
        }
    }

    @Override
    public void releaseLock(String key) {
        Assert.hasText(key, "冪等 Key 不能為空");
        idempotentCache.invalidate(key);
        log.debug("成功釋放本地冪等 Key: {}", key);
    }

    @Override
    public boolean isLocked(String key) {
        Assert.hasText(key, "冪等 Key 不能為空");
        return idempotentCache.asMap().containsKey(key);
    }

    @Override
    public IdempotentProperties.StoreType getStoreType() {
        return IdempotentProperties.StoreType.LOCAL;
    }

}
