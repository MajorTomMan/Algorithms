package com.majortom.algorithms.core.runtime;

import com.majortom.algorithms.core.base.BaseStructure;

/**
 * 执行控制接口。
 *
 * <p>core 层算法只负责产生 {@link ExecutionFrame}，不关心这些帧如何被播放。
 * visualization 层会实现或适配这个接口，把帧交给 JavaFX 渲染线程，并提供取消状态和播放速度。</p>
 *
 * @param <S> 被算法操作并被可视化渲染的数据结构类型
 */
public interface ExecutionControl<S extends BaseStructure<?>> {
    /**
     * 接收算法产生的一帧执行快照。
     *
     * @param frame 当前执行帧
     * @param awaitStep 是否等待渲染、暂停和延迟逻辑完成后再让算法继续
     */
    void acceptFrame(ExecutionFrame<S> frame, boolean awaitStep);

    /**
     * 判断调用方是否请求取消本次执行。
     *
     * @return 如果算法应尽快停止，返回 true
     */
    boolean isCancelled();

    /**
     * 获取当前帧间延迟。
     *
     * @return 延迟毫秒数
     */
    long getDelayMillis();
}
