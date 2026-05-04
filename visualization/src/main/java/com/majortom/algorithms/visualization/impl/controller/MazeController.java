package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.BaseMazeAlgorithms;
import com.majortom.algorithms.core.maze.algorithms.generate.*;
import com.majortom.algorithms.core.maze.algorithms.pathfinding.*;
import com.majortom.algorithms.core.maze.impl.ArrayMaze;
import com.majortom.algorithms.utils.EffectUtils;
import com.majortom.algorithms.visualization.VisualizationActionType;
import com.majortom.algorithms.visualization.base.BaseMazeVisualizer;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.manager.AlgorithmThreadManager;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * 迷宫算法控制器
 * 职责：管理迷宫生成与寻路的策略切换，处理尺寸动态调整与可视化渲染同步。
 */
public class MazeController<T> extends BaseModuleController<BaseMaze<T>> {

    private enum MazeMode {
        GENERATION, SOLVING
    }

    private BaseMazeAlgorithms<T> currentAlgorithm;
    private BaseMaze<T> mazeEntity;
    private MazeMode currentMode = MazeMode.GENERATION;

    @FXML
    private Slider sizeSlider;
    @FXML
    private Label sizeValueLabel;
    @FXML
    private ComboBox<String> algoSelector;
    @FXML
    private ComboBox<String> solverSelector;
    @FXML
    private Label densityLabel, genTitleLabel, solveTitleLabel;
    @FXML
    private Button buildBtn, solveBtn;

    public MazeController(BaseMaze<T> mazeEntity,
            BaseMazeAlgorithms<T> generator,
            BaseMazeVisualizer<BaseMaze<T>> visualizer) {
        super(visualizer, "/fxml/MazeControls.fxml");
        this.mazeEntity = mazeEntity;
        this.currentAlgorithm = generator;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);

