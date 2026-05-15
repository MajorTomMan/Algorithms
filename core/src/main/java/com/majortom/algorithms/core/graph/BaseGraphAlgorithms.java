package com.majortom.algorithms.core.graph;

import com.majortom.algorithms.core.base.BaseAlgorithms;

/**
 * 图算法逻辑基类。
 *
 * <p>具体图算法通过 {@link BaseGraph} 操作 GraphStream 图，同时继承
 * {@link BaseAlgorithms} 的执行同步能力。算法在访问节点或确认边后调用 sync，
 * 可视化层就能收到包含节点/边高亮状态的图快照。</p>
 *
 * @param <V> 业务数据类型，例如存储在节点属性中的自定义对象
 */
public abstract class BaseGraphAlgorithms<V> extends BaseAlgorithms<BaseGraph<V>> {

    /**
     * 从指定节点开始执行图算法。
     *
     * @param graphData 业务图包装器，内部持有 GraphStream 图实例
     * @param startNodeId 起始节点 ID
     */
    public abstract void run(BaseGraph<V> graphData, String startNodeId);

}
