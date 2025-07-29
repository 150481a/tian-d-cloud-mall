package com.td.boot.starter.distributedid.generator.segment;

import com.td.boot.starter.distributedid.generator.IIdGenerator;
import com.td.boot.starter.distributedid.generator.segment.buffer.IdSegment;
import com.td.boot.starter.distributedid.generator.segment.buffer.RingBuffer;
import com.td.boot.starter.distributedid.generator.segment.provider.IdSegmentProvider;
import com.td.boot.starter.distributedid.properties.DistributedIdProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 號段模式 ID 生成器實現。
 * 為不同的業務鍵（bizKey）管理獨立的 ID 號段。
 * 採用雙緩衝區機制和異步填充，確保高可用和高性能。
 */
@Slf4j
public class SegmentIdGenerator implements IIdGenerator {

    private final Map<String, RingBuffer> ringBufferMap = new ConcurrentHashMap<>();
    private final IdSegmentProvider idSegmentProvider;
    private final DistributedIdProperties.SegmentProperties segmentProperties;
    private final ExecutorService executorService; // 用於異步填充號段

    public SegmentIdGenerator(IdSegmentProvider idSegmentProvider, DistributedIdProperties properties) {
        Assert.notNull(idSegmentProvider, "IdSegmentProvider 不能為空");
        Assert.notNull(properties, "DistributedIdProperties 不能為空");
        this.idSegmentProvider = idSegmentProvider;
        this.segmentProperties = properties.getSegment();

        // 初始化業務鍵對應的環形緩衝區
        for (String bizKey : segmentProperties.getBizKeys()) {
            ringBufferMap.put(bizKey, new RingBuffer());
            log.info("為業務鍵 {} 初始化 Segment RingBuffer", bizKey);
        }

        // 創建異步填充線程池
        this.executorService = new ThreadPoolExecutor(
                segmentProperties.getCorePoolSize(),
                segmentProperties.getMaxPoolSize(),
                segmentProperties.getKeepAliveTimeSeconds(),
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(segmentProperties.getQueueCapacity()),
                r -> new Thread(r, "SegmentIdWorker-" + r.hashCode()),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        // 啟動預填充邏輯
        startPreloadSegments();
    }

    /**
     * 啟動所有業務鍵的號段預填充。
     */
    private void startPreloadSegments() {
        ringBufferMap.forEach((bizKey, ringBuffer) -> {
            // 初始填充兩個緩衝區，確保 ID 服務可用
            try {
                // 填充第一個緩衝區
                IdSegment segment1 = idSegmentProvider.getNextSegment(bizKey);
                ringBuffer.fillSegment(0, segment1);

                // 填充第二個緩衝區 (可選，但推薦以確保雙緩衝區都有數據)
                IdSegment segment2 = idSegmentProvider.getNextSegment(bizKey);
                ringBuffer.fillSegment(1, segment2);

            } catch (Exception e) {
                log.error("初始填充業務鍵 {} 的號段失敗，ID 服務可能不可用！", bizKey, e);
                // 這裡可以選擇拋出 RuntimeException 阻止服務啟動，或者記錄錯誤並讓後續請求觸發重試
            }
        });
    }


    @Override
    public long generateLongId() {
        // 號段模式通常需要一個 bizKey 來區分不同的 ID 類型
        // 這裡提供一個默認的生成方式，如果沒有傳入 bizKey，使用 defaultBizKey
        return generateLongId(segmentProperties.getDefaultBizKey());
    }

    @Override
    public String generateStringId() {
        return String.valueOf(generateLongId(segmentProperties.getDefaultBizKey()));
    }

    @Override
    public String generateStringId(String prefix) {
        Assert.hasText(prefix, "ID前綴不能為空");
        return prefix + generateLongId(segmentProperties.getDefaultBizKey());
    }

    /**
     * 生成指定業務鍵的長整型 ID。
     *
     * @param bizKey 業務鍵
     * @return 生成的 ID
     */
    public long generateLongId(String bizKey) {
        Assert.hasText(bizKey, "業務鍵不能為空");
        RingBuffer ringBuffer = ringBufferMap.get(bizKey);
        if (ringBuffer == null) {
            throw new IllegalArgumentException("未找到業務鍵 [" + bizKey + "] 對應的號段緩衝區，請檢查配置。");
        }

        while (true) {
            IdSegment currentSegment = ringBuffer.getCurrentSegment();
            if (currentSegment == null) {
                // 緩衝區為空，嘗試填充 (首次啟動或錯誤恢復時可能發生)
                log.warn("業務鍵 {} 的當前號段為空，嘗試緊急填充。", bizKey);
                try {
                    IdSegment newSegment = idSegmentProvider.getNextSegment(bizKey);
                    ringBuffer.fillSegment(ringBuffer.currentPos, newSegment); // 填充當前緩衝區
                    currentSegment = ringBuffer.getCurrentSegment();
                } catch (Exception e) {
                    log.error("緊急填充業務鍵 {} 的號段失敗，請檢查號段提供者！", bizKey, e);
                    throw new RuntimeException("獲取 ID 失敗：號段填充異常。", e);
                }
            }

            long id = currentSegment.getAndIncrement();
            if (id != -1) { // 成功從當前號段獲取到 ID
                // 判斷當前號段是否即將用盡，如果用盡則異步觸發下一個號段的獲取
                if (currentSegment.isAlmostExhausted() && System.currentTimeMillis() > currentSegment.getNextReadyTime()) {
                    preloadNextSegment(bizKey, ringBuffer);
                }
                return id;
            } else { // 當前號段已用盡，需要切換到下一個號段
                log.info("業務鍵 {} 的當前號段已用盡，準備切換緩衝區。", bizKey);
                // 嘗試切換到下一個緩衝區
                try {
                    // 等待下一個緩衝區準備好（可能需要等待異步填充完成）
                    IdSegment nextSegment = ringBuffer.getNextSegment();
                    // 如果下一個號段仍為空或不健全，則嘗試重新填充
                    if (nextSegment == null || nextSegment.isExhausted() || nextSegment.getUpdateTime() == 0) {
                        log.warn("業務鍵 {} 的下一個號段未準備好或已用盡，嘗試緊急填充。", bizKey);
                        IdSegment newSegment = idSegmentProvider.getNextSegment(bizKey);
                        ringBuffer.fillSegment(ringBuffer.getNextPos(), newSegment);
                        // 更新 nextReadyTime，防止頻繁觸發
                        newSegment.setNextReadyTime(System.currentTimeMillis() + segmentProperties.getPreloadIntervalMillis());
                    }
                    ringBuffer.switchSegment(); // 切換緩衝區
                    // 再次循環，從新的緩衝區獲取 ID
                } catch (Exception e) {
                    log.error("業務鍵 {} 號段切換或緊急填充失敗，請檢查號段提供者！", bizKey, e);
                    throw new RuntimeException("獲取 ID 失敗：號段切換異常。", e);
                }
            }
        }
    }

    /**
     * 異步預加載下一個號段。
     *
     * @param bizKey 業務鍵
     * @param ringBuffer 環形緩衝區
     */
    private void preloadNextSegment(String bizKey, RingBuffer ringBuffer) {
        // 使用 AtomicBoolean 避免重複觸發，如果已有任務在執行，則跳過
        AtomicBoolean isLoading = new AtomicBoolean(false); // 這裡需要每個 RingBuffer 有自己的 isLoading 標誌
        // 可以在 RingBuffer 類中添加此字段
        // 為了簡化，這裡直接用一個全局標誌，但更好的方式是 RingBuffer 內部管理其加載狀態
        // 或者使用一個 ConcurrentHashMap<String, AtomicBoolean> 來管理每個 bizKey 的加載狀態

        // 簡單實現：如果下一個緩衝區是空的，則觸發加載
        if (ringBuffer.isSegmentEmpty(ringBuffer.getNextPos())) {
            // 這裡可以考慮加一個鎖或AtomicBoolean來防止多個線程同時觸發預加載
            executorService.submit(() -> {
                try {
                    IdSegment newSegment = idSegmentProvider.getNextSegment(bizKey);
                    ringBuffer.fillSegment(ringBuffer.getNextPos(), newSegment);
                    // 更新 nextReadyTime，防止頻繁觸發
                    newSegment.setNextReadyTime(System.currentTimeMillis() + segmentProperties.getPreloadIntervalMillis());
                } catch (Exception e) {
                    log.error("異步預加載業務鍵 {} 的號段失敗！", bizKey, e);
                }
            });
        }
    }

}
