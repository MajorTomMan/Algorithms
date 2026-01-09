package com.majortom.algorithms.core.graph;

import com.majortom.algorithms.core.base.BaseAlgorithm;

import com.majortom.algorithms.core.graph.node.Vertex;

/**
 * 图算法逻辑基类
 * 职责：
 * 1. 接收一个 BaseGraph 作为操作目标
 * 2. 统筹执行过程中的统计数据同步
 */
public abstract class BaseGraphAlgorithms<V> extends BaseAlgorithm<BaseGraph<V>> {

    /**
     * 执行算法的核心入口
     * 
     * @param graph 目标图结构（有向或无向）
     * @param start 起始顶点
     */
    public abstract void run(BaseGraph<V> graph, Vertex<V> start);


}
