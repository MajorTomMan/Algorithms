package com.majortom.algorithms.core.visualization.base;

import com.majortom.algorithms.core.graph.BaseGraph;
import com.majortom.algorithms.core.visualization.BaseVisualizer;
import javafx.application.Platform;
import org.graphstream.graph.Graph;
import org.graphstream.ui.fx_viewer.FxViewPanel;
import org.graphstream.ui.fx_viewer.FxViewer;

/**
 * 图算法视觉呈现基类
 * 职责：
 * 1. 桥接 GraphStream 引擎与 JavaFX Canvas 体系。
 * 2. 统一加载基于《乱》色彩方案的 CSS 样式表。
 * 3. 管理图节点的动态布局与高亮逻辑。
 */
public abstract class BaseGraphVisualizer<V> extends BaseVisualizer<BaseGraph<V>> {

    protected FxViewer viewer;
    protected FxViewPanel viewPanel;
    protected Graph gsGraph;

    @Override
    protected void draw(BaseGraph<V> data, Object a, Object b) {
        if (data == null)
            return;

        // 第一次渲染时初始化引擎
        if (gsGraph == null) {
            this.gsGraph = data.getGraph();
            Platform.runLater(this::initializeGraphEngine);
        }

        // 调用具体的业务高亮逻辑（如变蓝、变黄）
        updateGraphStyles(data, a, b);
    }

    /**
     * 初始化 GraphStream 渲染引擎
     */
    private void initializeGraphEngine() {
        System.setProperty("org.graphstream.ui", "javafx");

        // 1. 应用样式表（定义在外部 CSS 中，映射 RAN_RED, RAN_BLUE 等）
        applyRanStylesheet();

        // 2. 创建查看器并开启自动布局（力导向）
        this.viewer = new FxViewer(gsGraph, FxViewer.ThreadingModel.GRAPH_IN_GUI_THREAD);
        this.viewer.enableAutoLayout();

        // 3. 将图层嵌入当前 Visualizer 容器
        this.viewPanel = (FxViewPanel) viewer.addDefaultView(false);
        this.getChildren().add(0, viewPanel); // 放在 Canvas 之下

        // 4. 绑定尺寸
        viewPanel.prefWidthProperty().bind(this.widthProperty());
        viewPanel.prefHeightProperty().bind(this.heightProperty());
    }

    /**
     * 子类实现：具体的算法高亮逻辑
     * 
     * @param a 当前活跃节点 (Active Node)
     * @param b 辅助/邻居节点 (Target/Neighbor)
     */
    protected abstract void updateGraphStyles(BaseGraph<V> data, Object a, Object b);

    /**
     * 加载外部样式表
     */
    protected void applyRanStylesheet() {
        try {
            // 这里建议在资源文件夹下建立 graph.css，将 RAN_RED 等色值写入
            String stylesheet = getClass().getResource("/style/graph.css").toExternalForm();
            gsGraph.setAttribute("ui.stylesheet", "url('" + stylesheet + "')");
        } catch (Exception e) {
            // 如果加载失败，使用备用硬编码样式
            gsGraph.setAttribute("ui.stylesheet", getDefaultFallbackStyle());
        }
        gsGraph.setAttribute("ui.antialias");
    }

    private String getDefaultFallbackStyle() {
        return "node { fill-color: #B40000; size: 30px; text-size: 16px; text-color: #F0F0E6; }" +
                "node.highlight { fill-color: #0078FF; size: 35px; }" +
                "node.secondary { fill-color: #DCB400; }" +
                "edge { fill-color: #3C3C46; width: 2px; }";
    }

    @Override
    public void clearCanvas() {
        super.clearCanvas();
        if (gsGraph != null) {
            gsGraph.nodes().forEach(n -> n.removeAttribute("ui.class"));
            gsGraph.edges().forEach(e -> e.removeAttribute("ui.class"));
        }
    }
}