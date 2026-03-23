package com.majortom.algorithms.visualization.base;

import com.majortom.algorithms.core.graph.BaseGraph;
import com.majortom.algorithms.visualization.BaseVisualizer;

import javafx.application.Platform;
import org.graphstream.graph.Graph;
import org.graphstream.ui.fx_viewer.FxViewPanel;
import org.graphstream.ui.fx_viewer.FxViewer;

/**
 * 图算法可视化抽象基类
 * 职责：集成 GraphStream 渲染引擎，管理 ViewPanel 布局与样式表加载
 */
public abstract class BaseGraphVisualizer<V> extends BaseVisualizer<BaseGraph<V>> {

    protected FxViewer viewer;
    protected FxViewPanel viewPanel;
    protected final Graph gsGraph;

    public BaseGraphVisualizer(BaseGraph<V> baseGraph) {
        System.setProperty("org.graphstream.ui", "javafx");
        this.gsGraph = baseGraph.getGraph();
        // 延迟初始化 Viewer，确保 JavaFX 环境就绪
        Platform.runLater(this::initializeGraphEngine);
    }

    private void initializeGraphEngine() {
        if (gsGraph == null)
            return;

        // 设置全局抗锯齿
        gsGraph.setAttribute("ui.antialias");

        // 加载样式表（由子类提供路径）
        String stylePath = getStyleSheetPath();
        if (stylePath != null) {
            try {
                String stylesheet = getClass().getResource(stylePath).toExternalForm();
                gsGraph.setAttribute("ui.stylesheet", "url('" + stylesheet + "')");
            } catch (Exception e) {
                System.err.println("[BaseGraphVisualizer] 样式文件加载失败: " + stylePath);
            }
        }

        this.viewer = new FxViewer(gsGraph, FxViewer.ThreadingModel.GRAPH_IN_GUI_THREAD);
        setupLayout();

        this.viewPanel = (FxViewPanel) viewer.addDefaultView(false);

        // 布局组装
        this.getChildren().clear();
        this.getChildren().addAll(viewPanel, canvas);

        // Canvas 层设为鼠标透明，允许用户操作底层的 GraphStream 节点
        canvas.setMouseTransparent(true);

        // 绑定宽高
        viewPanel.prefWidthProperty().bind(this.widthProperty());
        viewPanel.prefHeightProperty().bind(this.heightProperty());
    }

    /**
     * 子类需提供 CSS 样式文件路径
     */
    protected abstract String getStyleSheetPath();

    /**
     * 布局设置，默认为开启自动布局，子类可重写以固定位置
     */
    protected void setupLayout() {
        if (viewer != null)
            viewer.enableAutoLayout();
    }

    @Override
    public void clear() {
        if (gsGraph != null) {
            gsGraph.nodes().forEach(n -> n.removeAttribute("ui.class"));
            gsGraph.edges().forEach(e -> e.removeAttribute("ui.class"));
        }
    }
}