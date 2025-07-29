package com.td.boot.starter.designpattern.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "td.designpattern")
public class DesignPatternProperties {
    // 新增：是否啟用策略模式相關的自動配置
    private boolean enableStrategyPattern = true;

    // 新增：是否啟用工廠模式相關的自動配置
    private boolean enableFactoryPattern = true;

    // 新增：是否啟用事件發布訂閱相關的自動配置
    private boolean enableEventPublishing = true;

    // 新增：是否启用责任链模式相关自动配置
    private boolean enableChainOfResponsibility = true;
    // 新增：是否啟用抽象工廠模式相關的自動配置
    private boolean enableAbstractFactory = true;
    // 新增：是否啟用模板方法模式相關的自動配置
//    private boolean enableTemplateMethodPattern = true;
    // 新增：是否啟用享元模式相關的自動配置
//    private boolean enableFlyweightPattern = true;
//    private boolean enableProxyPattern = true;
    // 新增：是否啟用建造者模式相關的自動配置
//    private boolean enableBuilderPattern = true;
    // 新增：是否啟用原型模式相關的自動配置
//    private boolean enablePrototypePattern = true;

}
