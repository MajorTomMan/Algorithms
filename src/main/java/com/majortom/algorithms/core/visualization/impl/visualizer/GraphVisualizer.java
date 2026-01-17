package com.majortom.algorithms.core.visualization.impl.visualizer;

import com.majortom.algorithms.core.graph.BaseGraph;
import com.majortom.algorithms.core.visualization.BaseVisualizer;
import javafx.application.Platform;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.ui.fx_viewer.FxViewPanel;
import org.graphstream.ui.fx_viewer.FxViewer;

/**
 * 图算法可视化器 - 适配《乱》色彩体系
 * 映射规则：
 * a: 当前处理节点 -> highlight (次郎蓝)
 * b: 已探索/邻居节点 -> secondary (三郎黄)
 * 默认节点/边 -> 红色 (太郎红)
 */
public class GraphVisualizer<V> extends BaseVisualizer<BaseGraph<V>> {

    private FxViewer viewer;
    private FxViewPanel viewPanel;
    private final Graph gsGraph;

    public GraphVisualizer(BaseGraph<V> baseGraph) {
        System.setProperty("org.graphstream.ui", "javafx");
        this.gsGraph = baseGraph.getGraph();
        Platform.runLater(this::initializeViewer);
    }

    @Override
    protected void draw(BaseGraph<V> data, Object a, Object b) {
        if (data == null || gsGraph == null) return;

        try {
            // 1. 处理当前核心节点 (Active Node) -> 次郎蓝
            if (a instanceof String nodeId) {
                Node nodeA = gsGraph.getNode(nodeId);
                if (nodeA != null) {
                    nodeA.setAttribute("ui.class", "highlight");
                }
            }

            // 2. 处理关联节点 (Neighbor Node) -> 三郎黄
            if (b instanceof String nodeId) {
                Node nodeB = gsGraph.getNode(nodeId);
                if (nodeB != null) {
                    nodeB.setAttribute("ui.class", "secondary");
                }
            }
        } catch (Exception e) {
            // 处理并发状态下的微小异常
        }
    }

    private void initializeViewer() {
        if (gsGraph == null) return;

        // 加载符合《乱》美学的样式表
        try {
            String stylesheet = getClass().getResource("/style/graph.css").toExternalForm();
            gsGraph.setAttribute("ui.stylesheet", "url('" + stylesheet + "')");
        } catch (Exception e) {
            System.err.println("[Error] 样式文件加载失败");
        }

        gsGraph.setAttribute("ui.antialias");
        this.viewer = new FxViewer(gsGraph, FxViewer.ThreadingModel.GRAPH_IN_GUI_THREAD);
        this.viewer.enableAutoLayout(); 
        this.viewPanel = (FxViewPanel) viewer.addDefaultView(false);

        this.getChildren().clear();
        this.getChildren().addAll(viewPanel, canvas);
        canvas.setMouseTransparent(true);

        viewPanel.prefWidthProperty().bind(this.widthProperty());
        viewPanel.prefHeightProperty().bind(this.heightProperty());
    }

    @Override
    public void clear() {
        if (gsGraph != null) {
            gsGraph.nodes().forEach(n -> n.removeAttribute("ui.class"));
            gsGraph.edges().forEach(e -> e.removeAttribute("ui.class"));
        }
    }
}