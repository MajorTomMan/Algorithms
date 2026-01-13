package com.majortom.algorithms.core.visualization.impl.visualizer;

import com.majortom.algorithms.core.graph.BaseGraph;
import com.majortom.algorithms.core.visualization.BaseVisualizer;
import javafx.application.Platform;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.ui.fx_viewer.FxViewPanel;
import org.graphstream.ui.fx_viewer.FxViewer;

/**
 * 图算法可视化器 (重构版)
 * 职责：适配 GraphStream 引擎，将 BaseGraph 的状态映射到视觉层面。
 * * @param <V> 节点存储的数据类型
 */
public class GraphVisualizer<V> extends BaseVisualizer<BaseGraph<V>> {

    private FxViewer viewer;
    private FxViewPanel viewPanel;
    private final Graph gsGraph; // 内部引用的 GraphStream 对象

    public GraphVisualizer(BaseGraph<V> baseGraph) {
        // 设置 GS 渲染引擎为 JavaFX
        System.setProperty("org.graphstream.ui", "javafx");
        this.gsGraph = baseGraph.getGraph();
        
        // 初始渲染：由于 Viewer 初始化较慢，放入 Platform.runLater
        Platform.runLater(this::initializeViewer);
    }

    /**
     * 实现渲染钩子
     * 由 BaseController 触发，运行在 JavaFX 线程。
     */
    @Override
    protected void draw(BaseGraph<V> data, Object a, Object b) {
        if (data == null || gsGraph == null) return;

        try {
            // 🚩 1. 清理上一帧的高亮状态
            gsGraph.nodes().forEach(n -> n.removeAttribute("ui.class"));
            gsGraph.edges().forEach(e -> e.removeAttribute("ui.class"));

            // 🚩 2. 处理当前焦点节点 A (通常是正在访问的节点)
            if (a instanceof String nodeId) {
                Node nodeA = gsGraph.getNode(nodeId);
                if (nodeA != null) nodeA.setAttribute("ui.class", "highlight");
            }

            // 🚩 3. 处理次要焦点 B (通常是路径或父节点)
            if (b instanceof String nodeId) {
                Node nodeB = gsGraph.getNode(nodeId);
                if (nodeB != null) nodeB.setAttribute("ui.class", "secondary");
            }

            // GraphStream 的 CSS 应用是异步的，此处微调 sleep 确保渲染完成
            // 这种写法在 AlgorithmThreadManager 的管控下是安全的
            Thread.sleep(1);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initializeViewer() {
        if (gsGraph == null) return;

        // 设置全局霓虹色风格
        gsGraph.setAttribute("ui.stylesheet", getNeonStyleSheet());
        gsGraph.setAttribute("ui.antialias");

        // 初始化 Viewer
        this.viewer = new FxViewer(gsGraph, FxViewer.ThreadingModel.GRAPH_IN_GUI_THREAD);
        this.viewer.enableAutoLayout();
        
        // 获取视图面板
        this.viewPanel = (FxViewPanel) viewer.addDefaultView(false);

        // 🚩 修正：直接加入到当前的 StackPane 中，利用 StackPane 的自动填充特性
        this.getChildren().setAll(viewPanel);
        
        // 让 viewPanel 的大小绑定到本组件
        viewPanel.prefWidthProperty().bind(this.widthProperty());
        viewPanel.prefHeightProperty().bind(this.heightProperty());
    }

    /**
     * 定义与你气质相符的“极夜霓虹”样式表
     */
    private String getNeonStyleSheet() {
        return "graph { fill-color: #0A0A0E; padding: 50px; }" +
               "node { " +
               "   size: 28px; " +
               "   fill-color: #CFD8DC; " + // 基础冷灰
               "   text-size: 15px; " +
               "   text-color: #CFD8DC; " +
               "   text-offset: 0, 30; " +
               "   stroke-mode: plain; " +
               "   stroke-color: #455A64; " +
               "   stroke-width: 1px; " +
               "}" +
               "node.highlight { " +
               "   fill-color: #7E57C2; " + // 忧郁紫
               "   stroke-color: #FFFFFF; " +
               "   stroke-width: 2px; " +
               "   size: 32px; " +
               "}" +
               "node.secondary { " +
               "   fill-color: #00A0FF; " + // 专注蓝
               "   size: 28px; " +
               "}" +
               "edge { " +
               "   fill-color: #455A64; " +
               "   size: 2px; " +
               "}";
    }

    @Override
    public void clear() {
        if (gsGraph != null) {
            gsGraph.nodes().forEach(n -> n.removeAttribute("ui.class"));
            gsGraph.edges().forEach(e -> e.removeAttribute("ui.class"));
        }
    }
}