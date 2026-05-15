package com.majortom.algorithms.visualization.impl.visualizer;

import com.majortom.algorithms.core.graph.BaseGraph;
import com.majortom.algorithms.visualization.VisualizationEvent;
import com.majortom.algorithms.visualization.base.BaseGraphVisualizer;

import org.graphstream.graph.Node;

/**
 * 具体的图算法可视化器
 * 风格：基于《乱》色彩体系进行节点着色
 */
public class GraphVisualizer<V> extends BaseGraphVisualizer<V> {

    private long accentUntilMillis;

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

        drawAccentFrame();
        drawTransientFeedbackOverlay();
    }

    @Override
    public void onControlAction(VisualizationEvent event) {
        super.onControlAction(event);
        accentUntilMillis = System.currentTimeMillis() + FEEDBACK_DURATION_MS;
    }

    @Override
    public void onVisualizationReset() {
        accentUntilMillis = 0L;
        super.onVisualizationReset();
    }

    @Override
    public void onModuleDetached(String moduleId) {
        accentUntilMillis = 0L;
        super.onModuleDetached(moduleId);
    }

    private void drawAccentFrame() {
        if (System.currentTimeMillis() >= accentUntilMillis) {
            return;
        }
        gc.save();
        gc.setGlobalAlpha(Math.max(0.0, Math.min(1.0, (accentUntilMillis - System.currentTimeMillis()) / (double) FEEDBACK_DURATION_MS)));
        gc.setStroke(RAN_CYAN);
        gc.setLineWidth(2.5);
        gc.strokeRoundRect(12, 12, Math.max(0, canvas.getWidth() - 24), Math.max(0, canvas.getHeight() - 24), 18, 18);
        gc.restore();
    }
}
