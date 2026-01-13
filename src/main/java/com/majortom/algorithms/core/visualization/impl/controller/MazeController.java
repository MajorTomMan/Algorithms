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
    private Label mazeDensityLabel; // 需在 FXML 增加 fx:id
    @FXML
    private Label mazeGenTitleLabel;
    @FXML
    private Label mazeSolveTitleLabel;
    @FXML
    private Button generateBtn; // 需在 FXML 增加 fx:id
    @FXML
    private Button solveBtn;
    @FXML
    private Button resetBtn;

    private Node customControlPane;

    public MazeController(BaseMaze<T> mazeEntity,
            BaseMazeAlgorithms<T> generator,
            BaseMazeVisualizer<BaseMaze<T>> visualizer) {
        super(visualizer);

        this.mazeEntity = mazeEntity;
        this.mazeGenerator = generator;
        this.mazeVisualizer = visualizer;
        loadFXMLControls();
    }

    private void loadFXMLControls() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MazeControls.fxml"));
            loader.setResources(ResourceBundle.getBundle("language.language"));
            loader.setController(this);
            this.customControlPane = loader.load();
        } catch (IOException e) {
            System.err.println("[Error] Maze FXML load failed.");
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
    }

    /**
     * 实时刷新迷宫（跳过动画，直接出结果）
     */
    private void refreshMazeRealtime(int size) {
        // 确保奇数
        int oddSize = (size % 2 == 0) ? size + 1 : size;

        // 停止并清理之前的任务
        stopAlgorithm();

        // 创建新迷宫并静默初始化（清空为全墙或全路）
        @SuppressWarnings("unchecked")
        BaseMaze<T> newMaze = (BaseMaze<T>) new ArrayMaze(oddSize, oddSize);
        this.mazeEntity = newMaze;
        this.mazeEntity.initialSilent();

        // 🚩 关键：立即渲染空白网格，实现“变大变小”的视觉反馈
        if (this.visualizer != null) {
            this.visualizer.render(mazeEntity, null, null);
        }
    }

    @FXML
    public void handleReset() {
        stopAlgorithm();

        int currentSize = Integer.parseInt(sizeValueLabel.getText().split("x")[0]);

        // 🚩 修正：显式强转。由于 T 通常是 int[][]，ArrayMaze 完美契合
        @SuppressWarnings("unchecked")
        BaseMaze<T> newMaze = (BaseMaze<T>) new ArrayMaze(currentSize, currentSize);
        this.mazeEntity = newMaze;
        this.mazeEntity.initialSilent();

        this.visualizer.render(mazeEntity, null, null);

        if (logArea != null) {
            logArea.appendText("System: Grid reset to " + currentSize + "\n");
        }
    }

    @FXML
    public void handleGenerate() {
        stopAlgorithm();
        mazeEntity.initialSilent(); // 生成前清空背景

        String selected = algoSelector.getValue();
        // 🚩 修正：实例化现在变得非常利落
        if ("Randomized BFS".equals(selected)) {
            this.mazeGenerator = (BaseMazeAlgorithms<T>) new BFSMazeGenerator();
        } else if ("Recursive Backtracker".equals(selected)) {
            this.mazeGenerator = (BaseMazeAlgorithms<T>) new DFSMazeGenerator();
        } else if ("Prim's Algorithm".equals(selected)) {
            this.mazeGenerator = (BaseMazeAlgorithms<T>) new UnionFindMazeGenerator();
        }

        if (this.mazeGenerator != null) {
            this.mazeGenerator.setMazeEntity(mazeEntity);
            startAlgorithm(mazeGenerator, mazeEntity);
        }
    }

    @FXML
    public void handleSolve() {
        // 🚩 1. 物理切断：这会触发 ThreadManager 内部 Worker 线程抛出 InterruptedException
        // 从而让生成算法的循环瞬间崩塌退出
        AlgorithmThreadManager.stopAll();

        // 🚩 2. 原地开始寻路
        // 此时生成算法留下的 mazeEntity 状态就是“当前状态”，我们直接在上面跑寻路
        if (mazeEntity != null) {
            // 清除临时的高亮（比如生成算法正在探索的绿色点），但不重置墙壁
            mazeEntity.clearVisualStates();

            // 寻路必须有起点和终点，直接在现有的“路”里随机挑两个点
            mazeEntity.pickRandomPointsOnAvailablePaths();

            // 获取寻路算法实例
            String selected = solverSelector.getValue();
            BaseMazeAlgorithms<T> solver = "A* Search".equals(selected)
                    ? (BaseMazeAlgorithms<T>) new AStarMazePathfinder()
                    : (BaseMazeAlgorithms<T>) new BFSMazePathfinder();

            AlgorithmThreadManager.run(() -> {
                solver.setMazeEntity(mazeEntity);
                solver.run(mazeEntity);
            });

            if (logArea != null) {
                logArea.appendText("System: Pathfinding force-started on current maze state.\n");
            }
        }
    }

    @Override
    public void handleAlgorithmStart() {
        // 默认行为：点击开始按钮执行生成
        handleGenerate();
    }

    @Override
    protected void executeAlgorithm(BaseAlgorithms<BaseMaze<T>> alg, BaseMaze<T> data) {
        // 🚩 修正：算法基类已统一 run(S data)，直接执行
        alg.run(data);
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
        if (mazeDensityLabel != null)
            mazeDensityLabel.textProperty().bind(I18N.createStringBinding("ctrl.maze.density"));
        if (mazeGenTitleLabel != null)
            mazeGenTitleLabel.textProperty().bind(I18N.createStringBinding("ctrl.maze.gen_title"));
        if (mazeSolveTitleLabel != null)
            mazeSolveTitleLabel.textProperty().bind(I18N.createStringBinding("ctrl.maze.solve_title"));
        if (generateBtn != null)
            generateBtn.textProperty().bind(I18N.createStringBinding("btn.maze.build"));
        if (solveBtn != null)
            solveBtn.textProperty().bind(I18N.createStringBinding("btn.maze.solve"));
        if (resetBtn != null)
            resetBtn.textProperty().bind(I18N.createStringBinding("btn.reset"));

        setupComboBoxI18n();
    }

    private void setupComboBoxI18n() {
        // 定义 Key 的列表
        List<String> genAlgos = List.of("maze.algo.bfs", "maze.algo.dfs", "maze.algo.prim");

        // 使用 StringBinding 转换整个列表
        algoSelector.itemsProperty().bind(Bindings.createObjectBinding(() -> {
            ObservableList<String> localizedNames = FXCollections.observableArrayList();
            for (String key : genAlgos) {
                localizedNames.add(I18N.getBundle().getString(key));
            }
            return localizedNames;
        }, I18N.localeProperty()));

        // 默认选择第一个
        algoSelector.getSelectionModel().selectFirst();
    }
}