package com.majortom.algorithms.visualization.base;

import com.majortom.algorithms.core.graph.BaseGraph;
import com.majortom.algorithms.visualization.BaseVisualizer;

import javafx.application.Platform;
import org.graphstream.graph.Graph;
import org.graphstream.ui.fx_viewer.FxViewPanel;
import org.graphstream.ui.fx_viewer.FxViewer;

/**
 * 图算法可视化抽象基类。
 *
 * <p>图模块使用 GraphStream 自己的 JavaFX 面板渲染节点和边，因此这个类负责把
 * GraphStream 的 {@link FxViewer} 嵌入项目统一的 {@link BaseVisualizer} 布局中。
 * Canvas 会作为透明叠加层保留，未来可用于绘制额外提示。</p>
 *
 * @param <V> 图节点业务数据类型
 */
public abstract class BaseGraphVisualizer<V> extends BaseVisualizer<BaseGraph<V>> {

    /**
     * GraphStream JavaFX viewer。
     */
    protected FxViewer viewer;

    /**
     * GraphStream 的 JavaFX 视图面板。
     */
    protected FxViewPanel viewPanel;

    /**
     * 被渲染的 GraphStream 图实例。
     */
    protected final Graph gsGraph;

    /**
     * 创建图可视化组件。
     *
     * @param baseGraph 图结构包装器
     */
    public BaseGraphVisualizer(BaseGraph<V> baseGraph) {
        System.setProperty("org.graphstream.ui", "javafx");
        this.gsGraph = baseGraph.getGraph();
        // 延迟初始化 Viewer，确保 JavaFX 环境就绪
        Platform.runLater(this::initializeGraphEngine);
    }

    /**
     * 初始化 GraphStream 渲染引擎。
     */
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
     * 子类提供 GraphStream CSS 样式文件路径。
     *
     * @return classpath 下的样式文件路径
     */
    protected abstract String getStyleSheetPath();

    /**
     * 配置 GraphStream 布局策略。
     *
     * <p>默认开启自动布局，子类可以重写以固定节点位置。</p>
     */
    protected void setupLayout() {
        if (viewer != null)
            viewer.enableAutoLayout();
    }

    /**
     * 清除图上的可视化状态。
     */
    @Override
    public void clear() {
        if (gsGraph != null) {
            gsGraph.nodes().forEach(n -> n.removeAttribute("ui.class"));
            gsGraph.edges().forEach(e -> e.removeAttribute("ui.class"));
        }
    }
}
