package com.majortom.algorithms.core.visualization.impl.window;

import com.majortom.algorithms.core.graph.BaseGraph;
import com.majortom.algorithms.core.graph.BaseGraphAlgorithms;
import com.majortom.algorithms.core.graph.node.Vertex;
import com.majortom.algorithms.core.visualization.BaseWindow;
import com.majortom.algorithms.core.visualization.impl.visualizer.GraphVisualizer;
import com.majortom.algorithms.utils.GraphUtils;
import javafx.application.Platform;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

/**
 * 图算法实验室窗口
 * 职责：适配 FXML 架构，管理图结构的力导向布局与遍历动画
 */
public class GraphWindow<V> extends BaseWindow<BaseGraph<V>> {

    private GraphVisualizer<V> canvas;
    private final BaseGraph<V> graph;
    private BaseGraphAlgorithms<V> executor;

    public GraphWindow(BaseGraph<V> graph) {
        this.graph = graph;
    }

    @Override
    protected Region createCenterComponent() {
        // 创建图算法专用的渲染画布
        this.canvas = new GraphVisualizer<>(graph);
        return canvas;
    }

    @Override
    protected void setupActions() {
        // 修改按钮文本
        startBtn.setText("开始遍历");

        // 1. 绑定算法监听器到基类的 onSync 接口
        this.executor.setListener((data, a, b, comp, act) -> {
            this.onSync(data, a, b, comp, act);
        });

        // 2. 注意：此处不需要手动 setupActions，基类已经处理了 startBtn 的点击逻辑
        // 我们只需要确保 task 已经通过 launch 方法设置好了
    }

    /**
     * 自动布局初始化：在窗口显示后调用，分配节点初始坐标
     */
    public void initAndLayout(double width, double height) {
        // 使用工具类执行自动布局计算
        GraphUtils.autoLayout(graph.getVertices(), (int) width, (int) height);
        // 数据更新后手动触发一次重绘
        canvas.setData(graph);
    }

    @Override
    protected void refresh(BaseGraph<V> data, Object a, Object b) {
        // 1. 更新画布高亮状态
        canvas.updateState(data, a, b);

        // 2. 拼接遍历路径显示在右侧 logArea (对应 XML 中的 fx:id)
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
        logArea.setText(sb.toString());
    }

    @Override
    protected void handleDataReset() {
        // 1. 清除图节点的访问标记
        this.graph.resetGraphNodes();
        // 2. 同步 UI
        canvas.updateState(this.graph, null, null);
        logArea.clear();
    }

    /**
     * 静态启动入口
     */
    public static <V> void launch(BaseGraph<V> graph, BaseGraphAlgorithms<V> executor, V startData) {
        Platform.runLater(() -> {
            try {
                GraphWindow<V> window = new GraphWindow<>(graph);
                window.executor = executor;

                Stage stage = new Stage();
                // 启动窗口（加载 FXML 并在内部调用 setupActions）
                window.start(stage);

                // 设置具体的算法执行任务
                window.setTask(() -> {
                    Vertex<V> startNode = graph.findVertex(startData);
                    if (startNode != null) {
                        executor.run(graph, startNode);
                    }
                });

                // 执行布局计算：利用 FXML 定义的初始大小
                window.initAndLayout(1000, 700);

                stage.setTitle("图遍历实验室 - " + executor.getClass().getSimpleName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}