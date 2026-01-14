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
import com.majortom.algorithms.core.graph.impl.UndirectedGraph;
import com.majortom.algorithms.core.maze.algorithms.generate.BFSMazeGenerator;
import com.majortom.algorithms.core.maze.impl.ArrayMaze;
import com.majortom.algorithms.core.sort.impl.QuickSort;
import com.majortom.algorithms.core.tree.BaseTree;
import com.majortom.algorithms.core.tree.impl.AVLTree;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
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
 * 职责：负责模块导航切换、子控制器生命周期管理、全局UI组件分发。
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
    private Button startBtn, pauseBtn, resetBtn;

    private BaseController<?> currentSubController;
    private ResourceBundle resources;

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
            }, I18N.localeProperty(), delayMsPropertyForBinding()));
        }
    }

    private void setupGlobalActions() {
        startBtn.setOnAction(e -> {
            if (currentSubController != null)
                currentSubController.handleAlgorithmStart();
        });

        pauseBtn.setOnAction(e -> {
            if (currentSubController != null) {
                currentSubController.togglePause();
                logArea.appendText(AlgorithmThreadManager.isPaused() ? "Command: PAUSED\n" : "Command: RESUMED\n");
            }
        });

        resetBtn.setOnAction(e -> {
            if (currentSubController != null) {
                currentSubController.stopAlgorithm();
                currentSubController.getVisualizer().clear();
                logArea.clear();
                logArea.appendText("System: Module reset.\n");
            }
        });
    }

    /**
     * 加载子模块的核心逻辑
     */
    private void loadSubController(BaseController<?> newController) {
        AlgorithmThreadManager.stopAll();

        visualizationContainer.getChildren().clear();
        customControlBox.getChildren().clear();

        newController.setUIReferences(statsLabel, logArea, delaySlider);

        // 4. 挂载画布
        BaseVisualizer<?> viz = newController.getVisualizer();
        if (viz != null) {
            viz.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            viz.setMinSize(0, 0);
            visualizationContainer.getChildren().add(viz);
            viz.prefWidthProperty().bind(visualizationContainer.widthProperty());
            viz.prefHeightProperty().bind(visualizationContainer.heightProperty());
            viz.requestLayout();
        }
        List<Node> subControls = newController.getCustomControls();
        if (subControls != null) {
            customControlBox.getChildren().addAll(subControls);
        }

        this.currentSubController = newController;
    }

    // --- 模块切换方法 (保持你的逻辑) ---

    @FXML
    public void switchToSortModule() {
        loadSubController(new SortController<Integer>(
                new QuickSort<Integer>(),
                new HistogramSortVisualizer<Integer>()));
    }

    @FXML
    public void switchToMazeModule() {
        ArrayMaze maze = new ArrayMaze(51, 51);
        BFSMazeGenerator gen = new BFSMazeGenerator();
        gen.setMazeEntity(maze);
        SquareMazeVisualizer visualizer = new SquareMazeVisualizer();
        loadSubController(new MazeController<int[][]>(maze, gen, visualizer));
    }

    @FXML
    public void switchToTreeModule() {
        BaseTree<Integer> tree = new BaseTree<>();
        AVLTree<Integer> algorithms = new AVLTree<>();
        Integer[] array = AlgorithmsUtils.randomArray(23, 12);
        for (Integer integer : array) {
            algorithms.put(tree, integer);
        }

        loadSubController(new TreeController<>(tree, algorithms));
    }

    @FXML
    public void switchToGraphModule() {
        DirectedGraph<Integer> graph = new DirectedGraph<>("A");
        AlgorithmsUtils.buildRandomGraph(graph, 15, 20, true);
        loadSubController(new GraphController<>(new BFSAlgorithms<>(), graph));
    }

    // 辅助方法，用于刷新按钮绑定
    private Property<Number> delayMsPropertyForBinding() {
        return delaySlider.valueProperty();
    }

    @FXML
    private void toggleLanguage() {
        Locale currentLocale = I18N.getLocale();

        // 翻转语言状态
        Locale newLocale = (currentLocale.getLanguage().equals("zh"))
                ? Locale.ENGLISH
                : Locale.CHINESE;

        // 更新 I18N 的 Property，这会自动触发所有 createStringBinding 的 UI 刷新
        I18N.setLocale(newLocale);

        if (logArea != null) {
            String msg = (newLocale == Locale.CHINESE) ? "系统：语言已切换为中文" : "System: Language switched to English";
            logArea.appendText(msg + "\n");
        }
    }

    @FXML
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // TODO Auto-generated method stub
        if (resources != null) {
            this.resources = resources;
            I18N.setLocale(resources.getLocale());
        }
        setupI18n();
        setupGlobalActions();
        switchToSortModule(); // 初始模块
        logArea.appendText("System: Lab Initialized.\n");
    }
}