        // 1. 尺寸变更逻辑：实时更新网格规模
        if (sizeSlider != null) {
            updateSizeValueLabel(sizeSlider.getValue());
            sizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                int oddSize = updateSizeValueLabel(newVal.doubleValue());

                if (!AlgorithmThreadManager.isRunning()) {
                    updateMazeSize(oddSize);
                }
            });
        }

        // 2. 初始数据状态
        if (mazeEntity != null) {
            mazeEntity.initialSilent();
        }

        EffectUtils.applyDynamicEffect(buildBtn, solveBtn);
        visualizer.render(mazeEntity);
        refreshStatsDisplay();
    }

    /**
     * 响应全局 Start 按钮：默认执行生成逻辑
     */
    @Override
    public void handleAlgorithmStart() {
        if (!AlgorithmThreadManager.isRunning()) {
            handleGenerate();
        }

    }

    /**
     * UI 事件：处理迷宫生成请求
     */
    @FXML
    public void handleGenerate() {
        int index = algoSelector.getSelectionModel().getSelectedIndex();
        dispatchVisualizerAction(VisualizationActionType.MAZE_BUILD, Map.of(
                "rows", mazeEntity == null ? 0 : mazeEntity.getRows(),
                "cols", mazeEntity == null ? 0 : mazeEntity.getCols(),
                "algorithmId", generatorAlgorithmId(index)));
        stopAlgorithm();
        mazeEntity.initialSilent();
        currentMode = MazeMode.GENERATION;

        // 策略切换
        this.currentAlgorithm = switch (index) {
            case 1 -> (BaseMazeAlgorithms<T>) new DFSMazeGenerator();
            case 2 -> (BaseMazeAlgorithms<T>) new UnionFindMazeGenerator();
            default -> (BaseMazeAlgorithms<T>) new BFSMazeGenerator();
        };

        if (this.currentAlgorithm != null) {
            this.currentAlgorithm.setMazeEntity(mazeEntity);
            startAlgorithm(currentAlgorithm, mazeEntity);
        }
    }

    /**
     * UI 事件：处理寻路请求
     */
    @FXML
    public void handleSolve() {
        if (AlgorithmThreadManager.isRunning()) {
            return;
        }
        if (mazeEntity == null) {
            return;
        }

        int index = solverSelector.getSelectionModel().getSelectedIndex();
        dispatchVisualizerAction(VisualizationActionType.MAZE_SOLVE, Map.of(
                "rows", mazeEntity.getRows(),
                "cols", mazeEntity.getCols(),
                "solverId", solverAlgorithmId(index)));

        mazeEntity.setGenerated(true);
        mazeEntity.clearVisualStates();
        mazeEntity.pickRandomPointsOnAvailablePaths();
        visualizer.render(mazeEntity, null, null);
        appendLog(I18N.text("message.maze.snapshot_ready"));

        BaseMazeAlgorithms<T> solver = switch (index) {
            case 1 -> (BaseMazeAlgorithms<T>) new DFSMazePathfinder();
            default -> (BaseMazeAlgorithms<T>) new AStarMazePathfinder();
        };

        currentMode = MazeMode.SOLVING;
        currentAlgorithm = solver;

        if (currentAlgorithm != null) {
            currentAlgorithm.setMazeEntity(mazeEntity);
            startAlgorithm(currentAlgorithm, mazeEntity);
        }
    }

    @Override
    protected void executeAlgorithm(BaseAlgorithms<BaseMaze<T>> alg, BaseMaze<T> data) {
        if (alg instanceof BaseMazeAlgorithms) {
            ((BaseMazeAlgorithms<T>) alg).execute(data);
        }
    }

    private void updateMazeSize(int oddSize) {
        stopAlgorithm();
        // 重新实例化数据结构
        this.mazeEntity = (BaseMaze<T>) new ArrayMaze(oddSize, oddSize);
        this.mazeEntity.initialSilent();
        visualizer.render(mazeEntity, null, null);
        refreshStatsDisplay();
    }

    @Override
    protected String formatStatsMessage() {
        String mode = (currentMode == MazeMode.SOLVING) ? "Pathfinding" : "Generation";
        return String.format("%s | %s | %s",
                I18N.text("stats.maze.mode", mode),
                formatMetric("stats.action", stats.actionCount()),
                I18N.text("stats.maze.scale", mazeEntity.getCols(), mazeEntity.getRows()));
    }

    @Override
    protected void onResetData() {
        if (mazeEntity != null) {
            mazeEntity.initialSilent();
            visualizer.render(mazeEntity, null, null);
            refreshStatsDisplay();
            appendLog(I18N.text("message.maze.reset"));
        }
    }

    private int updateSizeValueLabel(double rawSize) {
        int val = (int) rawSize;
        int oddSize = (val % 2 == 0) ? val + 1 : val;
        if (sizeValueLabel != null) {
            sizeValueLabel.setText(oddSize + "x" + oddSize);
        }
        return oddSize;
    }

    @Override
    protected void setupI18n() {
        densityLabel.textProperty().bind(I18N.createStringBinding("label.maze.size"));
        genTitleLabel.textProperty().bind(I18N.createStringBinding("label.maze.generator"));
        solveTitleLabel.textProperty().bind(I18N.createStringBinding("label.maze.solver"));
        buildBtn.textProperty().bind(I18N.createStringBinding("action.maze.build"));
        solveBtn.textProperty().bind(I18N.createStringBinding("action.maze.solve"));

        // ComboBox 数据源绑定
        bindComboBox(algoSelector, List.of("algorithm.maze.generate.bfs", "algorithm.maze.generate.dfs", "algorithm.maze.generate.uf"));
        bindComboBox(solverSelector, List.of("algorithm.maze.solve.astar", "algorithm.maze.solve.dfs"));
    }

    private void bindComboBox(ComboBox<String> comboBox, List<String> keys) {
        comboBox.itemsProperty().bind(Bindings.createObjectBinding(() -> {
            ObservableList<String> list = FXCollections.observableArrayList();
            keys.forEach(key -> list.add(I18N.getBundle().getString(key)));
            return list;
        }, I18N.localeProperty()));

        AlgorithmThreadManager.postStatus(() -> comboBox.getSelectionModel().selectFirst());
    }

    @Override
    protected String moduleId() {
        return "maze";
    }

    private String generatorAlgorithmId(int index) {
        return switch (index) {
            case 1 -> "dfs-maze-generator";
            case 2 -> "union-find-maze-generator";
            default -> "bfs-maze-generator";
        };
    }

    private String solverAlgorithmId(int index) {
        return switch (index) {
            case 1 -> "dfs-maze-pathfinder";
            default -> "astar-maze-pathfinder";
        };
    }
}
