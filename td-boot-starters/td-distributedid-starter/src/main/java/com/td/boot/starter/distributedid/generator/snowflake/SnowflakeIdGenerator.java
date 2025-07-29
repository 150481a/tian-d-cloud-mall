package com.td.boot.starter.distributedid.generator.snowflake;

import com.td.boot.starter.distributedid.generator.IIdGenerator;
import com.td.boot.starter.distributedid.generator.snowflake.worker.WorkerIdAssigner;
import org.springframework.util.Assert;

import java.time.Instant;

/**
 * Snowflake 分佈式 ID 生成器實現。
 * 核心思想是：(符號位 1bit) + (時間戳 41bit) + (數據中心 ID 5bit) + (機器 ID 5bit) + (序列號 12bit) = 64bit。
 * 總共可支持 69 年，每毫秒支持 4096 個 ID。
 */
public class SnowflakeIdGenerator implements IIdGenerator {

    // 起始的時間戳，可以設置為系統上線時間，避免 ID 過小
    // 這裡使用 2024-01-01 00:00:00.000 (UTC+8) 作為默認起始時間，毫秒數
    private final long epoch = Instant.parse("2024-01-01T00:00:00.000+08:00").toEpochMilli();

    // 機器 ID 佔用的位數
    private final long workerIdBits = 5L;
    // 數據中心 ID 佔用的位數
    private final long datacenterIdBits = 5L;
    // 序列號佔用的位數
    private final long sequenceBits = 12L;

    // 機器 ID 最大值 (2^5 - 1)
    private final long maxWorkerId = ~(-1L << workerIdBits);
    // 數據中心 ID 最大值 (2^5 - 1)
    private final long maxDatacenterId = ~(-1L << datacenterIdBits);
    // 序列號最大值 (2^12 - 1)
    private final long sequenceMask = ~(-1L << sequenceBits);

    // 機器 ID 左移位數 (序列號位數)
    private final long workerIdShift = sequenceBits;
    // 數據中心 ID 左移位數 (序列號位數 + 機器 ID 位數)
    private final long datacenterIdShift = sequenceBits + workerIdBits;
    // 時間戳左移位數 (序列號位數 + 機器 ID 位數 + 數據中心 ID 位數)
    private final long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;

    private long datacenterId; // 數據中心 ID (0~31)
    private long workerId;     // 機器 ID (0~31)
    private long sequence = 0L; // 毫秒內序列 (0~4095)
    private long lastTimestamp = -1L; // 上次生成 ID 的時間戳

    // 鎖對象，用於保證線程安全
    private final Object lock = new Object();

    /**
     * 構造函數。
     * @param workerIdAssigner 工作節點 ID 分配器，用於獲取數據中心 ID 和機器 ID。
     */
    public SnowflakeIdGenerator(WorkerIdAssigner workerIdAssigner) {
        Assert.notNull(workerIdAssigner, "WorkerIdAssigner 不能為空");

        this.datacenterId = workerIdAssigner.getDatacenterId();
        this.workerId = workerIdAssigner.getWorkerId();

        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException(String.format("數據中心 ID 不能大於 %d 或小於 0", maxDatacenterId));
        }
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(String.format("工作節點 ID 不能大於 %d 或小於 0", maxWorkerId));
        }

        System.out.printf("SnowflakeIdGenerator 初始化成功，數據中心ID: %d, 工作節點ID: %d, 起始時間戳: %d%n",
                this.datacenterId, this.workerId, this.epoch);
    }

    @Override
    public long generateLongId() {
        return nextId();
    }

    @Override
    public String generateStringId() {
        return String.valueOf(nextId());
    }

    @Override
    public String generateStringId(String prefix) {
        Assert.hasText(prefix, "ID前綴不能為空");
        return prefix + nextId();
    }

    /**
     * 核心方法：生成下一個 ID。
     * 線程安全。
     *
     * @return 下一個唯一 ID
     */
    private long nextId() {
        synchronized (lock) {
            long timestamp = timeGen();

            if (timestamp < lastTimestamp) {
                // 如果當前時間小於上次 ID 生成時間，說明時鐘回撥，拋出異常
                throw new RuntimeException(String.format(
                        "時鐘回撥！拒絕為 %d 毫秒內的請求生成 ID，因為上次生成 ID 的時間是 %d 毫秒",
                        lastTimestamp - timestamp, lastTimestamp));
            }

            if (lastTimestamp == timestamp) {
                // 如果是同一毫秒內，則序列號遞增
                sequence = (sequence + 1) & sequenceMask;
                if (sequence == 0) {
                    // 毫秒內序列溢出，等到下一毫秒
                    timestamp = tilNextMillis(lastTimestamp);
                }
            } else {
                // 新的毫秒，序列號重置為 0
                sequence = 0L;
            }

            lastTimestamp = timestamp;

            // 組合成最終的 ID
            return ((timestamp - epoch) << timestampLeftShift) // 時間戳部分
                    | (datacenterId << datacenterIdShift)       // 數據中心 ID 部分
                    | (workerId << workerIdShift)               // 機器 ID 部分
                    | sequence;                                 // 序列號部分
        }
    }

    /**
     * 阻塞到下一個毫秒，直到獲得新的時間戳。
     *
     * @param lastTimestamp 上次生成 ID 的時間戳
     * @return 當前時間戳
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * 獲取當前時間戳（毫秒）。
     *
     * @return 當前時間戳
     */
    private long timeGen() {
        return System.currentTimeMillis();
    }
}
