package com.majortom.algorithms.core.graph;

import org.graphstream.graph.Graph;

import com.majortom.algorithms.core.base.BaseAlgorithms;

/**
 * 图算法逻辑基类
 * 职责：
 * 1. 接收 BaseGraph 包装器作为操作目标。
 * 2. 统筹算法执行过程中的统计数据记录与 UI 同步。
 * 3. 屏蔽底层 GraphStream 的复杂性，提供统一的算法入口。
 * * @param <V> 业务数据的类型（如存储在 Node 属性中的自定义对象）
 */
public abstract class BaseGraphAlgorithms<V> extends BaseAlgorithms<BaseGraph<V>> {

    /**
     * 执行算法的核心入口
     * * @param graphData 业务图包装器，内部持有具体的 Graph 实例。
     * 
     * @param startNodeId 起始节点的唯一标识符（ID）。
     */
    public abstract void run(BaseGraph<V> graphData, String startNodeId);

}