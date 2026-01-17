package com.majortom.algorithms.core.base;

import com.majortom.algorithms.core.base.listener.SyncListener;
import com.majortom.algorithms.visualization.manager.AlgorithmThreadManager;
import com.majortom.algorithms.core.base.listener.StepListener;

/**
 * 算法逻辑抽象基类
 * 职责：控制执行节奏，处理算法中断，并将算法状态同步至可视化层。
 * * @param <S> 必须是 BaseStructure 的子类，确保具备统计能力
 */
public abstract class BaseAlgorithms<S extends BaseStructure<?>> {

    /** 同步监听器：负责接收算法产生的“快照”信号 */
    protected SyncListener<S> syncListener;

    /** 步进监听器：负责控制算法的执行节奏（阻塞/节流/延迟） */
    protected StepListener stepListener;

    /**
     * 设置算法运行环境
     */
    public void setEnvironment(SyncListener<S> syncListener, StepListener stepListener) {
        this.syncListener = syncListener;
        this.stepListener = stepListener;
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
        // 1. 熔断检查：如果管理线程调用了 cancel(true)，此处捕获中断状态
        if (Thread.currentThread().isInterrupted()) {
            // 清除中断位并抛出自定义异常，实现算法逻辑的“即刻跳出”
            System.out.println("Algorithm execution safely aborted.");
        }

        // 2. 发送同步信号，携带最新的 compareCount 和 actionCount
        if (syncListener != null) {
            syncListener.onSync(
                    structure,
                    a,
                    b,
                    structure.getCompareCount(),
                    structure.getActionCount());
        }

        // 3. 节奏控制：执行步进延迟（通过 Thread.sleep 或自定义 Lock 阻塞）
        if (stepListener != null) {
            stepListener.onStep();
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