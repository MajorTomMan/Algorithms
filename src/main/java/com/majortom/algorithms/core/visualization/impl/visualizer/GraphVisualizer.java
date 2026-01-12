package com.majortom.algorithms.core.visualization.impl.visualizer;

import com.majortom.algorithms.core.visualization.BaseVisualizer;
import javafx.application.Platform;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.ui.fx_viewer.FxViewPanel;
import org.graphstream.ui.fx_viewer.FxViewer;

/**
 * 图算法可视化器
 * 职责：适配 GraphStream 的 JavaFX 视图，通过 CSS 样式表实现霓虹视觉效果，
 * 并根据算法反馈更新节点状态。
 */
public class GraphVisualizer extends BaseVisualizer<Graph> {

    private FxViewer viewer;
    private FxViewPanel viewPanel;
    private Graph internalGraph;

    public GraphVisualizer() {
        // 1. 指定 GraphStream 使用 JavaFX 渲染引擎
        System.setProperty("org.graphstream.ui", "javafx");

        // 初始状态下不持有特定的 Graph 引用，由第一次 draw 传入或由 Controller 注入
        // 此时仅作为容器准备
    }

    /**
     * 实现渲染钩子
     * 
     * @param data 传入的 GraphStream 图实例
     * @param a    当前操作的主节点 ID (String)
     * @param b    关联操作的辅助节点 ID (String)
     */
    @Override
    protected void draw(Graph data, Object a, Object b) {
        if (data == null)
            return;

        // 首次运行或图实例切换时初始化 Viewer
        if (internalGraph != data) {
            this.internalGraph = data;
            initializeViewer();
        }

        // 修改节点样式属于 UI 更新范畴
        Platform.runLater(() -> {
            // 清除旧的高亮状态
            data.nodes().forEach(n -> n.removeAttribute("ui.class"));

            // 设置主焦点样式 (如: START_VIOLET 效果)
            if (a instanceof String nodeId) {
                Node nodeA = data.getNode(nodeId);
                if (nodeA != null)
                    nodeA.setAttribute("ui.class", "highlight");
            }

            // 设置副焦点样式 (如: NEON_BLUE 效果)
            if (b instanceof String nodeId) {
                Node nodeB = data.getNode(nodeId);
                if (nodeB != null)
                    nodeB.setAttribute("ui.class", "secondary");
            }
        });
    }

    private void initializeViewer() {
        Platform.runLater(() -> {
            // 应用霓虹样式表
            internalGraph.setAttribute("ui.stylesheet", getNeonStyleSheet());
            internalGraph.setAttribute("ui.antialias");

            this.viewer = new FxViewer(internalGraph, FxViewer.ThreadingModel.GRAPH_IN_GUI_THREAD);
            this.viewer.enableAutoLayout();

            // 获取 FxViewPanel 并挂载到 StackPane
            this.viewPanel = (FxViewPanel) viewer.addDefaultView(false);

            // 清除 BaseVisualizer 默认生成的画布
            this.getChildren().clear();
            this.getChildren().add(viewPanel);
        });
    }

    /**
     * 对齐全局视觉风格的样式定义
     */
    private String getNeonStyleSheet() {
        return "graph { fill-color: #0A0A0E; padding: 60px; }" +
                "node { " +
                "   size: 26px; " +
                "   fill-color: #CFD8DC; " + // CRYSTAL_WHITE 基调
                "   text-size: 14px; " +
                "   text-color: #0A0A0E; " +
                "   stroke-mode: plain; " +
                "   stroke-color: #455A64; " +
                "   stroke-width: 1px; " +
                "}" +
                "node.highlight { " +
                "   fill-color: #7E57C2; " + // START_VIOLET
                "   stroke-color: #FFFFFF; " +
                "   stroke-width: 2px; " +
                "   shadow-mode: gradient-radial; " +
                "   shadow-color: rgba(126, 87, 194, 0.5), rgba(10, 10, 14, 0); " +
                "   shadow-width: 15px; shadow-offset: 0px; " +
                "}" +
                "node.secondary { " +
                "   fill-color: #00A0FF; " + // NEON_BLUE
                "}" +
                "edge { " +
                "   fill-color: #455A64; " +
                "   width: 2px; " +
                "   arrow-size: 10px, 4px; " +
                "}";
    }

    @Override
    public void clear() {
        super.clear();
        if (internalGraph != null) {
            internalGraph.nodes().forEach(n -> n.removeAttribute("ui.class"));
        }
    }
}