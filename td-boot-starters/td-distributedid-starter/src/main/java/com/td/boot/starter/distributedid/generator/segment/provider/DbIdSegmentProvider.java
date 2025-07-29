package com.td.boot.starter.distributedid.generator.segment.provider;

import com.td.boot.starter.distributedid.generator.segment.buffer.IdSegment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.Assert;

/**
 * 基於數據庫的 ID 號段提供者實現。
 * 通過數據庫事務和樂觀鎖（或行鎖）來保證號段獲取的原子性。
 */
@Slf4j
public class DbIdSegmentProvider implements IdSegmentProvider {

    private final JdbcTemplate jdbcTemplate;
    private final DataSourceTransactionManager transactionManager;

    public DbIdSegmentProvider(JdbcTemplate jdbcTemplate, DataSourceTransactionManager transactionManager) {
        Assert.notNull(jdbcTemplate, "JdbcTemplate 不能為空");
        Assert.notNull(transactionManager, "DataSourceTransactionManager 不能為空");
        this.jdbcTemplate = jdbcTemplate;
        this.transactionManager = transactionManager;
    }

    @Override
    public IdSegment getNextSegment(String bizKey) throws Exception {
        Assert.hasText(bizKey, "業務鍵不能為空");

        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW); // 確保在新的事務中獲取號段
        TransactionStatus status = transactionManager.getTransaction(def);

        try {
            // 1. 查詢當前 biz_key 的 max_id 和 step
            String selectSql = "SELECT max_id, step FROM t_segment_id_biz WHERE biz_key = ? FOR UPDATE"; // 使用行鎖
            Long[] currentData = jdbcTemplate.queryForObject(selectSql, new Object[]{bizKey}, (rs, rowNum) ->
                    new Long[]{rs.getLong("max_id"), (long) rs.getInt("step")});

            if (currentData == null) {
                throw new IllegalArgumentException("業務鍵 [" + bizKey + "] 不存在於號段配置表 t_segment_id_biz 中。");
            }

            long currentMaxId = currentData[0];
            int step = currentData[1].intValue();
            long newMaxId = currentMaxId + step; // 計算新的 max_id

            // 2. 更新 max_id
            String updateSql = "UPDATE t_segment_id_biz SET max_id = ? WHERE biz_key = ? AND max_id = ?"; // 樂觀鎖
            int rowsAffected = jdbcTemplate.update(updateSql, newMaxId, bizKey, currentMaxId);

            if (rowsAffected == 0) {
                // 如果更新失敗，說明在讀取後被其他事務修改了，進行重試 (這裡簡單拋異常，實際可以加入重試機制)
                throw new RuntimeException("獲取號段失敗，可能是並發衝突，請重試。bizKey: " + bizKey);
            }

            transactionManager.commit(status); // 提交事務

            log.info("成功從數據庫獲取號段: bizKey={}, currentMaxId={}, step={}, newMaxId={}",
                    bizKey, currentMaxId, step, newMaxId);

            return new IdSegment(currentMaxId + 1, step); // 返回新的號段，min 是 currentMaxId + 1
        } catch (Exception e) {
            transactionManager.rollback(status); // 回滾事務
            log.error("從數據庫獲取號段失敗: bizKey={}", bizKey, e);
            throw e;
        }
    }

}
