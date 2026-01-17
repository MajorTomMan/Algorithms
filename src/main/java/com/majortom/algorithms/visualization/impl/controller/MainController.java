package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.graph.algorithms.BFSAlgorithms;
import com.majortom.algorithms.core.graph.impl.DirectedGraph;
import com.majortom.algorithms.core.maze.algorithms.generate.BFSMazeGenerator;
import com.majortom.algorithms.core.maze.impl.ArrayMaze;
import com.majortom.algorithms.core.sort.alg.InsertionSort;
import com.majortom.algorithms.core.tree.BaseTree;
import com.majortom.algorithms.core.tree.alg.AVLTreeAlgorithms;
import com.majortom.algorithms.core.tree.impl.AVLTreeEntity;
import com.majortom.algorithms.utils.AlgorithmsUtils;
import com.majortom.algorithms.utils.EffectUtils;
import com.majortom.algorithms.visualization.BaseController;
import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.impl.visualizer.HistogramSortVisualizer;
import com.majortom.algorithms.visualization.impl.visualizer.SquareMazeVisualizer;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.manager.AlgorithmThreadManager;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * 主界面控制器
 * 职责：负责全局容器管理、模块切换、语言切换以及顶层控制逻辑。
 */
public class MainController implements Initializable {

    @FXML
    private StackPane visualizationContainer;
    @FXML
    private HBox customControlBox;
    @FXML
    private Label menuTitleLabel, statsTitleLabel, logTitleLabel, statsLabel, delayLabel;
    @FXML
    private TextArea logArea;
    @FXML
    private Button sortBtn, mazeBtn, treeBtn, graphBtn;
    @FXML
    private Button startBtn, pauseBtn, resetBtn, langBtn;
    @FXML
    private Slider delaySlider;

    private BaseController<?> currentSubController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. 初始化国际化上下文
        if (resources != null) {
            I18N.setLocale(resources.getLocale());
        }

        // 2. 绑定 UI 文本与特效
        setupI18n();
        setupGlobalActions();
        EffectUtils.applyDynamicEffect(sortBtn, mazeBtn, treeBtn, graphBtn, startBtn, resetBtn, pauseBtn, langBtn);

        // 3. 默认启动排序模块
        switchToSortModule();

        appendSystemLog("Lab Initialized.");
    }

    /**
     * 设置全局国际化绑定
     */
    private void setupI18n() {
        menuTitleLabel.textProperty().bind(I18N.createStringBinding("menu.lab"));
        statsTitleLabel.textProperty().bind(I18N.createStringBinding("side.stats"));
        logTitleLabel.textProperty().bind(I18N.createStringBinding("side.log"));
        sortBtn.textProperty().bind(I18N.createStringBinding("menu.sort"));
        mazeBtn.textProperty().bind(I18N.createStringBinding("menu.maze"));
        treeBtn.textProperty().bind(I18N.createStringBinding("menu.tree"));
        graphBtn.textProperty().bind(I18N.createStringBinding("menu.graph"));
        startBtn.textProperty().bind(I18N.createStringBinding("btn.start"));
        resetBtn.textProperty().bind(I18N.createStringBinding("btn.reset"));
        delayLabel.textProperty().bind(I18N.createStringBinding("bottom.delay"));

        if (pauseBtn != null) {
            pauseBtn.textProperty().bind(Bindings.createStringBinding(() -> {
                boolean paused = AlgorithmThreadManager.isPaused();
                return I18N.getBundle().getString(paused ? "btn.resume" : "btn.pause");
            }, I18N.localeProperty()));
        }
    }

    /**
     * 设置全局操作按钮行为
     */
    private void setupGlobalActions() {
        startBtn.setOnAction(e -> {
            if (currentSubController != null)
                currentSubController.handleAlgorithmStart();
        });

        pauseBtn.setOnAction(e -> {
            if (currentSubController != null) {
                currentSubController.togglePause();
                appendSystemLog(AlgorithmThreadManager.isPaused() ? "Command: PAUSED" : "Command: RESUMED");
            }
        });

        resetBtn.setOnAction(e -> {
            if (currentSubController instanceof BaseModuleController) {
                // 利用重构后的 resetModule 清理子模块内部状态
                ((BaseModuleController<?>) currentSubController).resetModule();
                appendSystemLog("Module reset.");
            }
        });
    }

    /**
     * 核心加载逻辑：注入容器并挂载自定义面板
     */
    private void loadSubController(BaseController<?> newController) {
        // 1. 停止当前运行的所有任务
        AlgorithmThreadManager.stopAll();

        // 2. 清理 UI 容器
        visualizationContainer.getChildren().clear();
        customControlBox.getChildren().clear();

        // 3. 建立引用与绑定
        newController.setUIReferences(statsLabel, logArea, delaySlider, customControlBox, startBtn, pauseBtn, resetBtn);
        this.currentSubController = newController;

        // 4. 挂载可视化画布
        BaseVisualizer<?> viz = newController.getVisualizer();
        if (viz != null) {
            viz.prefWidthProperty().bind(visualizationContainer.widthProperty());
            viz.prefHeightProperty().bind(visualizationContainer.heightProperty());
            visualizationContainer.getChildren().add(viz);
        }

        // 5. 触发自定义控件挂载
        newController.setupCustomControls(customControlBox);
    }

    @FXML
    public void switchToSortModule() {
        InsertionSort<Integer> sort = new InsertionSort<>();
        HistogramSortVisualizer<Integer> visualizer = new HistogramSortVisualizer<>();
        loadSubController(new SortController<>(sort, visualizer));
    }

    @FXML
    public void switchToMazeModule() {
        ArrayMaze maze = new ArrayMaze(51, 51);
        BFSMazeGenerator gen = new BFSMazeGenerator();
        gen.setMazeEntity(maze);
        loadSubController(new MazeController<>(maze, gen, new SquareMazeVisualizer()));
    }

    @FXML
    public void switchToTreeModule() {
        BaseTree<Integer> tree = new AVLTreeEntity<>();
        AVLTreeAlgorithms<Integer> algorithms = new AVLTreeAlgorithms<>();
        // 初始数据填充
        Integer[] array = AlgorithmsUtils.randomArray(15, 100);
        for (Integer i : array) {
            algorithms.put(tree, i);
        }

        loadSubController(new TreeController<>(tree, algorithms));
    }

    @FXML
    public void switchToGraphModule() {
        DirectedGraph<Integer> graph = new DirectedGraph<>("A");
        AlgorithmsUtils.buildRandomGraph(graph, 10, 15, true);
        loadSubController(new GraphController<>(new BFSAlgorithms<>(), graph));
    }

    @FXML
    private void toggleLanguage() {
        Locale newLocale = (I18N.getLocale().getLanguage().equals("zh")) ? Locale.ENGLISH : Locale.CHINESE;
        I18N.setLocale(newLocale);
        appendSystemLog(newLocale == Locale.CHINESE ? "系统：语言已切换" : "System: Language switched");
    }

    private void appendSystemLog(String msg) {
        if (logArea != null)
            logArea.appendText("System: " + msg + "\n");
    }
}