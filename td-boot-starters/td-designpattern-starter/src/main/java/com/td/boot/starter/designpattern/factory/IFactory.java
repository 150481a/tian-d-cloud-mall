package com.td.boot.starter.designpattern.factory;

/**
 * 通用工廠接口。
 * 定義了所有具體工廠必須實現的創建方法。
 *
 * @param <T> 工廠能夠創建的產品類型
 * @param <P> 創建產品所需的參數類型
 */
public interface IFactory<T, P> {
    /**
     * 根據給定的參數創建一個產品實例。
     *
     * @param param 創建產品所需的參數
     * @return 創建的產品實例
     */
    T create(P param);

    /**
     * 獲取工廠的唯一標識。
     * 通常用於從 FactoryContext 中根據標識獲取對應的工廠。
     *
     * @return 工廠的唯一標識字符串
     */
    String getFactoryType();
}
