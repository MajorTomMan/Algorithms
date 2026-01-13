package com.majortom.algorithms.core.base;

import com.majortom.algorithms.core.base.listener.SyncListener;
import com.majortom.algorithms.core.base.listener.StepListener;

/**
 * 算法逻辑抽象基类 (Refactored)
 * 职责：控制执行节奏，将算法状态同步至可视化层。
 * 变化：统计数据（compare/action）已移至 BaseStructure，此处仅负责传递。
 * * @param <S> 必须是 BaseStructure 的子类，确保具备统计能力
 */
public abstract class BaseAlgorithms<S extends BaseStructure<?>> {

    /** 同步监听器：负责接收算法产生的“快照”信号 */
    protected SyncListener<S> syncListener;

    /** 步进监听器：负责控制算法的执行节奏（阻塞/节流） */
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
     * 自动从 Structure 中提取最新的统计数据并通知 UI，随后进入节流等待。
     * * @param structure 数据结构实体
     * 
     * @param a 主焦点 (如当前节点、正在比较的索引)
     * @param b 副焦点 (如目标节点、被比较的索引)
     */
    protected void sync(S structure, Object a, Object b) {
        // 1. 发送同步信号，自动携带 Structure 内部维护的计数
        if (syncListener != null) {
            syncListener.onSync(
                    structure,
                    a,
                    b,
                    structure.getCompareCount(),
                    structure.getActionCount());
        }

        // 2. 节奏控制：阻塞当前线程直到渲染完成或延迟结束
        if (stepListener != null) {
            stepListener.onStep();
        }
    }

    /**
     * 算法执行入口
     * 
     * @param structure 包含具体数据（Graph/Array/Tree）的实体
     */
    public abstract void run(S structure);
}