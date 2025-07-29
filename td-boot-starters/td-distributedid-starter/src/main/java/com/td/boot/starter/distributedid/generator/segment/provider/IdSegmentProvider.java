package com.td.boot.starter.distributedid.generator.segment.provider;


import com.td.boot.starter.distributedid.generator.segment.buffer.IdSegment;

/**
 * ID 號段提供者接口。
 * 定義了從外部源（如數據庫或 Redis）獲取新號段的方法。
 */
public interface IdSegmentProvider {

    /**
     * 獲取指定業務鍵的下一個 ID 號段。
     * 這是核心方法，需要保證在分佈式環境下的原子性操作。
     *
     * @param bizKey 業務鍵，例如 "order_id", "user_id"
     * @return 新獲取的號段
     * @throws Exception 如果獲取失敗
     */
    IdSegment getNextSegment(String bizKey) throws Exception;
}
