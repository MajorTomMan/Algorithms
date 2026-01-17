package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.core.graph.BaseGraph;
import com.majortom.algorithms.visualization.base.BaseGraphVisualizer;

import org.graphstream.graph.Node;

/**
 * 具体的图算法可视化器
 * 风格：基于《乱》色彩体系进行节点着色
 */
public class GraphVisualizer<V> extends BaseGraphVisualizer<V> {

    public GraphVisualizer(BaseGraph<V> baseGraph) {
        super(baseGraph);
    }

    @Override
    protected String getStyleSheetPath() {
        return "/style/graph.css";
    }

    @Override
    protected void draw(BaseGraph<V> data, Object a, Object b) {
        if (data == null || gsGraph == null)
            return;

        try {
            // 逻辑 A: 处理当前核心节点 -> highlight (次郎蓝)
            if (a instanceof String nodeId) {
                Node nodeA = gsGraph.getNode(nodeId);
                if (nodeA != null)
                    nodeA.setAttribute("ui.class", "highlight");
            }

            // 逻辑 B: 处理关联节点 -> secondary (三郎黄)
            if (b instanceof String nodeId) {
                Node nodeB = gsGraph.getNode(nodeId);
                if (nodeB != null)
                    nodeB.setAttribute("ui.class", "secondary");
            }
        } catch (Exception e) {
            // 针对 GraphStream 并发修改的静默处理
        }
    }
}