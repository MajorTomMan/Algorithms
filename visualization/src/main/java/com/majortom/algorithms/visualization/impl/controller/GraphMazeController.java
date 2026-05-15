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
 * <p>第一版只负责展示和重置 {@link GraphMaze}，不绑定任何具体生成或寻路算法。
 * 后续你实现 {@code BaseGraphMazeAlgorithms} 子类后，可以在这里添加算法选择和启动入口。</p>
 */
public class GraphMazeController extends BaseModuleController<GraphMaze> {

    /**
     * 当前图迷宫实体。
     */
    private GraphMaze mazeEntity;

    @FXML
    private Slider sizeSlider;

    @FXML
    private Label sizeValueLabel;

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
     * 第一版没有默认算法，点击开始只刷新当前图迷宫快照。
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
     * 图迷宫第一版不执行具体算法。
     */
    @Override
    protected void executeAlgorithm(BaseAlgorithms<GraphMaze> algorithm, GraphMaze data) {
        // Reserved for future BaseGraphMazeAlgorithms implementations.
    }

    /**
     * 格式化图迷宫统计信息。
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
