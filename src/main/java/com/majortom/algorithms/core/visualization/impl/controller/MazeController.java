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
 * 职责：管理迷宫生成与寻路的策略切换。
 */
public class MazeController<T> extends BaseController<BaseMaze<T>> {

    // 算法引用不再需要双泛型，直接对齐 BaseMazeAlgorithms<T>
    private BaseMazeAlgorithms<T> mazeGenerator;
    private BaseMazeAlgorithms<T> mazeSolver;
    private BaseMaze<T> mazeEntity;

    private final BaseMazeVisualizer<BaseMaze<T>> mazeVisualizer;

    @FXML
    private Slider sizeSlider;

    @FXML
    private Label sizeValueLabel;

    @FXML
    private ComboBox<String> algoSelector;

    @FXML
    private ComboBox<String> solverSelector;

    @FXML
    private Label densityLabel; // 对应 FXML: fx:id="densityLabel"

    @FXML
    private Label genTitleLabel; // 对应 FXML: fx:id="genTitleLabel"

    @FXML
    private Label solveTitleLabel; // 对应 FXML: fx:id="solveTitleLabel"

    @FXML
    private Button buildBtn; // 对应 FXML: fx:id="buildBtn"

    @FXML
    private Button solveBtn; // 对应 FXML: fx:id="solveBtn"

    private Node customControlPane;

    public MazeController(BaseMaze<T> mazeEntity,
            BaseMazeAlgorithms<T> generator,
            BaseMazeVisualizer<BaseMaze<T>> visualizer) {
        super(visualizer);

        this.mazeEntity = mazeEntity;
        this.mazeGenerator = generator;
        this.mazeVisualizer = visualizer;
        loadFXMLControls();
        if (customControlPane != null) {
            setupI18n();
        }
    }

