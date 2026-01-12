package com.majortom.algorithms.core.visualization.impl.controller;

import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.BaseMazeAlgorithms;
import com.majortom.algorithms.core.maze.algorithms.generate.*;
import com.majortom.algorithms.core.maze.algorithms.pathfinding.*;
import com.majortom.algorithms.core.maze.impl.ArrayMaze;
import com.majortom.algorithms.core.visualization.BaseController;
import com.majortom.algorithms.core.visualization.base.BaseMazeVisualizer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

public class MazeController<T> extends BaseController<T> {

    private BaseMazeAlgorithms<T> mazeGenerator;
    private BaseMazeAlgorithms<T> mazeSolver;
    private BaseMaze<T> mazeEntity;
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
        this.mazeGenerator = generator;
        this.mazeEntity = mazeEntity;
        this.mazeVisualizer = visualizer;
        loadFXMLControls();
    }

    private void loadFXMLControls() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MazeControls.fxml"));
            loader.setController(this);
            this.customControlPane = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.visualizer = this.mazeVisualizer;

        // 1. 滑动条联动
        if (sizeSlider != null) {
            sizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                int val = newVal.intValue();
                int oddSize = (val % 2 == 0) ? val + 1 : val;
                sizeValueLabel.setText(oddSize + "x" + oddSize);
            });
        }

        // 2. 解决黑屏：监听 Canvas 尺寸，就绪后立即渲染首帧
        this.visualizer.widthProperty().addListener((obs, oldV, newV) -> {
            if (newV.doubleValue() > 0 && mazeEntity != null) {
                Platform.runLater(() -> this.visualizer.render(mazeEntity.getData(), null, null));
            }
        });

        mazeEntity.initialSilent();
        syncArchitecture();
    }

    private void syncArchitecture() {
        if (mazeGenerator != null && mazeEntity != null) {
            mazeGenerator.setEnvironment(this::onSync, this::onStep);
            mazeGenerator.setMazeEntity(mazeEntity);
        }
        if (mazeSolver != null) {
            mazeSolver.setEnvironment(this::onSync, this::onStep);
            mazeSolver.setMazeEntity(mazeEntity);
        }
    }

    @FXML
    public void handleReset() {
        stopAlgorithm();
        // 根据当前 Slider 值重新生成迷宫矩阵
        int currentSize = Integer.parseInt(sizeValueLabel.getText().split("x")[0]);
        this.mazeEntity = (BaseMaze<T>) new ArrayMaze(currentSize, currentSize);

        mazeEntity.initialSilent();
        syncArchitecture();
        this.visualizer.render(mazeEntity.getData(), null, null);
        if (logArea != null)
            logArea.appendText("System: Grid reset to " + currentSize + "\n");
    }

    @FXML
    public void handleGenerate() {
        stopAlgorithm();
        mazeEntity.initialSilent();

        // 策略路由
        String selected = algoSelector.getValue();
        if ("Randomized BFS".equals(selected)) {
            this.mazeGenerator = (BaseMazeAlgorithms<T>) new BFSMazeGenerator();
        } else if ("Recursive Backtracker".equals(selected)) {
            this.mazeGenerator = (BaseMazeAlgorithms<T>) new DFSMazeGenerator();
        } else if ("Prim's Algorithm".equals(selected)) {
            this.mazeGenerator = (BaseMazeAlgorithms<T>) new UnionFindMazeGenerator();
        }

        syncArchitecture();
        if (logArea != null)
            logArea.appendText("System: Generating [" + selected + "]\n");
        startAlgorithm(mazeGenerator, mazeEntity.getData());
    }

    @FXML
    public void handleSolve() {
        stopAlgorithm();
        mazeEntity.pickRandomPoints(); // 放置起点终点

        String selected = solverSelector.getValue();
        if ("A* Search".equals(selected)) {
            this.mazeSolver = (BaseMazeAlgorithms<T>) new AStarMazePathfinder();
        } else if ("DFS Solver".equals(selected)) {
            this.mazeSolver = (BaseMazeAlgorithms<T>) new DFSMazePathfinder();
        } else {
            this.mazeSolver = (BaseMazeAlgorithms<T>) new BFSMazePathfinder();
        }

        syncArchitecture();
        startAlgorithm(mazeSolver, mazeEntity.getData());
    }

    @Override
    public void handleAlgorithmStart() {
        handleGenerate();
    }

    @Override
    protected void executeAlgorithm(BaseAlgorithms<T> alg, T data) {
        alg.run(data);
    }

    @Override
    protected void updateUIComponents(int compareCount, int actionCount) {
        if (statsLabel != null) {
            Platform.runLater(() -> statsLabel.setText(
                    String.format("VISITED: %d\nOPS: %d", actionCount, compareCount)));
        }
    }

    @Override
    public List<Node> getCustomControls() {
        return (customControlPane != null) ? Collections.singletonList(customControlPane) : Collections.emptyList();
    }
}