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
import com.majortom.algorithms.core.sort.impl.InsertionSort;
import com.majortom.algorithms.core.sort.impl.QuickSort;
import com.majortom.algorithms.core.tree.BaseTree;
import com.majortom.algorithms.core.tree.impl.AVLTree;
import com.majortom.algorithms.utils.EffectUtils; // 导入工具类
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
    @FXML
    private Button langBtn; // 确保 FXML 中有这个 ID

    private BaseController<?> currentSubController;
    private ResourceBundle resources;

    /**
     * 批量为所有按钮应用动态特效
     */
    private void applyEffectsToAllButtons() {
        // 导航栏按钮
        EffectUtils.applyDynamicEffect(sortBtn);
        EffectUtils.applyDynamicEffect(mazeBtn);
        EffectUtils.applyDynamicEffect(treeBtn);
        EffectUtils.applyDynamicEffect(graphBtn);

        // 控制台按钮
        EffectUtils.applyDynamicEffect(startBtn);
        EffectUtils.applyDynamicEffect(resetBtn);
        if (pauseBtn != null)
            EffectUtils.applyDynamicEffect(pauseBtn);
        if (langBtn != null)
            EffectUtils.applyDynamicEffect(langBtn);
    }

    @FXML
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (resources != null) {
            this.resources = resources;
            I18N.setLocale(resources.getLocale());
        }

        // 1. 设置国际化绑定
        setupI18n();

        // 2. 设置全局按钮行为
        setupGlobalActions();

        // 3. 应用动态呼吸灯特效 (划过呼吸，点完即消失)
        applyEffectsToAllButtons();

        // 4. 初始模块加载
        switchToSortModule();

        logArea.appendText("System: Lab Initialized.\n");
    }

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
            }, I18N.localeProperty(), delaySlider.valueProperty()));
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

    private void loadSubController(BaseController<?> newController) {
        AlgorithmThreadManager.stopAll();
        visualizationContainer.getChildren().clear();
        customControlBox.getChildren().clear();
        newController.setUIReferences(statsLabel, logArea, delaySlider);

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
        BFSMazeGenerator gen = new BFSMazeGenerator();
        gen.setMazeEntity(maze);
        loadSubController(new MazeController<>(maze, gen, new SquareMazeVisualizer()));
    }

    @FXML
    public void switchToTreeModule() {
        BaseTree<Integer> tree = new BaseTree<>();
        AVLTree<Integer> algorithms = new AVLTree<>();
        Integer[] array = AlgorithmsUtils.randomArray(23, 12);
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

    @FXML
    private void toggleLanguage() {
        Locale newLocale = (I18N.getLocale().getLanguage().equals("zh")) ? Locale.ENGLISH : Locale.CHINESE;
        I18N.setLocale(newLocale);
        if (logArea != null) {
            logArea.appendText(((newLocale == Locale.CHINESE) ? "系统：语言已切换" : "System: Language switched") + "\n");
        }
    }
}