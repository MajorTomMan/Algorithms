package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.maze.impl.GraphMaze;
import com.majortom.algorithms.visualization.impl.visualizer.GraphMazeVisualizer;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.manager.AlgorithmThreadManager;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;

/**
 * 图结构迷宫模块控制器。
 *
 * <p>控制器负责把 UI 事件接到数据结构和可视化器上：尺寸滑块会重建 {@link GraphMaze}，
 * 开始按钮会触发初始快照，重置按钮会恢复图迷宫状态。真正的算法逻辑不写在这里，
 * 后续应通过 {@code BaseGraphMazeAlgorithms} 子类接入执行框架。</p>
 *
 * <p>可以把它看成联动链路里的“调度台”：用户操作进入控制器，控制器准备结构和算法，
 * 执行层负责播放步骤，可视化器负责画面。控制器只协调，不直接实现生成或寻路。</p>
 */
public class GraphMazeController extends BaseModuleController<GraphMaze> {

    /**
     * 当前图迷宫实体。
     */
    private GraphMaze mazeEntity;

    /**
     * 控制迷宫尺寸的滑块。
     */
    @FXML
    private Slider sizeSlider;

    /**
     * 展示当前规范化后的迷宫尺寸。
     */
    @FXML
    private Label sizeValueLabel;

    /**
     * 尺寸/密度区域的文案标签。
     */
    @FXML
    private Label densityLabel;

    /**
     * 创建图迷宫控制器。
     *
     * @param mazeEntity 图迷宫实体
     * @param visualizer 图迷宫可视化器
     */
    public GraphMazeController(GraphMaze mazeEntity, GraphMazeVisualizer visualizer) {
        super(visualizer, "/fxml/GraphMazeControls.fxml");
        this.mazeEntity = mazeEntity;
    }

    /**
     * 初始化尺寸控件和初始画面。
     *
     * <p>FXML 加载完成后会调用这里。它会绑定尺寸滑块、渲染初始图迷宫，并刷新统计栏。
     * 如果算法正在运行，滑块变化不会立即重建结构，避免把执行中的快照链打断。</p>
     */
    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        super.initialize(location, resources);

        if (sizeSlider != null) {
            updateSizeValueLabel(sizeSlider.getValue());
            sizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                int oddSize = updateSizeValueLabel(newVal.doubleValue());
                if (!AlgorithmThreadManager.isRunning()) {
                    updateMazeSize(oddSize);
                }
            });
        }

        if (mazeEntity != null) {
            mazeEntity.initialSilent();
            visualizer.render(mazeEntity, null, null);
        }
        refreshStatsDisplay();
    }

    /**
     * 处理开始按钮。
     *
     * <p>当前没有默认图迷宫生成器或寻路器，因此点击开始只发出当前迷宫的初始快照。
     * 这个入口先保留下来，后续接入 {@code BaseGraphMazeAlgorithms} 后，可以沿用同一个用户操作路径。</p>
     */
    @Override
    public void handleAlgorithmStart() {
        if (mazeEntity == null || AlgorithmThreadManager.isRunning()) {
            return;
        }
        mazeEntity.initial();
        visualizer.render(mazeEntity, null, null);
        appendLog(I18N.text("message.graph_maze.ready"));
    }

    /**
     * 执行图迷宫算法。
     *
     * <p>这里暂时留空，因为图迷宫生成器和寻路器还没有注册进主链路。
     * 后续接入时，这里应该调用图迷宫算法的安全执行入口，并让执行上下文把每一步同步到可视化层。</p>
     *
     * @param algorithm 待执行的图迷宫算法
     * @param data 图迷宫数据结构
     */
    @Override
    protected void executeAlgorithm(BaseAlgorithms<GraphMaze> algorithm, GraphMaze data) {
        // Reserved for future BaseGraphMazeAlgorithms implementations.
    }

    /**
     * 格式化图迷宫统计信息。
     *
     * <p>统计栏同时展示网格规模和图规模：节点数来自行列乘积，边数来自底层 GraphStream 图。
     * 这能帮助学习者把“迷宫格子”和“图节点/边”对应起来。</p>
     */
    @Override
    protected String formatStatsMessage() {
        if (mazeEntity == null) {
            return I18N.text("stats.graph_maze.empty");
        }
        return String.format("%s | %s | %s",
                I18N.text("stats.graph_maze.nodes", mazeEntity.getRows() * mazeEntity.getCols()),
                I18N.text("stats.graph_maze.edges", mazeEntity.asGraph().getGraph().getEdgeCount()),
                I18N.text("stats.maze.scale", mazeEntity.getCols(), mazeEntity.getRows()));
    }

    /**
     * 重置图迷宫数据。
     */
    @Override
    protected void onResetData() {
        if (mazeEntity != null) {
            mazeEntity.initialSilent();
            visualizer.render(mazeEntity, null, null);
            refreshStatsDisplay();
            appendLog(I18N.text("message.graph_maze.reset"));
        }
    }

    /**
     * 绑定图迷宫模块文案。
     */
    @Override
    protected void setupI18n() {
        if (densityLabel != null) {
            densityLabel.textProperty().bind(I18N.createStringBinding("label.graph_maze.size"));
        }
    }

    /**
     * 获取模块 ID。
     */
    @Override
    protected String moduleId() {
        return "graph-maze";
    }

    /**
     * 按尺寸重建图迷宫。
     *
     * <p>重建会先停止当前算法，再创建新的 {@link GraphMaze} 并刷新画面。
     * 这样可以避免旧执行帧继续作用在已经替换的数据结构上。</p>
     *
     * @param oddSize 已规范化为奇数的迷宫尺寸
     */
    private void updateMazeSize(int oddSize) {
        stopAlgorithm();
        this.mazeEntity = new GraphMaze(oddSize, oddSize);
        this.mazeEntity.initialSilent();
        visualizer.render(mazeEntity, null, null);
        refreshStatsDisplay();
    }

    /**
     * 更新尺寸标签，并把偶数尺寸修正为奇数。
     *
     * <p>迷宫常用奇数尺寸来保证墙和道路交错生成。这里把 UI 滑块值规范化，
     * 避免结构层收到不适合迷宫生成的尺寸。</p>
     *
     * @param rawSize 滑块原始尺寸值
     * @return 规范化后的奇数尺寸
     */
    private int updateSizeValueLabel(double rawSize) {
        int val = (int) rawSize;
        int oddSize = (val % 2 == 0) ? val + 1 : val;
        if (sizeValueLabel != null) {
            sizeValueLabel.setText(oddSize + "x" + oddSize);
        }
        return oddSize;
    }
}
