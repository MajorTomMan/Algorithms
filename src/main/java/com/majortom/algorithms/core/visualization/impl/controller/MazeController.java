package com.majortom.algorithms.core.visualization.impl.controller;

import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.BaseMazeAlgorithms;
import com.majortom.algorithms.core.maze.algorithms.generate.*;
import com.majortom.algorithms.core.maze.algorithms.pathfinding.*;
import com.majortom.algorithms.core.maze.impl.ArrayMaze;
import com.majortom.algorithms.core.visualization.BaseController;
import com.majortom.algorithms.core.visualization.base.BaseMazeVisualizer;
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
 * 迷宫算法控制器 (重构版)
 * 职责：管理迷宫生成与寻路的策略切换。
 * 🚩 修正：泛型对齐为 BaseMaze<T>，确保符合 BaseStructure 约束
 */
public class MazeController<T> extends BaseController<BaseMaze<T>> {

    private BaseMazeAlgorithms<T, BaseMaze<T>> mazeGenerator;
    private BaseMazeAlgorithms<T, BaseMaze<T>> mazeSolver;
    private BaseMaze<T> mazeEntity;

    // 🚩 修正：视觉组件也需对齐泛型
    private final BaseMazeVisualizer<T> mazeVisualizer;

    @FXML
    private Slider sizeSlider;
    @FXML
    private Label sizeValueLabel;
    @FXML
    private ComboBox<String> algoSelector;
    @FXML
    private ComboBox<String> solverSelector;

    private Node customControlPane;

    public MazeController(BaseMazeAlgorithms<T> generator,
            BaseMaze<T> mazeEntity,
            BaseMazeVisualizer<T> visualizer) {
        // 🚩 修正：super 调用，第一个参数不需要传 null，BaseController 已经重构
        super(visualizer);
        this.mazeGenerator = generator;
        this.mazeEntity = mazeEntity;
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

        if (sizeSlider != null) {
            sizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                int val = newVal.intValue();
                int oddSize = (val % 2 == 0) ? val + 1 : val;
                sizeValueLabel.setText(oddSize + "x" + oddSize);
            });
        }

        // 🚩 修正：使用 getLastData() 保证一致性
        this.visualizer.widthProperty().addListener((obs, oldV, newV) -> {
            if (newV.doubleValue() > 0 && mazeEntity != null) {
                this.visualizer.render(mazeEntity, null, null);
            }
        });

        mazeEntity.initialSilent();
    }

    @FXML
    public void handleReset() {
        stopAlgorithm();

        int currentSize = Integer.parseInt(sizeValueLabel.getText().split("x")[0]);
        // 🚩 修正：确保新生成的实体也被正确引用
        this.mazeEntity = (BaseMaze<T>) new ArrayMaze(currentSize, currentSize);
        mazeEntity.initialSilent();

        this.visualizer.render(mazeEntity, null, null);

        if (logArea != null) {
            logArea.appendText("System: Grid reset to " + currentSize + "\n");
        }
    }

    @FXML
    public void handleGenerate() {
        stopAlgorithm();
        mazeEntity.initialSilent();

        String selected = algoSelector.getValue();
        if ("Randomized BFS".equals(selected)) {
            this.mazeGenerator = new BFSMazeGenerator();
        } else if ("Recursive Backtracker".equals(selected)) {
            this.mazeGenerator = new DFSMazeGenerator();
        } else if ("Prim's Algorithm".equals(selected)) {
            this.mazeGenerator = new UnionFindMazeGenerator();
        }

        this.mazeGenerator.setMazeEntity(mazeEntity);

        // 🚩 修正：传入实体对象 mazeEntity
        startAlgorithm(mazeGenerator, mazeEntity);
    }

    @FXML
    public void handleSolve() {
        stopAlgorithm();
        mazeEntity.pickRandomPoints();

        String selected = solverSelector.getValue();
        if ("A* Search".equals(selected)) {
            this.mazeSolver = new AStarMazePathfinder();
        } else if ("DFS Solver".equals(selected)) {
            this.mazeSolver = new DFSMazePathfinder();
        } else {
            this.mazeSolver = new BFSMazePathfinder();
        }

        this.mazeSolver.setMazeEntity(mazeEntity);

        // 🚩 修正：传入实体对象 mazeEntity
        startAlgorithm(mazeSolver, mazeEntity);
    }

    @Override
    public void handleAlgorithmStart() {
        handleGenerate();
    }

    @Override
    protected void executeAlgorithm(BaseAlgorithms<BaseMaze<T>> alg, BaseMaze<T> data) {
        // 🚩 修正：现在数据是实体，算法运行直接调用即可
        alg.run(data);
    }

    @Override
    protected void updateUIComponents(int compareCount, int actionCount) {
        if (statsLabel != null) {
            // 🚩 解决 Bound Property 报错：确保 statsLabel 没有在 FXML 被绑定
            statsLabel.setText(String.format("VISITED: %d\nSCALE: %s",
                    actionCount, sizeValueLabel.getText()));
        }
    }

    @Override
    public List<Node> getCustomControls() {
        return (customControlPane != null) ? Collections.singletonList(customControlPane) : Collections.emptyList();
    }
}