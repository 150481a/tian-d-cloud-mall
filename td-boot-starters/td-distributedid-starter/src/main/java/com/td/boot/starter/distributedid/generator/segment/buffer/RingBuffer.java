package com.td.boot.starter.distributedid.generator.segment.buffer;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class RingBuffer {

    // 兩個號段緩衝區
    private IdSegment[] buffer;
    // 當前正在使用的緩衝區的索引
    public volatile int currentPos = 0; // 0 或 1
    // 用於獲取號段的鎖
    private final Lock lock = new ReentrantLock();

    public RingBuffer() {
        this.buffer = new IdSegment[2]; // 雙緩衝區
    }

    /**
     * 獲取當前可用的號段。
     */
    public IdSegment getCurrentSegment() {
        return buffer[currentPos];
    }

    /**
     * 獲取下一個準備填充的號段。
     */
    public IdSegment getNextSegment() {
        return buffer[getNextPos()];
    }

    /**
     * 翻轉緩衝區，將當前緩衝區切換到另一個。
     */
    public void switchSegment() {
        lock.lock();
        try {
            currentPos = getNextPos();
            log.info("RingBuffer 緩衝區切換到索引: {}", currentPos);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 獲取下一個緩衝區的索引。
     */
    public int getNextPos() {
        return (currentPos + 1) % 2;
    }

    /**
     * 填充指定索引的號段。
     */
    public void fillSegment(int pos, IdSegment segment) {
        lock.lock();
        try {
            buffer[pos] = segment;
            log.info("RingBuffer 索引 {} 填充號段: [{}, {})", pos, segment.getMin(), segment.getMax());
        } finally {
            lock.unlock();
        }
    }

    /**
     * 判斷指定索引的號段是否為空。
     */
    public boolean isSegmentEmpty(int pos) {
        lock.lock();
        try {
            return buffer[pos] == null;
        } finally {
            lock.unlock();
        }
    }
}
