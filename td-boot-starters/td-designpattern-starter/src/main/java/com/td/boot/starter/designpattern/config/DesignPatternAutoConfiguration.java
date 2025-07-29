package com.td.boot.starter.designpattern.config;

import com.td.boot.starter.designpattern.abstractfactory.AbstractFactoryContext;
import com.td.boot.starter.designpattern.chain.ChainContext;
import com.td.boot.starter.designpattern.event.DomainEventPublisher;
import com.td.boot.starter.designpattern.factory.FactoryContext;
import com.td.boot.starter.designpattern.properties.DesignPatternProperties;
import com.td.boot.starter.designpattern.strategy.StrategyContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 設計模式 Starter 的自動配置類。
 * 負責將策略模式相關的 Bean 註冊到 Spring 容器中。
 */
@Configuration
@EnableConfigurationProperties(DesignPatternProperties.class) // 啟用配置屬性綁定
@ConditionalOnMissingBean(DesignPatternAutoConfiguration.class) // 避免用戶重複定義
@ConditionalOnClass(DesignPatternProperties.class) // 確保 DesignPatternProperties 類別存在
public class DesignPatternAutoConfiguration {
    private final DesignPatternProperties properties;

    @Autowired
    public DesignPatternAutoConfiguration(DesignPatternProperties properties) {
        this.properties = properties;
    }

    /**
     * 將 StrategyContext 註冊為 Spring Bean。
     * 只有當容器中沒有 StrategyContext 類型的 Bean 時才創建。
     */
    @Bean
    @ConditionalOnMissingBean(StrategyContext.class) // 避免用戶重複定義
    public StrategyContext strategyContext() {
        return new StrategyContext();
    }

    /**
     * 將 FactoryContext 註冊為 Spring Bean。
     * 只有當容器中沒有 FactoryContext 類型的 Bean 時才創建。
     */
    @Bean
    @ConditionalOnMissingBean(FactoryContext.class) // 新增
    public FactoryContext factoryContext() {
        return new FactoryContext();
    }

    /**
     * 將 DomainEventPublisher 註冊為 Spring Bean。
     * 只有當容器中沒有 DomainEventPublisher 類型的 Bean 時才創建。
     */
    @Bean
    @ConditionalOnMissingBean(DomainEventPublisher.class) // 新增
    public DomainEventPublisher domainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        return new DomainEventPublisher(applicationEventPublisher);
    }

    /**
     * 将 ChainContext 注册为 Spring Bean。
     * 只有当容器中没有 ChainContext 类型的 Bean 时才创建。
     */
    @Bean
    @ConditionalOnMissingBean(ChainContext.class) // 新增
    public ChainContext chainContext() {
        return new ChainContext();
    }

    @Bean
    @ConditionalOnMissingBean(AbstractFactoryContext.class)
    public AbstractFactoryContext abstractFactoryContext() {
        return new AbstractFactoryContext();
    }

    // 後續可以根據 properties 控制其他設計模式組件的啟用與禁用
    // 提醒：原先在此處的 MessageSource 和 LocalValidatorFactoryBean 如果是在 td-convention-starter 中定義的，
    // 則不需要在此模塊重複定義。這裡確保只有設計模式相關的 Bean。
}
