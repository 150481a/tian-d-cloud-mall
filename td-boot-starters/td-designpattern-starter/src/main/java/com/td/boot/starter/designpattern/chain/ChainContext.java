package com.td.boot.starter.designpattern.chain;


import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 责任链上下文。
 * 负责管理和提供应用中所有的处理器实现。
 * 它会扫描所有 IHandler 实现，并支持构建处理器链。
 */
@Component
public class ChainContext implements ApplicationContextAware {

    // 存储所有处理器实例的映射，key 为处理器ID，value 为处理器 Bean 实例
    private final Map<String, IHandler<?, ?>> handlerMap = new ConcurrentHashMap<>();

    /**
     * 根据处理器ID获取对应的处理器实例。
     *
     * @param handlerId 处理器ID
     * @param <T>       请求参数类型
     * @param <R>       处理结果类型
     * @return 对应的处理器实例
     * @throws IllegalArgumentException 如果找不到对应的处理器
     */
    @SuppressWarnings("unchecked")
    public <T, R> IHandler<T, R> getHandler(String handlerId) {
        Assert.hasText(handlerId, "处理器ID不能为null或空");
        IHandler<?, ?> handler = handlerMap.get(handlerId);
        if (handler == null) {
            throw new IllegalArgumentException("未找到ID为 [" + handlerId + "] 的处理器。");
        }
        return (IHandler<T, R>) handler;
    }

    /**
     * 构建一个指定处理器ID列表的责任链，并返回链的起始处理器。
     * 链的顺序严格按照传入的 handlerIds 列表。
     *
     * @param handlerIds 处理器ID的有序列表
     * @param <T>        请求参数类型
     * @param <R>        处理结果类型
     * @return 链的起始处理器
     * @throws IllegalArgumentException 如果列表中有ID不存在或构建失败
     */
    @SuppressWarnings("unchecked")
    public <T, R> IHandler<T, R> buildChain(List<String> handlerIds) {
        Assert.notEmpty(handlerIds, "处理器ID列表不能为空");

        IHandler<T, R> head = null;
        IHandler<T, R> current = null;

        for (String handlerId : handlerIds) {
            IHandler<T, R> handler = getHandler(handlerId);
            if (head == null) {
                head = handler; // 链的第一个处理器
                current = head;
            } else {
                current.setNext(handler); // 设置当前处理器的下一个
                current = handler; // 移动到下一个处理器
            }
        }
        return head;
    }

    /**
     * 根据所有 IHandler Bean 的排序（如果实现了 Ordered 接口）或者默认顺序，
     * 构建一条完整的责任链。
     *
     * @param <T> 请求参数类型
     * @param <R> 处理结果类型
     * @return 链的起始处理器
     * @throws IllegalStateException 如果没有找到任何处理器
     */
    @SuppressWarnings("unchecked")
    public <T, R> IHandler<T, R> buildFullChain() {
        if (handlerMap.isEmpty()) {
            throw new IllegalStateException("未找到任何处理器来构建责任链。");
        }

        // 获取所有处理器，并按 Spring 的 Order 进行排序
        List<IHandler<T, R>> sortedHandlers = handlerMap.values().stream()
                .filter(handler -> handler instanceof IHandler) // 确保类型正确
                .map(handler -> (IHandler<T, R>) handler)
                .sorted(Comparator.comparing(h -> {
                    // 如果处理器实现了 Ordered 接口，则按 Order 排序
                    if (h instanceof org.springframework.core.Ordered) {
                        return ((org.springframework.core.Ordered) h).getOrder();
                    }
                    return Integer.MAX_VALUE; // 未实现 Ordered 的排在后面
                }))
                .collect(Collectors.toList());

        return buildChain(sortedHandlers.stream().map(IHandler::getHandlerId).collect(Collectors.toList()));
    }


    /**
     * 在 Spring 應用上下文加載完成後，掃描所有 IHandler 實現並註冊。
     *
     * @param applicationContext Spring 應用上下文
     * @throws BeansException 如果在查找 Bean 時發生錯誤
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // 获取所有 IHandler 类型的 Bean
        Map<String, IHandler> handlers = applicationContext.getBeansOfType(IHandler.class);
        handlers.values().forEach(handler -> {
            String handlerId = handler.getHandlerId();
            if (handlerId != null && !handlerId.isEmpty()) {
                if (handlerMap.containsKey(handlerId)) {
                    throw new IllegalStateException("检测到重复的处理器ID [" + handlerId + "]，请检查配置。");
                }
                handlerMap.put(handlerId, handler);
            } else {
                System.out.println("警告：处理器 " + handler.getClass().getName() + " 未指定 getHandlerId()，将不会被注册到 ChainContext。");
            }
        });
        System.out.println("ChainContext 成功注册了 " + handlerMap.size() + " 个处理器。");
    }

}
