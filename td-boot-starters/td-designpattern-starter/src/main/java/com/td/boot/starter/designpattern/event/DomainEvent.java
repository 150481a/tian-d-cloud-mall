package com.td.boot.starter.designpattern.event;

import org.springframework.context.ApplicationEvent;


/**
 * 領域事件基類。
 * 所有業務領域事件都應繼承此類。
 * 繼承 Spring 的 ApplicationEvent，使其能被 ApplicationEventPublisher 發布。
 */
public abstract class DomainEvent  extends ApplicationEvent {
    // 事件發生時間戳
    private final long timestamp;

    /**
     * 構造函數。
     * @param source 事件源，通常是觸發事件的對象。
     */
    public DomainEvent(Object source) {
        super(source);
        this.timestamp = System.currentTimeMillis();
    }

    // 可以添加通用屬性，例如事件ID、操作用戶ID等
}
