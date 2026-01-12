package com.majortom.algorithms.core.base;

import com.majortom.algorithms.core.base.listener.SyncListener;
import com.majortom.algorithms.core.base.listener.StepListener;

/**
 * 算法逻辑抽象基类（Base Algorithms Abstraction）
 * * 适配说明：
 * 1. 增加了 StepListener 成员，用于实现算法线程的步进节流。
 * 2. 强化了 sync 方法，使其在同步数据后能自动阻塞，等待 UI 渲染完成。
 * * @param <T> 数据模型类型
 */
public abstract class BaseAlgorithms<T> {

    /** 同步监听器：负责接收算法产生的“快照”信号（发信号） */
    protected SyncListener<T> syncListener;

    /** 步进监听器：负责控制算法的执行节奏（等回执/节流） */
    protected StepListener stepListener;

    /** 比较计数器 */
    protected int compareCount = 0;

    /** 操作计数器 */
    protected int actionCount = 0;

    /**
     * 设置算法运行环境
     * * @param syncListener 负责数据同步
     * 
     * @param stepListener 负责节奏控制（阻塞/暂停/延迟）
     */
    public void setEnvironment(SyncListener<T> syncListener, StepListener stepListener) {
        this.syncListener = syncListener;
        this.stepListener = stepListener;
    }

    /**
     * 核心同步钩子：同步当前算法状态并进入节流等待
     * * @param data 当前时刻的数据快照
     * 
     * @param a 需要高亮显示的主焦点
     * @param b 需要高亮显示的副焦点
     */
    protected void sync(T data, Object a, Object b) {
        // 1. 通知 UI 渲染
        if (syncListener != null) {
            syncListener.onSync(data, a, b, compareCount, actionCount);
        }

        // 2. 阻塞当前算法线程，等待 UI 渲染完成信号或延迟设置
        if (stepListener != null) {
            stepListener.onStep();
        }
    }

    /**
     * 重置算法统计量
     */
    public void resetStatistics() {
        this.compareCount = 0;
        this.actionCount = 0;
    }

    // --- Getters ---
    public int getCompareCount() {
        return compareCount;
    }

    public int getActionCount() {
        return actionCount;
    }

    /**
     * 强制统一入口
     * 
     * @param data 算法操作的数据核心 (如 int[], BaseTree, Graph)
     */
    public abstract void run(T data);
}