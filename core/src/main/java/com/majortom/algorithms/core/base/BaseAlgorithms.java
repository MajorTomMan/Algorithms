package com.majortom.algorithms.core.base;

import com.majortom.algorithms.core.runtime.ExecutionContext;
import com.majortom.algorithms.core.runtime.ExecutionException;

/**
 * 算法逻辑抽象基类
 * 职责：控制执行节奏，处理算法中断，并将算法状态同步至可视化层。
 * * @param <S> 必须是 BaseStructure 的子类，确保具备统计能力
 */
public abstract class BaseAlgorithms<S extends BaseStructure<?>> {

    protected ExecutionContext<S> executionContext;

    /**
     * 设置算法运行环境
     */
    public void setExecutionContext(ExecutionContext<S> executionContext) {
        this.executionContext = executionContext;
    }

    /**
     * 核心同步钩子
     * 在算法执行的每一个关键步点调用。它不仅同步 UI，还充当“熔断器”。
     * * @param structure 数据结构实体
     * 
     * @param a 主焦点 (如当前访问的节点、正在比较的索引)
     * @param b 副焦点 (如目标节点、辅助指针)
     * @throws AlgorithmInterruptedException 当线程收到中断信号时抛出，用于强制终止算法
     */
    protected void sync(S structure, Object a, Object b) {
        sync(structure, a, b, null, true);
    }

    protected void sync(S structure, Object a, Object b, String label, boolean awaitStep) {
        requireActive();
        if (executionContext != null) {
            executionContext.sync(structure, a, b, label, awaitStep);
        }
        requireActive();
    }

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
     * 算法执行入口
     * 由具体算法类（如 BFSMazeGenerator）实现
     * * @param structure 包含具体数据（Graph/Array/Tree）的实体
     */
    public abstract void run(S structure);

    /**
     * 自定义运行时异常
     * 专门用于静默终止算法线程，不建议在业务逻辑中捕获它。
     */
    public static class AlgorithmInterruptedException extends RuntimeException {
        public AlgorithmInterruptedException(String message) {
            super(message);
        }
    }
}
