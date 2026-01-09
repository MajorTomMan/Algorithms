package com.majortom.algorithms.core.visualization.impl.frame;

import javax.swing.SwingUtilities;
import com.majortom.algorithms.core.graph.BaseGraph;
import com.majortom.algorithms.core.graph.BaseGraphAlgorithms;
import com.majortom.algorithms.core.graph.node.Vertex;
import com.majortom.algorithms.core.visualization.BaseFrame;
import com.majortom.algorithms.core.visualization.impl.panel.GraphPanel;
import com.majortom.algorithms.utils.GraphUtils;

public class GraphFrame<V> extends BaseFrame<BaseGraph<V>> {
    private final GraphPanel<V> canvas;
    private final BaseGraph<V> graph;

    public GraphFrame(BaseGraph<V> graph) {
        super("图算法实验室 - " + graph.getClass().getSimpleName());
        this.graph = graph;
        this.canvas = new GraphPanel<>(graph);

        // 改造：使用 MigLayout 约束，将画布放入中心并自动拉伸
        add(canvas, "center, grow");

        graph.setListener(this);
        setupActions();
    }

    private void setupActions() {
        startBtn.setText("开始遍历");
    }

    @Override
    protected void initAndLaunch() {
        super.initAndLaunch();
        // 自动布局逻辑
        GraphUtils.autoLayout(graph.getVertices(), canvas.getWidth(), canvas.getHeight());
        canvas.repaint();
    }

    @Override
    protected void refresh(BaseGraph<V> data, Object a, Object b) {
        canvas.updateData(data, a, b);

        // 拼接遍历路径显示在侧边栏 dataListArea
        StringBuilder sb = new StringBuilder("遍历序列:\n");
        boolean first = true;
        for (Vertex<V> v : data.getVertices()) {
            if (v.isVisited()) {
                if (!first)
                    sb.append(" -> ");
                sb.append(v.getData());
                first = false;
            }
        }
        dataListArea.setText(sb.toString());
    }

    /**
     * 静态启动入口
     */
    public static <V> void launch(BaseGraph<V> graph, BaseGraphAlgorithms<V> executor, V startData) {
        SwingUtilities.invokeLater(() -> {
            GraphFrame<V> frame = new GraphFrame<>(graph);

            frame.setTask(() -> {
                Vertex<V> startNode = graph.findVertex(startData);
                if (startNode != null) {
                    executor.run(graph, startNode);
                } else {
                    System.err.println("Error: Vertex " + startData + " not found.");
                }
            });

            frame.initAndLaunch();
        });
    }

    @Override
    protected void handleDataReset() {
        // 重置数据状态
        this.graph.resetGraphNodes();
        // 同步 UI 画布
        this.canvas.updateData(this.graph, null, null);
    }
}