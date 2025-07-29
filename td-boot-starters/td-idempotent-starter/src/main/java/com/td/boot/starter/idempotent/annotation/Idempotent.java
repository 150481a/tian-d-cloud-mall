package com.td.boot.starter.idempotent.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 冪等性註解。
 * 用於標記需要進行冪等性處理的方法。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * 冪等 Key 的生成策略。
     * 可以是 SpEL 表達式，例如 "#args[0].requestId"、"#request.getHeader('X-Request-Id')"。
     * 如果為空，則會嘗試使用默認的 Key 生成邏輯（例如，基於方法簽名和參數 Hash）。
     * @return 冪等 Key 的 SpEL 表達式
     */
    String key() default "";

    /**
     * 冪等 Key 的前綴。
     * 最終的 Key = prefix + 實際生成的 Key。
     * @return 冪等 Key 的前綴
     */
    String prefix() default "";

    /**
     * 冪等 Key 的過期時間，單位由 {@link #unit()} 指定。
     * 默認 5 分鐘。
     * @return 過期時間
     */
    long expireTime() default 5 * 60; // 默認 5 分鐘

    /**
     * 冪等 Key 的過期時間單位。
     * @return 時間單位
     */
    TimeUnit unit() default TimeUnit.SECONDS;

    /**
     * 當檢測到重複請求時，拋出的異常消息。
     * @return 重複請求的錯誤消息
     */
    String message() default "重複提交，請勿重複操作。";

}
