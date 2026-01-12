package com.majortom.algorithms.core.visualization.impl.window;

import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.maze.BaseMaze;
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

/**
 * 抽象迷宫算法控制器
 * 职责：适配任何数据模型 T 的迷宫，协调实体与算法的生命周期。
 * 
 * @param <T> 迷宫数据模型 (如 int[][], Graph, HexGrid 等)
 */
public class MazeController<T> extends BaseController<T> {

    // 算法执行者
    private final BaseAlgorithms<T> mazeAlgorithm;
    // 数据维护者
    private final BaseMaze<T> mazeEntity;

    private Node customControlPane;

    // --- 注入主界面的 UI 引用 ---
    private Label sideStatsLabel;
    private TextArea sideLogArea;

    @FXML
    private Slider sizeSlider;

    private final BaseMazeVisualizer<T> mazeVisualizer; // 外部传入的视觉实现

    /**
     * 外部注入：算法、实体、以及对应的可视化器
     */
    public MazeController(BaseAlgorithms<T> algorithm,
            BaseMaze<T> mazeEntity,
            BaseMazeVisualizer<T> visualizer) {
        this.mazeAlgorithm = algorithm;
        this.mazeEntity = mazeEntity;
        this.mazeVisualizer = visualizer; // 这里持有了具体的实现（如 Square 或 Hex）
        loadFXMLControls();
    }

    private void loadFXMLControls() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MazeControls.fxml"));
            loader.setController(this);
            this.customControlPane = loader.load();
        } catch (IOException e) {
            System.err.println("[Error] FXML 加载失败，请检查路径: " + e.getMessage());
        }
    }

    public void setUIReferences(Label statsLabel, TextArea logArea) {
        this.sideStatsLabel = statsLabel;
        this.sideLogArea = logArea;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 关键：直接将注入的实现赋值给基类的 visualizer 引用
        this.visualizer = this.mazeVisualizer;

        // 确保 UI 样式统一
        if (this.visualizer != null) {
            this.visualizer.setStyle("-fx-background-color: #0A0A0E;");
        }
    }

    @Override
    public List<Node> getCustomControls() {
        return Collections.singletonList(customControlPane);
    }

    // --- 交互入口 ---

    @FXML
    private void handleInitiate() {
        // 1. 获取 UI 配置（这里可以根据需要扩展 sizeSlider 的用途）
        int size = (int) sizeSlider.getValue();

        // 2. 调用 BaseMaze 的初始化。
        // 由于 BaseMaze 内部持有 T data，初始化后数据就准备好了。
        mazeEntity.initial();

        if (sideLogArea != null) {
            sideLogArea.appendText("System: Initializing specialized maze structure...\n");
        }

        // 3. 启动算法：执行者是 mazeAlgorithm，操作对象是 mazeEntity 的 T data
        startAlgorithm(mazeAlgorithm, mazeEntity.getData());
    }

    /**
     * 实现基类的抽象方法
     */
    @Override
    protected void executeAlgorithm(BaseAlgorithms<T> alg, T data) {
        // 完美对齐：由于 alg 是 BaseAlgorithms<T>，data 是 T，直接 run 即可。
        // 算法内部会调用 mazeEntity.setCellState(...)，从而触发 sync() 导致 Visualizer 重绘。
        alg.run(data);
    }

    @Override
    protected void updateUIComponents(int compareCount, int actionCount) {
        if (sideStatsLabel != null) {
            Platform.runLater(() -> sideStatsLabel.setText(
                    String.format("Maze Steps: %d\nSize Level: %d",
                            actionCount, (int) sizeSlider.getValue())));
        }
    }

    @Override
    protected void onAlgorithmFinished() {
        if (sideLogArea != null) {
            Platform.runLater(() -> sideLogArea.appendText("System: Maze generation complete.\n"));
        }
    }

    @Override
    public void handleAlgorithmStart() {
        // TODO Auto-generated method stub
        if (mazeAlgorithm != null && mazeEntity != null) {
            startAlgorithm(mazeAlgorithm, mazeEntity.getData());
        }

    }
}