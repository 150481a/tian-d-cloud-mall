package com.td.boot.starter.designpattern.strategy;

import com.td.boot.starter.designpattern.strategy.annotation.StrategyType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * 策略上下文。
 * 負責管理和提供應用程序中所有的策略實現。
 * 它會掃描所有帶有 @StrategyType 註解的 IStrategy 實現，並將其註冊到內部映射中。
 */
@Slf4j
@Component
public class StrategyContext implements ApplicationContextAware {

    // 存儲策略實例的映射，key 為策略類型（由 @StrategyType 定義），value 為策略 Bean 實例
    private final Map<String, IStrategy<?, ?>> strategyMap = new HashMap<>();

    /**
     * 根據策略類型獲取對應的策略實例。
     *
     * @param strategyType 策略類型標識符
     * @param <T>          策略方法接受的輸入參數類型
     * @param <R>          策略方法返回的結果類型
     * @return 對應的策略實例
     * @throws IllegalArgumentException 如果找不到對應的策略
     */
    @SuppressWarnings("unchecked") // 由於泛型擦除和類型安全的設計，此處需要類型轉換，但邏輯保證類型安全
    public <T, R> IStrategy<T, R> getStrategy(String strategyType) {
        Assert.hasText(strategyType, "策略類型不能為空");
        IStrategy<?, ?> strategy = strategyMap.get(strategyType);
        if (strategy == null) {
            throw new IllegalArgumentException("未找到類型為 [" + strategyType + "] 的策略實現。");
        }
        return (IStrategy<T, R>) strategy;
    }

    /**
     * 執行指定類型的策略。
     *
     * @param strategyType 策略類型標識符
     * @param param        策略執行所需的輸入參數
     * @param <T>          輸入參數類型
     * @param <R>          返回結果類型
     * @return 策略執行後的結果
     * @throws IllegalArgumentException 如果找不到對應的策略
     */
    public <T, R> R executeStrategy(String strategyType, T param) {
        return (R) getStrategy(strategyType).execute(param);
    }

    /**
     * 在 Spring 應用上下文加載完成後，掃描所有 IStrategy 實現並註冊。
     *
     * @param applicationContext Spring 應用上下文
     * @throws BeansException 如果在查找 Bean 時發生錯誤
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // 獲取所有 IStrategy 類型的 Bean
        Map<String, IStrategy> strategies = applicationContext.getBeansOfType(IStrategy.class);
        strategies.values().forEach(strategy -> {
            // 獲取策略上的 @StrategyType 註解
            StrategyType strategyType = strategy.getClass().getAnnotation(StrategyType.class);
            if (strategyType != null && !strategyType.value().isEmpty()) {
                if (strategyMap.containsKey(strategyType.value())) {
                    // 警告或拋出異常，如果存在重複的策略類型
                    throw new IllegalStateException("檢測到重複的策略類型 [" + strategyType.value() + "]，請檢查配置。");
                }
                strategyMap.put(strategyType.value(), strategy);
            } else {
                // 如果策略沒有註解 @StrategyType，可以選擇記錄警告或忽略
                log.info("警告：策略【{}】未指定 @StrategyType，將不會被註冊到 StrategyContext。", strategy.getClass().getName());
            }
        });
        // 可以在這裡打印註冊的策略數量，用於調試
        log.info("StrategyContext 成功註冊了【{}】個策略。", strategyMap.size());
    }
}
