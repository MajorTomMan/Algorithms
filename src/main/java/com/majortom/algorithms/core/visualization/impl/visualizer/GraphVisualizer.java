package com.majortom.algorithms.core.visualization.impl.visualizer;

import com.majortom.algorithms.core.graph.BaseGraph;
import com.majortom.algorithms.core.visualization.base.BaseGraphVisualizer;
import org.graphstream.graph.Node;

/**
 * 图算法可视化具体实现
 * 职责：执行具体的节点样式映射，确保算法焦点在《乱》配色体系下清晰移动。
 */
public class GraphVisualizer<V> extends BaseGraphVisualizer<V> {
    // 显式定义构造函数，确保泛型 V 能够被锁定
    public GraphVisualizer(BaseGraph<V> initialData) {
        super(); // 调用 BaseVisualizer 的构造函数
        // 初始渲染逻辑
        if (initialData != null) {
            render(initialData, null, null);
        }
    }

    /**
     * 实现具体的样式更新逻辑
     * 策略：在每一帧渲染前，先清理旧的焦点状态，再标记新的焦点。
     */
    @Override
    protected void updateGraphStyles(BaseGraph<V> data, Object a, Object b) {
        if (gsGraph == null)
            return;

        try {
            // 1. 状态重置：移除全图的焦点样式（保持太郎红背景）
            // 这样可以确保每次 render 只有当前的 a 和 b 会高亮
            gsGraph.nodes().forEach(n -> {
                if (n.hasAttribute("ui.class")) {
                    Object clazz = n.getAttribute("ui.class");
                    if ("highlight".equals(clazz) || "secondary".equals(clazz)) {
                        n.removeAttribute("ui.class");
                    }
                }
            });

            // 2. 映射当前焦点 (Active Node) -> 次郎蓝
            if (a instanceof String nodeId) {
                Node nodeA = gsGraph.getNode(nodeId);
                if (nodeA != null) {
                    nodeA.setAttribute("ui.class", "highlight");
                }
            }

            // 3. 映射辅助焦点 (Neighbor/Target) -> 三郎黄
            if (b instanceof String nodeId) {
                Node nodeB = gsGraph.getNode(nodeId);
                if (nodeB != null) {
                    nodeB.setAttribute("ui.class", "secondary");
                }
            }
        } catch (Exception e) {
            // 忽略图引擎在并发更新时的瞬时异常
        }
    }
}