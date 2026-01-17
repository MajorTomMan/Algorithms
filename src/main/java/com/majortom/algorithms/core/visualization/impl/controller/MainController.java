package com.majortom.algorithms.core.visualization.impl.controller;

import com.majortom.algorithms.core.visualization.BaseController;
import com.majortom.algorithms.core.visualization.BaseVisualizer;
import com.majortom.algorithms.core.visualization.impl.visualizer.HistogramSortVisualizer;
import com.majortom.algorithms.core.visualization.impl.visualizer.SquareMazeVisualizer;
import com.majortom.algorithms.core.visualization.international.I18N;
import com.majortom.algorithms.core.visualization.manager.AlgorithmThreadManager;
import com.majortom.algorithms.utils.AlgorithmsUtils;
import com.majortom.algorithms.core.graph.algorithms.BFSAlgorithms;
import com.majortom.algorithms.core.graph.impl.DirectedGraph;
import com.majortom.algorithms.core.maze.algorithms.generate.BFSMazeGenerator;
import com.majortom.algorithms.core.maze.impl.ArrayMaze;
import com.majortom.algorithms.core.sort.alg.InsertionSort;
import com.majortom.algorithms.core.tree.BaseTree;
import com.majortom.algorithms.core.tree.alg.AVLTreeAlgorithms;
import com.majortom.algorithms.core.tree.impl.AVLTreeEntity;
import com.majortom.algorithms.utils.EffectUtils;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * 主界面控制器
 * 职责：负责全局导航、模块切换、以及将底部控制栏指令分发给当前子模块控制器。
 */
public class MainController implements Initializable {

    @FXML
    private StackPane visualizationContainer;
    @FXML
    private HBox customControlBox;
    @FXML
    private Label menuTitleLabel, statsTitleLabel, logTitleLabel, statsLabel;
    @FXML
    private TextArea logArea;
    @FXML
    private Button sortBtn, mazeBtn, treeBtn, graphBtn;
    @FXML
    private Slider delaySlider;
    @FXML
    private Label delayLabel;
    @FXML
    private Button startBtn, pauseBtn, resetBtn, langBtn;

    private BaseController<?> currentSubController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (resources != null) {
            I18N.setLocale(resources.getLocale());
        }

        setupI18n();
        setupGlobalActions();
        applyEffectsToAllButtons();

        // 默认进入排序模块
        switchToSortModule();
        logArea.appendText("System: Lab Initialized.\n");
    }

    /**
     * 设置全局控制按钮的行为
     * 职责：仅作为指令分发者，具体的算法启停逻辑由子控制器实现
     */
    private void setupGlobalActions() {
        // 1. 开始动作
        startBtn.setOnAction(e -> {
            if (currentSubController != null) {
                currentSubController.handleStartAction();
            }
        });

        // 2. 暂停/恢复动作
        pauseBtn.setOnAction(e -> {
            if (currentSubController != null) {
                currentSubController.handlePauseAction();
                // 日志记录指令
                String cmd = AlgorithmThreadManager.isPaused() ? "Command: PAUSED" : "Command: RESUMED";
                logArea.appendText(cmd + "\n");
            }
        });

        // 3. 重置动作
        resetBtn.setOnAction(e -> {
            if (currentSubController != null) {
                // 调用子控制器实现的具体清理和快照恢复逻辑
                currentSubController.handleResetAction();

                logArea.clear();
                logArea.appendText("System: Module reset.\n");
            }
        });
    }

    /**
     * 加载子模块控制器
     */
    private void loadSubController(BaseController<?> newController) {
        // 切换前停止所有正在运行的算法线程
        AlgorithmThreadManager.stopAll();

        visualizationContainer.getChildren().clear();
        customControlBox.getChildren().clear();

        // 注入全局 UI 引用
        newController.setUIReferences(statsLabel, logArea, delaySlider);

        // 装载可视化画布
        BaseVisualizer<?> viz = newController.getVisualizer();
        if (viz != null) {
            viz.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            viz.setMinSize(0, 0);
            visualizationContainer.getChildren().add(viz);
            viz.prefWidthProperty().bind(visualizationContainer.widthProperty());
            viz.prefHeightProperty().bind(visualizationContainer.heightProperty());
        }

        // 装载子模块特有的控制项
        List<Node> subControls = newController.getCustomControls();
        if (subControls != null) {
            customControlBox.getChildren().addAll(subControls);
        }

        this.currentSubController = newController;
    }

    // --- 模块切换逻辑 ---

    @FXML
    public void switchToSortModule() {
        // QuickSort<Integer> algorithms = new QuickSort<>();
        InsertionSort<Integer> algorithms = new InsertionSort<>();
        HistogramSortVisualizer<Integer> visualizer = new HistogramSortVisualizer<>();
        SortController<Integer> sortController = new SortController<Integer>(algorithms,
                visualizer);
        loadSubController(sortController);
    }

    @FXML
    public void switchToMazeModule() {
        ArrayMaze maze = new ArrayMaze(51, 51);
        loadSubController(new MazeController<>(maze, new BFSMazeGenerator(), new SquareMazeVisualizer()));
    }

    @FXML
    public void switchToTreeModule() {
        BaseTree<Integer> tree = new AVLTreeEntity<>();
        AVLTreeAlgorithms<Integer> algorithms = new AVLTreeAlgorithms<>();
        Integer[] array = AlgorithmsUtils.randomArray(23, 100);
        for (Integer i : array)
            algorithms.put(tree, i);
        loadSubController(new TreeController<>(tree, algorithms));
    }

    @FXML
    public void switchToGraphModule() {
        DirectedGraph<Integer> graph = new DirectedGraph<>("A");
        AlgorithmsUtils.buildRandomGraph(graph, 15, 20, true);
        loadSubController(new GraphController<>(new BFSAlgorithms<>(), graph));
    }

    // --- 辅助逻辑 ---

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
                ResourceBundle bundle = I18N.getBundle();
                boolean paused = AlgorithmThreadManager.isPaused();
                return bundle.getString(paused ? "btn.resume" : "btn.pause");
            }, I18N.localeProperty()));
        }
    }

    private void applyEffectsToAllButtons() {
        EffectUtils.applyDynamicEffect(sortBtn);
        EffectUtils.applyDynamicEffect(mazeBtn);
        EffectUtils.applyDynamicEffect(treeBtn);
        EffectUtils.applyDynamicEffect(graphBtn);
        EffectUtils.applyDynamicEffect(startBtn);
        EffectUtils.applyDynamicEffect(resetBtn);
        if (pauseBtn != null)
            EffectUtils.applyDynamicEffect(pauseBtn);
        if (langBtn != null)
            EffectUtils.applyDynamicEffect(langBtn);
    }

    @FXML
    private void toggleLanguage() {
        Locale newLocale = (I18N.getLocale().getLanguage().equals("zh")) ? Locale.ENGLISH : Locale.CHINESE;
        I18N.setLocale(newLocale);
        if (logArea != null) {
            String msg = (newLocale == Locale.CHINESE) ? "系统：语言已切换" : "System: Language switched";
            logArea.appendText(msg + "\n");
        }
    }
}