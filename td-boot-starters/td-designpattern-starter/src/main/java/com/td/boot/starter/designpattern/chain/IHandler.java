package com.td.boot.starter.designpattern.chain;

/**
 * 责任链模式中的处理器接口。
 * 定义了处理请求的方法和设置下一个处理器的方法。
 *
 * @param <T> 请求参数类型
 * @param <R> 处理结果类型
 */
public interface IHandler<T, R> {

    /**
     * 处理请求的方法。
     * 如果当前处理器能处理，则处理并返回结果。
     * 如果不能处理或需要继续传递，则调用下一个处理器。
     *
     * @param request 请求参数
     * @return 处理结果
     */
    R handle(T request);

    /**
     * 设置链中的下一个处理器。
     *
     * @param nextHandler 下一个处理器实例
     */
    void setNext(IHandler<T, R> nextHandler);

    /**
     * 获取当前处理器的唯一标识符。
     * 用于在 ChainContext 中按 ID 查找或排序。
     *
     * @return 处理器ID
     */
    String getHandlerId();
}
