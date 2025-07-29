package com.td.boot.starter.designpattern.abstractfactory;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * 抽象工廠上下文。
 * 負責管理和提供應用程序中所有的抽象工廠實現。
 * 它會掃描所有實現了 IAbstractFactory 接口的 Bean，並根據其 getFactoryType() 註冊。
 */
@Component
public class AbstractFactoryContext implements ApplicationContextAware {

    // 存儲抽象工廠實例的映射，key 為工廠類型，value 為工廠 Bean 實例
    // 使用非泛型 IAbstractFactory 以便在 Map 中存儲不同泛型參數的工廠
    private final Map<String, IAbstractFactory<?, ?>> factoryMap = new HashMap<>();

    /**
     * 根據工廠類型獲取對應的抽象工廠實例。
     *
     * @param factoryType 工廠類型標識符
     * @param <PRODUCT_A> 工廠能夠創建的第一種抽象產品類型
     * @param <PRODUCT_B> 工廠能夠創建的第二種抽象產品類型
     * @return 對應的抽象工廠實例
     * @throws IllegalArgumentException 如果找不到對應的抽象工廠
     */
    @SuppressWarnings("unchecked") // 由於泛型擦除，此處需要強制轉換，但邏輯保證類型安全
    public <PRODUCT_A, PRODUCT_B> IAbstractFactory<PRODUCT_A, PRODUCT_B> getFactory(String factoryType) {
        Assert.hasText(factoryType, "抽象工廠類型不能為空");
        IAbstractFactory<?, ?> factory = factoryMap.get(factoryType);
        if (factory == null) {
            throw new IllegalArgumentException("未找到類型為 [" + factoryType + "] 的抽象工廠實現。");
        }
        return (IAbstractFactory<PRODUCT_A, PRODUCT_B>) factory;
    }

    /**
     * 在 Spring 應用上下文加載完成後，掃描所有 IAbstractFactory 實現並註冊。
     *
     * @param applicationContext Spring 應用上下文
     * @throws BeansException 如果在查找 Bean 時發生錯誤
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // 獲取所有 IAbstractFactory 類型的 Bean
        Map<String, IAbstractFactory> factories = applicationContext.getBeansOfType(IAbstractFactory.class);
        factories.values().forEach(factory -> {
            String factoryType = factory.getFactoryType();
            if (factoryType != null && !factoryType.isEmpty()) {
                if (factoryMap.containsKey(factoryType)) {
                    throw new IllegalStateException("檢測到重複的抽象工廠類型 [" + factoryType + "]，請檢查配置。");
                }
                factoryMap.put(factoryType, factory);
            } else {
                System.out.println("警告：抽象工廠 " + factory.getClass().getName() + " 未指定工廠類型，將不會被註冊到 AbstractFactoryContext。");
            }
        });
        System.out.println("AbstractFactoryContext 成功註冊了 " + factoryMap.size() + " 個抽象工廠。");
    }
}
