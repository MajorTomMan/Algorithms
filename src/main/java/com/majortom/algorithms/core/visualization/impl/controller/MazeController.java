package com.majortom.algorithms.core.visualization.impl.controller;

import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.BaseMazeAlgorithms;
import com.majortom.algorithms.core.maze.algorithms.generate.*;
import com.majortom.algorithms.core.maze.algorithms.pathfinding.*;
import com.majortom.algorithms.core.maze.impl.ArrayMaze;
import com.majortom.algorithms.core.visualization.BaseController;
import com.majortom.algorithms.core.visualization.base.BaseMazeVisualizer;
import com.majortom.algorithms.core.visualization.international.I18N;
import com.majortom.algorithms.core.visualization.manager.AlgorithmThreadManager;
import com.majortom.algorithms.utils.EffectUtils;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

/**
 * 迷宫算法控制器
 * 职责：统筹迷宫的生成与寻路逻辑，并管理状态回滚。
 */
public class MazeController<T> extends BaseController<BaseMaze<T>> {

    private BaseMazeAlgorithms<T> mazeGenerator;
    private BaseMaze<T> mazeEntity;
    private Node customControlPane;

    @FXML
    private Slider sizeSlider;
    @FXML
    private Label sizeValueLabel, densityLabel, genTitleLabel, solveTitleLabel;
    @FXML
    private ComboBox<String> algoSelector, solverSelector;
    @FXML
    private Button buildBtn, solveBtn;

    public MazeController(BaseMaze<T> mazeEntity,
            BaseMazeAlgorithms<T> generator,
            BaseMazeVisualizer<BaseMaze<T>> visualizer) {
        super(visualizer);
        this.mazeEntity = mazeEntity;
        this.mazeGenerator = generator;
        loadFXMLControls();
    }

