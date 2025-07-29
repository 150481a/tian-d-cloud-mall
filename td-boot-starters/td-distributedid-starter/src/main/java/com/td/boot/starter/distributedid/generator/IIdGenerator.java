package com.td.boot.starter.distributedid.generator;

/**
 * 通用 ID 生成器接口。
 * 定義了生成不同類型 ID 的方法。
 */
public interface IIdGenerator {
    /**
     * 生成一個長整型 ID。
     * 常用於 Snowflake 算法。
     *
     * @return 生成的長整型 ID
     */
    long generateLongId();

    /**
     * 生成一個字符串型 ID。
     * 可以是 UUID 或其他特定格式的字符串 ID。
     *
     * @return 生成的字符串型 ID
     */
    String generateStringId();

    /**
     * 生成一個帶有指定前綴的字符串型 ID。
     *
     * @param prefix ID 前綴
     * @return 生成的帶前綴的字符串型 ID
     */
    String generateStringId(String prefix);
}
