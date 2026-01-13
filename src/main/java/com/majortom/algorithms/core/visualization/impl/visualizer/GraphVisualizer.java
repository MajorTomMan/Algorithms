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

    @Override
    protected void draw(BaseGraph<V> data, Object a, Object b) {
        if (data == null || gsGraph == null)
            return;

        // 🚩 关键：删掉全场清理代码
        // gsGraph.nodes().forEach(n -> n.removeAttribute("ui.class")); <- 删掉这行

        try {
            // 🚩 2. 处理当前正在处理的节点 A (保持紫色 highlight)
            if (a instanceof String nodeId) {
                Node nodeA = gsGraph.getNode(nodeId);
                if (nodeA != null) {
                    nodeA.setAttribute("ui.class", "highlight");
                }
            }

            // 🚩 3. 处理已探索过的路径或关联节点 B (专注蓝 secondary)
            if (b instanceof String nodeId) {
                Node nodeB = gsGraph.getNode(nodeId);
                if (nodeB != null) {
                    nodeB.setAttribute("ui.class", "secondary");
                }
            }

            // 注意：因为不再全场清空，变紫的点会一直保持紫色，直到你手动点击“重置”
            Thread.sleep(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initializeViewer() {
        if (gsGraph == null)
            return;

        // 🚩 核心：加载外部 CSS 文件
        try {
            // 使用 ClassLoader 加载资源路径
            String stylesheet = getClass().getResource("/style/graph.css").toExternalForm();
            gsGraph.setAttribute("ui.stylesheet", "url('" + stylesheet + "')");
        } catch (NullPointerException e) {
            System.err.println("[Error] 样式文件加载失败，请检查路径: /style/graph_style.css");
            // 如果文件找不到，可以回退到默认样式，避免界面崩掉
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