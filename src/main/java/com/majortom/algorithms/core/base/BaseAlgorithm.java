package com.majortom.algorithms.core.base;

import com.majortom.algorithms.core.base.listener.SyncListener;

/**
 * 算法逻辑抽象基类（Base Algorithm Abstraction）
 * * 职责：
 * 1. 状态度量：统一管理算法运行时的关键性能指标（比较次数与操作次数）。
 * 2. 消息分发：提供同步机制，将算法内部逻辑状态实时推送至监听器（通常是 BaseFrame）。
 * 3. 泛型支持：支持任意数据结构（如数组、树、图或矩阵）的可视化同步。
 * * @param <T> 数据模型类型
 */
public abstract class BaseAlgorithm<T> {
    /** 同步监听器：负责接收算法产生的“快照”信号 */
    protected SyncListener<T> listener;

    /** 比较计数器：记录算法执行过程中元素间比对的次数 */
    protected int compareCount = 0;

    /** 操作计数器：记录算法执行过程中修改数据的次数（如交换、赋值、变色等） */
    protected int actionCount = 0;

    /**
     * 设置同步监听器
     * 
     * @param listener 实现 SyncListener 接口的窗体或控制器
     */
    public void setListener(SyncListener<T> listener) {
        this.listener = listener;
    }

    /**
     * 同步当前算法状态到界面（核心同步钩子）
     * 算法每完成一次关键步骤（如一次交换或探测），应主动调用此方法。
     * * @param data 当前时刻的数据快照
     * 
     * @param a 需要高亮显示的主焦点（Object 类型以适配索引或节点引用）
     * @param b 需要高亮显示的副焦点
     */
    protected void sync(T data, Object a, Object b) {
        if (listener != null) {
            // 将当前数据、焦点以及累计的统计量封装并分发
            listener.onSync(data, a, b, compareCount, actionCount);
        }
    }

    /**
     * 重置算法统计量
     * 用于在重新运行算法前，将比较次数和操作次数归零。
     */
    public void resetStatistics() {
        this.compareCount = 0;
        this.actionCount = 0;
    }

    /** 获取当前累计的比较次数 */
    public int getCompareCount() {
        return compareCount;
    }

    /** 获取当前累计的操作次数 */
    public int getActionCount() {
        return actionCount;
    }
}