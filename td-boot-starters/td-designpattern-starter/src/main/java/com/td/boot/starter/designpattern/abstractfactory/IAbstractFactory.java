package com.td.boot.starter.designpattern.abstractfactory;

/**
 * 抽象工廠接口。
 * 聲明了一組用於創建不同抽象產品的方法。
 * 使用泛型 <PRODUCT_A, PRODUCT_B> 來表示其生產的兩種產品類型，
 * 這些具體的產品類型將由使用方在實現時定義。
 *
 * @param <PRODUCT_A> 工廠能夠創建的第一種抽象產品類型
 * @param <PRODUCT_B> 工廠能夠創建的第二種抽象產品類型
 */
public interface IAbstractFactory<PRODUCT_A, PRODUCT_B> {
    /**
     * 創建抽象產品A的實例。
     * @return 抽象產品A的實例
     */
    PRODUCT_A createProductA();

    /**
     * 創建抽象產品B的實例。
     * @return 抽象產品B的實例
     */
    PRODUCT_B createProductB();

    /**
     * 獲取抽象工廠的唯一標識。
     * 通常用於從 AbstractFactoryContext 中根據標識獲取對應的抽象工廠。
     *
     * @return 工廠的唯一標識字符串
     */
    String getFactoryType();
}