    private void loadFXMLControls() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MazeControls.fxml"));
            loader.setResources(ResourceBundle.getBundle("language.language"));
            loader.setController(this);
            this.customControlPane = loader.load();
        } catch (IOException e) {
            System.err.println("[Error] Maze FXML load failed. ");
            System.err.println("error:"+e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);

        // 监听滑块，实时更新 UI 上的尺寸显示
        if (sizeSlider != null) {
            sizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                int val = newVal.intValue();
                // 确保迷宫尺寸为奇数，这对某些生成算法（如 DFS/Prim）很重要
                int oddSize = (val % 2 == 0) ? val + 1 : val;
                sizeValueLabel.setText(oddSize + "x" + oddSize);
                if (!AlgorithmThreadManager.isRunning()) {
                    refreshMazeRealtime(newVal.intValue());
                }
            });
        }
        if (sizeSlider != null) {
            sizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            });
        }
        // 监听容器宽度，确保在窗口缩放时迷宫能自适应渲染
        this.visualizer.widthProperty().addListener((obs, oldV, newV) -> {
            if (newV.doubleValue() > 0 && mazeEntity != null) {
                this.visualizer.render(mazeEntity, null, null);
            }
        });

        // 初始静默初始化，不产生步进动画
        mazeEntity.initialSilent();

        if (buildBtn != null) {
            EffectUtils.applyDynamicEffect(buildBtn);
        }
        if (solveBtn != null) {
            EffectUtils.applyDynamicEffect(solveBtn);
        }
    }

    /**
     * 实时刷新迷宫（跳过动画，直接出结果）
     */
    private void refreshMazeRealtime(int size) {
        // 确保奇数
        int oddSize = (size % 2 == 0) ? size + 1 : size;
        stopAlgorithm();
        @SuppressWarnings("unchecked")
        BaseMaze<T> newMaze = (BaseMaze<T>) new ArrayMaze(oddSize, oddSize);
        this.mazeEntity = newMaze;
        this.mazeEntity.initialSilent();
        if (this.visualizer != null) {
            this.visualizer.render(mazeEntity, null, null);
        }
        resetSelectorsToDefault();
    }

    @FXML
    public void handleGenerate() {
        stopAlgorithm();
        mazeEntity.initialSilent();

        int index = algoSelector.getSelectionModel().getSelectedIndex();

        // 根据索引匹配算法（对应 genKeys 的顺序）
        switch (index) {
            case 0 -> this.mazeGenerator = (BaseMazeAlgorithms<T>) new BFSMazeGenerator();
            case 1 -> this.mazeGenerator = (BaseMazeAlgorithms<T>) new DFSMazeGenerator();
            case 2 -> this.mazeGenerator = (BaseMazeAlgorithms<T>) new UnionFindMazeGenerator();
            default -> this.mazeGenerator = (BaseMazeAlgorithms<T>) new BFSMazeGenerator();
        }

        if (this.mazeGenerator != null) {
            this.mazeGenerator.setMazeEntity(mazeEntity);
            startAlgorithm(mazeGenerator, mazeEntity);
        }
    }

    @FXML
    public void handleSolve() {
        stopAlgorithm();
        AlgorithmThreadManager.run(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }

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
    public void handleAlgorithmStart() {
        // 默认行为：点击开始按钮执行生成
        handleGenerate();
    }

    @Override
    protected void executeAlgorithm(BaseAlgorithms<BaseMaze<T>> alg, BaseMaze<T> data) {
        if (alg instanceof BaseMazeAlgorithms) {
            AlgorithmThreadManager.run(() -> {
                ((BaseMazeAlgorithms<T>) alg).execute(data);
            });
        }
    }

    @Override
    protected void updateUIComponents(int compareCount, int actionCount) {
        if (statsLabel != null) {
            // 实时展示访问过的节点数和当前迷宫规模
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
        // 基础标签绑定
        if (densityLabel != null)
            densityLabel.textProperty().bind(I18N.createStringBinding("ctrl.maze.density"));
        if (genTitleLabel != null)
            genTitleLabel.textProperty().bind(I18N.createStringBinding("ctrl.maze.gen_title"));
        if (solveTitleLabel != null)
            solveTitleLabel.textProperty().bind(I18N.createStringBinding("ctrl.maze.solve_title"));

        // 按钮绑定
        if (buildBtn != null)
            buildBtn.textProperty().bind(I18N.createStringBinding("btn.maze.build"));
        if (solveBtn != null)
            solveBtn.textProperty().bind(I18N.createStringBinding("btn.maze.solve"));
        setupComboBoxI18n();
    }

    private void setupComboBoxI18n() {
        // 绑定生成算法列表
        List<String> genKeys = List.of("maze.algo.bfs", "maze.algo.dfs", "maze.algo.prim");
        algoSelector.itemsProperty().bind(Bindings.createObjectBinding(() -> {
            ObservableList<String> list = FXCollections.observableArrayList();
            for (String key : genKeys) {
                list.add(I18N.getBundle().getString(key));
            }
            return list;
        }, I18N.localeProperty()));

        // 绑定寻路算法列表
        List<String> solverKeys = List.of("maze.solver.astar", "maze.solver.dfs");
        solverSelector.itemsProperty().bind(Bindings.createObjectBinding(() -> {
            ObservableList<String> list = FXCollections.observableArrayList();
            for (String key : solverKeys) {
                list.add(I18N.getBundle().getString(key));
            }
            return list;
        }, I18N.localeProperty()));

        // 初始化默认选中项
        Platform.runLater(() -> {
            if (!algoSelector.getItems().isEmpty())
                algoSelector.getSelectionModel().selectFirst();
            if (!solverSelector.getItems().isEmpty())
                solverSelector.getSelectionModel().selectFirst();
        });
    }

    private void resetSelectorsToDefault() {
        if (algoSelector != null && !algoSelector.getItems().isEmpty()) {
            algoSelector.getSelectionModel().select(0);
        }
        if (solverSelector != null && !solverSelector.getItems().isEmpty()) {
            solverSelector.getSelectionModel().select(0);
        }
    }
}