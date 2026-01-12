package com.majortom.algorithms.core.visualization.main;

import com.majortom.algorithms.core.visualization.BaseController;
import com.majortom.algorithms.core.visualization.BaseVisualizer;
import com.majortom.algorithms.core.visualization.impl.visualizer.HistogramSortVisualizer;
import com.majortom.algorithms.core.visualization.impl.visualizer.SquareMazeVisualizer;
import com.majortom.algorithms.core.visualization.impl.window.MazeController;
import com.majortom.algorithms.core.visualization.impl.window.SortController;
import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.algorithms.generate.BFSMazeGenerator;
import com.majortom.algorithms.core.maze.impl.ArrayMaze;
import com.majortom.algorithms.core.sort.BaseSortAlgorithms;
import com.majortom.algorithms.core.sort.impl.QuickSort;
import com.majortom.algorithms.core.sort.BaseSort;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.util.List;

/**
 * 主界面控制器
 * 职责：负责模块导航切换、子控制器生命周期管理、全局UI组件状态分发以及后台线程调度。
 */
public class MainController {

    // --- FXML UI 注入 ---

    @FXML
    private StackPane visualizationContainer; // 对应 FXML 中心区域，用于承载 Canvas 渲染器

    @FXML
    private HBox customControlBox; // 对应 FBox 底部右侧，用于承载算法特有的控制组件

    @FXML
    private Label statsLabel; // 侧边栏数据统计展示文本

    @FXML
    private TextArea logArea; // 侧边栏系统运行日志文本域

    @FXML
    private Slider delaySlider; // 全局动画延迟步进控制器

    @FXML
    private Button startBtn; // 全局启动指令触发器

    @FXML
    private Button pauseBtn; // 全局暂停指令触发器

    @FXML
    private Button resetBtn; // 全局重置指令触发器

    /** 当前处于激活状态的子业务控制器引用 */
    private BaseController<?> currentSubController;

    /**
     * JavaFX 初始化回调，设置全局动作监听并加载默认模块
     */
    @FXML
    public void initialize() {
        logArea.appendText("System: Initializing Laboratory Environment...\n");

        // 绑定全局控制按钮的逻辑调度
        setupGlobalActions();

        // 初始加载排序模块
        switchToSortModule();
    }

    /**
     * 配置底部全局控制栏的事件处理器
     */
    private void setupGlobalActions() {
        // 启动逻辑：调用当前子控制器的算法执行入口
        startBtn.setOnAction(e -> {
            if (currentSubController != null) {
                currentSubController.handleAlgorithmStart();
                logArea.appendText("Command: START_SIGNAL_DISPATCHED\n");
            }
        });

        // 暂停逻辑：预留信号量控制接口
        pauseBtn.setOnAction(e -> {
            logArea.appendText("Command: PAUSE_SIGNAL_DISPATCHED\n");
            // TODO: 实现 BaseController 的暂停/恢复线程同步
        });

        // 重置逻辑：清理当前模块状态
        resetBtn.setOnAction(e -> handleGlobalReset());
    }

    // --- 导航模块切换逻辑 ---

    /**
     * 切换至排序算法模块
     */
    @FXML
    public void switchToSortModule() {
        // 显式指定泛型类型以辅助编译器进行类型安全检查
        BaseSortAlgorithms<Integer> quickSort = new QuickSort<>();
        BaseVisualizer<BaseSort<Integer>> visualizer = new HistogramSortVisualizer<>();

        SortController<Integer> sortCtrl = new SortController<>(quickSort, visualizer);

        loadSubController(sortCtrl);
        logArea.appendText("Module: Sorting module loaded successfully.\n");
    }

    /**
     * 切换至迷宫生成模块
     */
    @FXML
    public void switchToMazeModule() {
        // 初始化 51x51 规模的数组迷宫实体
        BaseMaze<int[][]> mazeEntity = new ArrayMaze(51, 51);

        MazeController<int[][]> mazeCtrl = new MazeController<>(
                new BFSMazeGenerator(),
                mazeEntity,
                new SquareMazeVisualizer());

        loadSubController(mazeCtrl);
        logArea.appendText("Module: Maze generation module loaded successfully.\n");
    }

    // --- 控制器生命周期管理 ---

    /**
     * 执行子控制器的动态加载与环境注入
     * 
     * @param newController 待加载的 BaseController 实现类
     */
    private void loadSubController(BaseController<?> newController) {
        // 1. 释放上一个控制器的资源，停止其后台计算线程
        if (currentSubController != null) {
            currentSubController.stopAlgorithm();
        }

        // 2. 清理 FXML 容器中的旧组件
        visualizationContainer.getChildren().clear();
        customControlBox.getChildren().clear();

        // 3. 注入全局 UI 组件引用，以便子控制器更新实时状态
        newController.setUIReferences(statsLabel, logArea);

        // 4. 将主界面的 Slider 数值与子控制器的延迟属性进行双向绑定/单向传递
        if (delaySlider != null) {
            newController.delayMsProperty().bind(delaySlider.valueProperty());
        }

        // 5. 调用子控制器的初始化方法（执行 FXML 加载与内部逻辑绑定）
        newController.initialize(null, null);

        BaseVisualizer<?> viz = newController.getVisualizer();
        if (viz != null) {
            visualizationContainer.getChildren().add(viz);

            visualizationContainer.widthProperty().addListener((obs, oldVal, newVal) -> {
                // Region 必须使用 setPrefWidth，因为 setWidth 是受保护的
                viz.setPrefWidth(newVal.doubleValue());
                viz.drawCurrent();
            });

            visualizationContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
                viz.setPrefHeight(newVal.doubleValue());
                viz.drawCurrent();
            });
        }

        // 7. 获取并挂载子模块特有的操作按钮（如“随机生成”等）
        List<Node> subControls = newController.getCustomControls();
        if (subControls != null) {
            customControlBox.getChildren().addAll(subControls);
        }

        // 8. 更新当前控制器上下文
        this.currentSubController = newController;
    }

    /**
     * 执行全局清空与重置
     */
    @FXML
    private void handleGlobalReset() {
        if (currentSubController != null) {
            currentSubController.stopAlgorithm();
            // 清理画布像素数据
            currentSubController.getVisualizer().clear();
        }
        logArea.clear();
        logArea.appendText("System: Workspace reset performed.\n");
    }
}