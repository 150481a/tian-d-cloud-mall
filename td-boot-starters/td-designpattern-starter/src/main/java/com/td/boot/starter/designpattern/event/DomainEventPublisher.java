package com.td.boot.starter.designpattern.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalApplicationListener;

/**
 * 領域事件發布器。
 * 封裝了 Spring 的 ApplicationEventPublisher，用於發布領域事件。
 */
@Component
public class DomainEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public DomainEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * 發布一個領域事件。
     * 事件會被所有相關的 ApplicationListener 或 @EventListener 監聽器處理。
     *
     * @param event 要發布的領域事件
     */
    public void publish(DomainEvent event) {
        eventPublisher.publishEvent(event);
        System.out.println("發布領域事件：" + event.getClass().getSimpleName() + " @ " + event.getTimestamp());
    }

    /**
     * 在事務提交後發布事件。
     * 適用於需要確保數據持久化後才觸發的事件。
     *
     * @param event 要發布的領域事件
     */
    public void publishAfterCommit(DomainEvent event) {
        eventPublisher.publishEvent(new TransactionalApplicationListener.forAfterCommit(event));
        System.out.println("發布事務提交後領域事件：" + event.getClass().getSimpleName() + " @ " + event.getTimestamp());
    }

    /**
     * 在事務完成後（無論是提交還是回滾）發布事件。
     *
     * @param event 要發布的領域事件
     */
    public void publishAfterCompletion(DomainEvent event) {
        eventPublisher.publishEvent(new TransactionalApplicationListener.forAfterCompletion(event));
        System.out.println("發布事務完成後領域事件：" + event.getClass().getSimpleName() + " @ " + event.getTimestamp());
    }
}
