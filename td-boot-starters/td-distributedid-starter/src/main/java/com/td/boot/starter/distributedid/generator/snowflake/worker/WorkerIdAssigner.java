package com.td.boot.starter.distributedid.generator.snowflake.worker;

/**
 * 工作節點 ID 分配器接口。
 * 用於在分佈式環境中獲取 Snowflake 算法所需的數據中心 ID 和工作節點 ID。
 * 不同的實現可以通過配置文件、數據庫、Zookeeper 或 Redis 來動態分配這些 ID。
 */
public interface WorkerIdAssigner {
    /**
     * 獲取當前應用實例的數據中心 ID。
     *
     * @return 數據中心 ID (通常 0-31)
     */
    long getDatacenterId();

    /**
     * 獲取當前應用實例的工作節點 ID (機器 ID)。
     *
     * @return 工作節點 ID (通常 0-31)
     */
    long getWorkerId();
}
