package com.td.boot.starter.idempotent.storage;

import com.td.boot.starter.idempotent.properties.IdempotentProperties;

import java.util.concurrent.TimeUnit;

/**
 * 冪等性存儲接口。
 * 定義了冪等 Key 的存儲、檢查和釋放操作。
 */
public interface IdempotentStorage {

    /**
     * 嘗試鎖定一個冪等 Key。
     * 如果 Key 不存在，則存儲並返回 true；如果 Key 已存在，則返回 false。
     *
     * @param key 冪等 Key
     * @param expireTime 過期時間
     * @param unit 時間單位
     * @return true 表示成功鎖定（首次請求），false 表示 Key 已存在（重複請求）
     */
    boolean tryLock(String key, long expireTime, TimeUnit unit);

    /**
     * 釋放一個冪等 Key。
     * 在業務處理完成後調用，表示該 Key 对应的操作已完成，可以清理。
     *
     * @param key 冪等 Key
     */
    void releaseLock(String key);

    /**
     * 檢查一個冪等 Key 是否存在。
     *
     * @param key 冪等 Key
     * @return true 如果 Key 存在，false 如果 Key 不存在
     */
    boolean isLocked(String key);

    /**
     * 獲取存儲類型，用於自動配置時區分不同的存儲實現。
     * @return 存儲類型
     */
    IdempotentProperties.StoreType getStoreType();
}
