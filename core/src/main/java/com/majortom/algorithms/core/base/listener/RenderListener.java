package com.majortom.algorithms.core.base.listener;

/**
 * 渲染完成监听器。
 *
 * <p>这是早期执行同步模型中用于通知“当前帧已经画完”的轻量回调接口。
 * 当前主链路更多使用 ExecutionControl 和 AlgorithmThreadManager，但这个接口仍可用于简单渲染器。</p>
 *
 * @param <T> 监听器关联的数据类型
 */
@FunctionalInterface
public interface RenderListener<T> {
    /**
     * 当前渲染任务完成时调用。
     */
    void onRenderComplete();
}
