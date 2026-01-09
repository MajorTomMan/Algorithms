package com.majortom.algorithms.core.visualization.impl.frame;

import javax.swing.SwingUtilities;
import com.majortom.algorithms.core.graph.BaseGraph;
import com.majortom.algorithms.core.graph.BaseGraphAlgorithms;
import com.majortom.algorithms.core.graph.node.Vertex;
import com.majortom.algorithms.core.visualization.BaseFrame;
import com.majortom.algorithms.core.visualization.impl.panel.GraphPanel;
import com.majortom.algorithms.utils.GraphUtils;
import java.awt.BorderLayout;

public class GraphFrame<V> extends BaseFrame<BaseGraph<V>> {
    private final GraphPanel<V> canvas;
    private final BaseGraph<V> graph;

    public GraphFrame(BaseGraph<V> graph) {
        super("图算法实验室 - " + graph.getClass().getSimpleName());
        this.graph = graph;
        this.canvas = new GraphPanel<>(graph);
        add(canvas, BorderLayout.CENTER);

        // 注意：现在是图作为数据源被监听
        graph.setListener(this);
        setupActions();
    }

    private void setupActions() {
        startBtn.setText("开始执行");
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
     * 静态启动入口（支持算法分离架构）
     * * @param graph 图的结构（Directed/Undirected）
     * 
     * @param executor  具体的算法逻辑实现（BFS/DFS/Dijkstra）
     * @param startData 起点数据
     */
    public static <V> void launch(BaseGraph<V> graph, BaseGraphAlgorithms<V> executor, V startData) {
        SwingUtilities.invokeLater(() -> {
            GraphFrame<V> frame = new GraphFrame<>(graph);

            // 任务装载：将算法执行器与图数据绑定
            frame.setTask(() -> {
                Vertex<V> startNode = graph.findVertex(startData);
                if (startNode != null) {
                    // 由执行器跑算法逻辑，操作 graph 数据
                    executor.run(graph, startNode);
                } else {
                    System.err.println("错误：未找到起始节点 " + startData);
                }
            });

            frame.initAndLaunch();
        });
    }
}