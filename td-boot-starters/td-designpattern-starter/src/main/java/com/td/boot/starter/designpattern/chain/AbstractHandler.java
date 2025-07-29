package com.td.boot.starter.designpattern.chain;

/**
 * 责任链模式的抽象处理器基类。
 * 提供了设置下一个处理器和默认的链式处理逻辑。
 *
 * @param <T> 请求参数类型
 * @param <R> 处理结果类型
 */
public abstract class AbstractHandler<T, R> implements IHandler<T, R> {

    protected IHandler<T, R> nextHandler; // 指向链中的下一个处理器

    /**
     * 子类实现此方法来处理具体的业务逻辑。
     *
     * @param request 请求参数
     * @return 如果当前处理器能够处理，返回处理结果；否则返回 null，表示需要传递给下一个处理器。
     */
    protected abstract R doHandle(T request);

    /**
     * 获取当前处理器的唯一标识符。
     * 子类必须实现此方法。
     *
     * @return 处理器ID
     */
    @Override
    public abstract String getHandlerId();

    /**
     * 设置链中的下一个处理器。
     *
     * @param nextHandler 下一个处理器实例
     */
    @Override
    public void setNext(IHandler<T, R> nextHandler) {
        this.nextHandler = nextHandler;
    }

    /**
     * 核心處理方法。
     * 嘗試處理請求，如果不能處理或處理完畢需要繼續，則將請求傳遞給下一個處理器。
     *
     * @param request 请求参数
     * @return 处理结果
     */
    @Override
    public R handle(T request) {
        // 先尝试当前处理器处理
        R result = doHandle(request);

        // 如果当前处理器没有返回明确结果（例如返回null表示未处理），并且存在下一个处理器，则传递给下一个
        if (result == null && nextHandler != null) {
            System.out.println(getHandlerId() + " 未处理或已处理完毕，传递给下一个处理器: " + nextHandler.getHandlerId());
            return nextHandler.handle(request);
        } else if (result != null) {
            System.out.println(getHandlerId() + " 已成功处理请求并返回结果。");
            return result; // 当前处理器已经处理并返回了明确结果
        } else { // result == null && nextHandler == null
            System.out.println(getHandlerId() + " 未处理请求，且没有下一个处理器。");
            // 可以在这里抛出异常或返回默认结果，取决于业务需求
            return null; // 或者抛出 new UnsupportedOperationException("未能找到合适的处理器处理请求");
        }
    }
}
