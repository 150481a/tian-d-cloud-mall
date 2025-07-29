package com.td.boot.starter.distributedid.generator.snowflake.worker;

import com.td.boot.starter.distributedid.properties.DistributedIdProperties;

/**
 * 默認的工作節點 ID 分配器實現。
 * 從 DistributedIdProperties 中獲取配置的數據中心 ID 和工作節點 ID。
 * 適用於 ID 預先分配或通過環境變量注入的場景。
 */
public class DefaultWorkerIdAssigner implements WorkerIdAssigner {
    private final DistributedIdProperties properties;

    public DefaultWorkerIdAssigner(DistributedIdProperties properties) {
        this.properties = properties;
    }

    @Override
    public long getDatacenterId() {
        return properties.getSnowflake().getDatacenterId();
    }

    @Override
    public long getWorkerId() {
        return properties.getSnowflake().getWorkerId();
    }
}
