package com.td.boot.starter.designpattern.strategy.annotation;

import java.lang.annotation.*;

/**
 * 用於標識具體策略的類型或標識符。
 * 結合 StrategyContext 使用，以便根據類型選擇對應的策略。
 */
@Target(ElementType.TYPE) // 作用於類上
@Retention(RetentionPolicy.RUNTIME) // 運行時保留
@Documented // 生成到文檔中
public @interface StrategyType {
    /**
     * 策略的唯一類型或標識符。
     * 可以是一個字符串、枚舉或其他可區分的類型。
     */
    String value();
}
