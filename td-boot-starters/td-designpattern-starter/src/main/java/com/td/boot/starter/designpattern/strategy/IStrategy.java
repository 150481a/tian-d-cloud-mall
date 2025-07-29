package com.td.boot.starter.designpattern.strategy;

/**
 * 策略模式的核心接口。
 * 所有具體的業務策略都必須實現此接口。
 *
 * @param <T> 策略方法接受的輸入參數類型
 * @param <R> 策略方法返回的結果類型
 */
public interface IStrategy<T, R> {
    /**
     * 執行策略定義的業務邏輯。
     *
     * @param param 策略執行所需的輸入參數
     * @return 策略執行後的結果
     */
    R execute(T param);
}
