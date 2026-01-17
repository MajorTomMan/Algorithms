package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.BaseMazeAlgorithms;
import com.majortom.algorithms.core.maze.algorithms.generate.*;
import com.majortom.algorithms.core.maze.algorithms.pathfinding.*;
import com.majortom.algorithms.core.maze.impl.ArrayMaze;
import com.majortom.algorithms.utils.EffectUtils;
import com.majortom.algorithms.visualization.base.BaseMazeVisualizer;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.manager.AlgorithmThreadManager;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * 迷宫算法控制器
 * 职责：管理迷宫生成与寻路的策略切换，处理尺寸动态调整与可视化渲染同步。
 */
public class MazeController<T> extends BaseModuleController<BaseMaze<T>> {

    private BaseMazeAlgorithms<T> currentAlgorithm;
    private BaseMaze<T> mazeEntity;

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
            sizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                int val = newVal.intValue();
                int oddSize = (val % 2 == 0) ? val + 1 : val;
                sizeValueLabel.setText(oddSize + "x" + oddSize);

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
        stopAlgorithm();
        mazeEntity.initialSilent();

        int index = algoSelector.getSelectionModel().getSelectedIndex();
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
        AlgorithmThreadManager.run(() -> {
            if (mazeEntity != null) {
                mazeEntity.setGenerated(true);
                mazeEntity.clearVisualStates();
                mazeEntity.pickRandomPointsOnAvailablePaths();

                Platform.runLater(() -> {
                    this.visualizer.render(mazeEntity, null, null);
                    logArea.appendText("System: Snapshot prepared. Starting solver...\n");
                });

                int index = solverSelector.getSelectionModel().getSelectedIndex();
                BaseMazeAlgorithms<T> solver = (index == 0)
                        ? (BaseMazeAlgorithms<T>) new AStarMazePathfinder()
                        : (BaseMazeAlgorithms<T>) new BFSMazePathfinder();

                solver.setMazeEntity(mazeEntity);
                solver.execute(mazeEntity);
            }
        });
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
    }

    @Override
    protected String formatStatsMessage() {
        String mode = (currentAlgorithm instanceof BaseMazeAlgorithms) ? "Pathfinding" : "Generation";
        return String.format("[%s]\nVisited: %d | Scale: %dx%d",
                mode, stats.actionCount, mazeEntity.getCols(), mazeEntity.getRows());
    }

    @Override
    protected void onResetData() {
        if (mazeEntity != null) {
            mazeEntity.initialSilent();
            visualizer.render(mazeEntity, null, null);
            appendLog("Maze reset to initial state.");
        }
    }

    @Override
    protected void setupI18n() {
        densityLabel.textProperty().bind(I18N.createStringBinding("ctrl.maze.density"));
        genTitleLabel.textProperty().bind(I18N.createStringBinding("ctrl.maze.gen_title"));
        solveTitleLabel.textProperty().bind(I18N.createStringBinding("ctrl.maze.solve_title"));
        buildBtn.textProperty().bind(I18N.createStringBinding("btn.maze.build"));
        solveBtn.textProperty().bind(I18N.createStringBinding("btn.maze.solve"));

        // ComboBox 数据源绑定
        bindComboBox(algoSelector, List.of("maze.algo.bfs", "maze.algo.dfs", "maze.algo.prim"));
        bindComboBox(solverSelector, List.of("maze.solver.astar", "maze.solver.dfs"));
    }

    private void bindComboBox(ComboBox<String> comboBox, List<String> keys) {
        comboBox.itemsProperty().bind(Bindings.createObjectBinding(() -> {
            ObservableList<String> list = FXCollections.observableArrayList();
            keys.forEach(key -> list.add(I18N.getBundle().getString(key)));
            return list;
        }, I18N.localeProperty()));

        Platform.runLater(() -> comboBox.getSelectionModel().selectFirst());
    }
}