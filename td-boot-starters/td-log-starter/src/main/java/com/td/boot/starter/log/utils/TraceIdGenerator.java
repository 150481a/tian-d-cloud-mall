package com.td.boot.starter.log.utils;

import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * TraceId 生成器。
 * 負責生成唯一的追踪 ID。
 */
public class TraceIdGenerator {

    /**
     * 生成一個新的 UUID 作為 TraceId。
     * @return 唯一的 TraceId
     */
    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 根據傳入的請求 ID 和當前生成的 TraceId 決定最終的 TraceId。
     * 如果請求 ID 不為空，則使用請求 ID；否則生成一個新的。
     * @param existingRequestId 請求頭中已有的請求 ID
     * @return 最終的 TraceId
     */
    public static String getOrCreateTraceId(String existingRequestId) {
        if (StringUtils.hasText(existingRequestId)) {
            return existingRequestId;
        }
        return generateTraceId();
    }

}
