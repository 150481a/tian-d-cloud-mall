package com.td.boot.starter.distributedid.generator.segment.buffer;

import lombok.Data;

/**
 * ID 號段。
 * 包含號段的起始值、最大值和步長。
 */
@Data
public class IdSegment {

    private long max;      // 號段的最大值 (不包含)
    private long min;      // 號段的最小值 (包含)
    private volatile long current; // 當前已使用的 ID 值
    private int step;      // 號段的步長

    // 擴展字段，用於標識這個號段是否可用 (例如，從數據庫獲取號段時，是否已過期或無效)
    private long updateTime; // 號段更新時間
    private long nextReadyTime; // 下一個號段準備好時間

    public IdSegment(long min, int step) {
        this.min = min;
        this.max = min + step;
        this.current = min;
        this.step = step;
        this.updateTime = System.currentTimeMillis();
        this.nextReadyTime = System.currentTimeMillis() + 1000 * 60 * 5; // 默認 5 分鐘後準備獲取下一個號段
    }

    /**
     * 從當前號段中獲取下一個 ID。
     *
     * @return 下一個 ID，如果號段已用完則返回 -1
     */
    public synchronized long getAndIncrement() {
        if (current < max) {
            return current++;
        }
        return -1; // 號段用盡
    }

    /**
     * 判斷當前號段是否即將用盡 (例如，已使用超過 90%)
     *
     * @return true 如果即將用盡，需要觸發下一號段獲取
     */
    public boolean isAlmostExhausted() {
        // 當前已使用量超過總量的一個閾值（例如 90%）
        return (current - min) * 1.0 / step > 0.9;
    }

    /**
     * 判斷號段是否已經用盡。
     */
    public boolean isExhausted() {
        return current >= max;
    }
}
