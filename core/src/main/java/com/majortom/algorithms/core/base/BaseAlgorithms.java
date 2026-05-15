package com.majortom.algorithms.core.base;

import com.majortom.algorithms.core.runtime.ExecutionContext;
import com.majortom.algorithms.core.runtime.ExecutionException;

/**
 * 算法逻辑抽象基类。
 *
 * <p>具体排序、树、图、迷宫算法都继承它。算法只需要在关键步骤调用
 * {@link #sync(BaseStructure, Object, Object)}，基类会把当前结构推给
 * {@link ExecutionContext}，再由执行层生成快照并联动可视化层。</p>
 *
 * @param <S> 算法操作的数据结构类型
 */
public abstract class BaseAlgorithms<S extends BaseStructure<?>> {

    /**
     * 当前算法运行上下文。
     */
    protected ExecutionContext<S> executionContext;

    /**
     * 设置算法运行环境。
     *
     * @param executionContext 执行上下文，通常由控制器在启动算法前注入
     */
    public void setExecutionContext(ExecutionContext<S> executionContext) {
        this.executionContext = executionContext;
    }

    /**
     * 同步一个默认需要等待渲染的算法步骤。
     *
     * @param structure 数据结构实体
     * @param a 主焦点 (如当前访问的节点、正在比较的索引)
     * @param b 副焦点 (如目标节点、辅助指针)
     */
    protected void sync(S structure, Object a, Object b) {
        sync(structure, a, b, null, true);
    }

    /**
     * 同步一个算法步骤到执行层和可视化层。
     *
     * <p>这个方法同时充当“熔断器”：同步前后都会检查线程中断和执行取消，
     * 让停止按钮能尽快中断长循环。</p>
     *
     * @param structure 数据结构实体
     * @param a 主焦点
     * @param b 副焦点
     * @param label 当前步骤说明
     * @param awaitStep 是否等待 UI 渲染和播放延迟
     * @throws AlgorithmInterruptedException 当线程被中断或执行上下文取消时抛出
     */
    protected void sync(S structure, Object a, Object b, String label, boolean awaitStep) {
        requireActive();
        if (executionContext != null) {
            executionContext.sync(structure, a, b, label, awaitStep);
        }
        requireActive();
    }

    /**
     * 检查当前算法是否仍允许继续执行。
     *
     * @throws AlgorithmInterruptedException 当线程被中断或执行上下文取消时抛出
     */
    protected void requireActive() {
        if (Thread.currentThread().isInterrupted()) {
            throw new AlgorithmInterruptedException("Algorithm execution interrupted.");
        }
        if (executionContext != null) {
            try {
                executionContext.requireActive();
            } catch (ExecutionException e) {
                throw new AlgorithmInterruptedException(e.getMessage());
            }
        }
    }

    /**
     * 算法执行入口。
     *
     * @param structure 包含具体数据的结构实体
     */
    public abstract void run(S structure);

    /**
     * 算法中断异常。
     *
     * <p>它用于从深层算法循环快速退出。控制器或算法基类可以捕获它并静默结束，
     * 普通业务逻辑不建议依赖它做分支。</p>
     */
    public static class AlgorithmInterruptedException extends RuntimeException {
        /**
         * 创建算法中断异常。
         *
         * @param message 中断原因
         */
        public AlgorithmInterruptedException(String message) {
            super(message);
        }
    }
}
