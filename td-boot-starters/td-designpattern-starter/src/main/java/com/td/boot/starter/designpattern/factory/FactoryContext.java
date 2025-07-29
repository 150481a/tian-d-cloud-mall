package com.td.boot.starter.designpattern.factory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;


/**
 * 工廠上下文。
 * 負責管理和提供應用程序中所有的工廠實現。
 * 它會掃描所有實現了 IFactory 接口的 Bean，並根據其 getFactoryType() 註冊。
 */
@Slf4j
@Component
public class FactoryContext implements ApplicationContextAware {
    // 存儲工廠實例的映射，key 為工廠類型（由 IFactory.getFactoryType() 定義），value 為工廠 Bean 實例
    private final Map<String, IFactory<?, ?>> factoryMap = new HashMap<>();

    /**
     * 根據工廠類型獲取對應的工廠實例。
     *
     * @param factoryType 工廠類型標識符
     * @param <T>         工廠能夠創建的產品類型
     * @param <P>         創建產品所需的參數類型
     * @return 對應的工廠實例
     * @throws IllegalArgumentException 如果找不到對應的工廠
     */
    @SuppressWarnings("unchecked")
    public <T, P> IFactory<T, P> getFactory(String factoryType) {
        Assert.hasText(factoryType, "工廠類型不能為空");
        IFactory<?, ?> factory = factoryMap.get(factoryType);
        if (factory == null) {
            throw new IllegalArgumentException("未找到類型為 [" + factoryType + "] 的工廠實現。");
        }
        return (IFactory<T, P>) factory;
    }

    /**
     * 在 Spring 應用上下文加載完成後，掃描所有 IFactory 實現並註冊。
     *
     * @param applicationContext Spring 應用上下文
     * @throws BeansException 如果在查找 Bean 時發生錯誤
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // 獲取所有 IFactory 類型的 Bean
        Map<String, IFactory> factories = applicationContext.getBeansOfType(IFactory.class);
        factories.values().forEach(factory -> {
            String factoryType = factory.getFactoryType();
            if (factoryType != null && !factoryType.isEmpty()) {
                if (factoryMap.containsKey(factoryType)) {
                    // 警告或拋出異常，如果存在重複的工廠類型
                    throw new IllegalStateException("檢測到重複的工廠類型 [" + factoryType + "]，請檢查配置。");
                }
                factoryMap.put(factoryType, factory);
            } else {
                // 如果工廠沒有指定 getFactoryType()，可以選擇記錄警告或忽略
                log.warn("工廠【{}】未指定工廠類型，將不會被註冊到 FactoryContext。",factory.getClass().getName());
            }
        });
    }
}
