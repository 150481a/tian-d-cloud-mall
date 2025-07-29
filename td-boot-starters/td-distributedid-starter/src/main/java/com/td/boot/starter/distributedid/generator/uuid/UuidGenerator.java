package com.td.boot.starter.distributedid.generator.uuid;

import com.td.boot.starter.distributedid.generator.IIdGenerator;
import org.springframework.util.Assert;

import java.util.UUID;

/**
 * UUID ID 生成器實現。
 * 生成 128 位 UUID，全局唯一但無序。
 */
public class UuidGenerator implements IIdGenerator {

    @Override
    public long generateLongId() {
        // UUID 主要用於字符串 ID，將其轉換為 Long 可能會丟失唯一性或導致碰撞，
        // 且通常不推薦將無序的 UUID 轉為 Long ID，可能不是趨勢遞增。
        // 這裡為了實現 IIdGenerator 接口，簡單地做 hash 或截取，實際應用中應避免。
        // 如果業務確實需要 Long 類型且不關心有序性，可以考慮使用 UUID 的 hashCode 或 BigInteger 轉換。
        // 但更好的做法是：如果需要 Long ID，就用 Snowflake 或 Segment；需要 String ID，就用 UUID。
        return UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE; // 取高64位，並轉為正數
    }

    @Override
    public String generateStringId() {
        return UUID.randomUUID().toString().replace("-", ""); // 移除橫線，更緊湊
    }

    @Override
    public String generateStringId(String prefix) {
        Assert.hasText(prefix, "ID前綴不能為空");
        return prefix + generateStringId();
    }

}