    private void loadFXMLControls() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MazeControls.fxml"));
            loader.setResources(I18N.getBundle()); // 使用统一的 I18N
            loader.setController(this);
            this.customControlPane = loader.load();
        } catch (IOException e) {
            System.err.println("[Error] Maze FXML load failed: " + e.getMessage());
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);
        setupI18n();

        if (sizeSlider != null) {
            sizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                int oddSize = (newVal.intValue() % 2 == 0) ? newVal.intValue() + 1 : newVal.intValue();
                sizeValueLabel.setText(oddSize + "x" + oddSize);
                if (!AlgorithmThreadManager.isRunning()) {
                    refreshMazeRealtime(newVal.intValue());
                }
            });
        }

        mazeEntity.initialSilent();
        EffectUtils.applyDynamicEffect(buildBtn);
        EffectUtils.applyDynamicEffect(solveBtn);
    }

    // --- 实现 BaseController 核心动作接口 ---

    @SuppressWarnings("unchecked")
    @Override
    public void handleStartAction() {
        // 统一入口：点击底部 Start 按钮，执行“生成 + 寻路”全流程
        if (mazeEntity != null) {
            // 1. 停止当前所有动作并重置状态
            stopAlgorithm();
            mazeEntity.initialSilent();

            // 2. 准备生成器
            int genIndex = algoSelector.getSelectionModel().getSelectedIndex();
            this.mazeGenerator = switch (genIndex) {
                case 1 -> (BaseMazeAlgorithms<T>) new DFSMazeGenerator();
                case 2 -> (BaseMazeAlgorithms<T>) new UnionFindMazeGenerator();
                default -> (BaseMazeAlgorithms<T>) new BFSMazeGenerator();
            };
            this.mazeGenerator.setMazeEntity(mazeEntity);

            // 3. 提交任务（父类会自动创建 originalData 快照）
            startAlgorithm(mazeGenerator, mazeEntity);
        }
    }

    @Override
    public void handleResetAction() {
        stopAlgorithm();

        // 逻辑：如果存在生成前的快照（即空白迷宫），则回滚到空白状态
        if (originalData != null) {
            BaseMaze<T> restored = (BaseMaze<T>) originalData.copy();
            updateCurrentDataReference(restored);
            visualizer.render(mazeEntity, null, null);
            updateUIComponents(0, 0);
        } else {
            // 无快照时，执行静默初始化（还原为全墙或全空）
            mazeEntity.initialSilent();
            visualizer.render(mazeEntity, null, null);
        }

        if (logArea != null) {
            logArea.clear();
            logArea.appendText("System: Maze reset to initial state.\n");
        }
    }

    // --- 业务逻辑处理 ---

    @SuppressWarnings("unchecked")
    @FXML
    public void handleGenerate() {
        stopAlgorithm();
        mazeEntity.initialSilent();

        // 根据选择器更新生成器算法
        int index = algoSelector.getSelectionModel().getSelectedIndex();
        this.mazeGenerator = switch (index) {
            case 1 -> (BaseMazeAlgorithms<T>) new DFSMazeGenerator();
            case 2 -> (BaseMazeAlgorithms<T>) new UnionFindMazeGenerator();
            default -> (BaseMazeAlgorithms<T>) new BFSMazeGenerator();
        };

        this.mazeGenerator.setMazeEntity(mazeEntity);
        // startAlgorithm 内部会自动调用 data.copy() 保存快照
        startAlgorithm(mazeGenerator, mazeEntity);
    }

    @SuppressWarnings("unchecked")
    @FXML
    public void handleSolve() {
        // 寻路属于特殊动作，不作为 handleStartAction，但仍需受控
        if (!mazeEntity.isGenerated()) {
            logArea.appendText("Warning: Please build maze first.\n");
            return;
        }

        stopAlgorithm();
        mazeEntity.clearVisualStates();
        mazeEntity.pickRandomPointsOnAvailablePaths();
        visualizer.render(mazeEntity, null, null);

        int index = solverSelector.getSelectionModel().getSelectedIndex();
        BaseMazeAlgorithms<T> solver = (index == 0)
                ? (BaseMazeAlgorithms<T>) new AStarMazePathfinder()
                : (BaseMazeAlgorithms<T>) new BFSMazePathfinder();

        solver.setMazeEntity(mazeEntity);
        // 执行寻路算法
        startAlgorithm(solver, mazeEntity);
    }

    @Override
    protected void executeAlgorithm(BaseAlgorithms<BaseMaze<T>> alg, BaseMaze<T> data) {
        if (alg instanceof BaseMazeAlgorithms) {
            // 第一阶段：执行生成
            ((BaseMazeAlgorithms<T>) alg).execute(data);

            // 第二阶段：生成结束后，自动无缝进入寻路
            if (mazeEntity.isGenerated()) {
                autoSwitchToSolver(data);
            }
        }
    }

    /**
     * 自动切换至寻路算法的私有逻辑
     */
    @SuppressWarnings("unchecked")
    private void autoSwitchToSolver(BaseMaze<T> data) {
        // 1. 准备工作：设置生成完成标志，清理视觉状态，选取随机点
        data.setGenerated(true);
        data.clearVisualStates();
        data.pickRandomPointsOnAvailablePaths();

        // 渲染一帧初始状态（起点/终点）
        AlgorithmThreadManager.postStatus(() -> {
            visualizer.render(data, null, null);
            logArea.appendText("System: Generation finished. Auto-starting solver...\n");
        });

        int solveIndex = solverSelector.getSelectionModel().getSelectedIndex();
        BaseMazeAlgorithms<T> solver = (solveIndex == 0)
                ? (BaseMazeAlgorithms<T>) new AStarMazePathfinder()
                : (BaseMazeAlgorithms<T>) new BFSMazePathfinder();

        solver.setMazeEntity(data);
        solver.execute(data);
    }

    @Override
    protected void updateCurrentDataReference(BaseMaze<T> restoredData) {
        this.mazeEntity = restoredData;
    }

    private void refreshMazeRealtime(int size) {
        int oddSize = (size % 2 == 0) ? size + 1 : size;
        stopAlgorithm();
        this.mazeEntity = (BaseMaze<T>) new ArrayMaze(oddSize, oddSize);
        this.mazeEntity.initialSilent();
        visualizer.render(mazeEntity, null, null);
        this.originalData = null; // 尺寸变了，清除旧快照
    }

    // --- 辅助与 I18N ---

    @Override
    protected void updateUIComponents(int compareCount, int actionCount) {
        if (statsLabel != null) {
            statsLabel.setText(String.format("VISITED: %d\nSCALE: %s",
                    actionCount, sizeValueLabel.getText()));
        }
    }

    @Override
    public List<Node> getCustomControls() {
        return (customControlPane != null) ? Collections.singletonList(customControlPane) : Collections.emptyList();
    }

    @Override
    protected void setupI18n() {
        densityLabel.textProperty().bind(I18N.createStringBinding("ctrl.maze.density"));
        genTitleLabel.textProperty().bind(I18N.createStringBinding("ctrl.maze.gen_title"));
        solveTitleLabel.textProperty().bind(I18N.createStringBinding("ctrl.maze.solve_title"));
        buildBtn.textProperty().bind(I18N.createStringBinding("btn.maze.build"));
        solveBtn.textProperty().bind(I18N.createStringBinding("btn.maze.solve"));
        setupComboBoxI18n();
    }

    private void setupComboBoxI18n() {
        List<String> genKeys = List.of("maze.algo.bfs", "maze.algo.dfs", "maze.algo.prim");
        algoSelector.itemsProperty().bind(Bindings.createObjectBinding(() -> {
            ObservableList<String> list = FXCollections.observableArrayList();
            for (String key : genKeys)
                list.add(I18N.getBundle().getString(key));
            return list;
        }, I18N.localeProperty()));

        List<String> solverKeys = List.of("maze.solver.astar", "maze.solver.dfs");
        solverSelector.itemsProperty().bind(Bindings.createObjectBinding(() -> {
            ObservableList<String> list = FXCollections.observableArrayList();
            for (String key : solverKeys)
                list.add(I18N.getBundle().getString(key));
            return list;
        }, I18N.localeProperty()));

        Platform.runLater(() -> {
            algoSelector.getSelectionModel().selectFirst();
            solverSelector.getSelectionModel().selectFirst();
        });
    }

    
}