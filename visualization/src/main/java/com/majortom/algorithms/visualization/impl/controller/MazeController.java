package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.BaseMazeAlgorithms;
import com.majortom.algorithms.core.maze.impl.ArrayMaze;
import com.majortom.algorithms.core.maze.impl.GraphMaze;
import com.majortom.algorithms.utils.EffectUtils;
import com.majortom.algorithms.visualization.algorithm.AlgorithmDescriptor;
import com.majortom.algorithms.visualization.algorithm.AlgorithmFamily;
import com.majortom.algorithms.visualization.algorithm.AlgorithmRegistry;
import com.majortom.algorithms.visualization.algorithm.AlgorithmStructure;
import com.majortom.algorithms.visualization.base.BaseMazeVisualizer;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.manager.AlgorithmThreadManager;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * 迷宫模块控制器。
 *
 * <p>底栏保持“结构 + 算法 + 运行 + 操作”的统一形态，尺寸等低频配置放入操作弹窗。
 * 这样迷宫模块不会因为多个算法下拉和多个按钮把默认底栏挤爆。</p>
 *
 * @param <T> 迷宫底层数据类型
 */
public class MazeController<T> extends BaseModuleController<BaseMaze<?>> {

    /**
     * 当前迷宫运行模式。
     */
    private enum MazeMode {
        GENERATION, SOLVING
    }

    /**
     * 当前迷宫底层结构。
     */
    private enum MazeStructure {
        ARRAY, GRAPH
    }

    private BaseMazeAlgorithms<T> currentAlgorithm;
    private BaseMaze<?> mazeEntity;
    private MazeMode currentMode;
    private MazeStructure currentStructure;
    private String currentAlgorithmId;
    private ObjectProperty<AlgorithmStructure> algorithmStructure;

    @FXML
    private ComboBox<String> structureSelector;
    @FXML
    private ComboBox<String> generatorSelector;
    @FXML
    private ComboBox<String> pathfinderSelector;
    @FXML
    private Label structureTitleLabel;
    @FXML
    private Label generatorTitleLabel;
    @FXML
    private Label pathfinderTitleLabel;
    @FXML
    private Button buildBtn;
    @FXML
    private Button solveBtn;
    @FXML
    private Button operationBtn;

    /**
     * 创建迷宫控制器。
     *
     * @param mazeEntity 迷宫实体
     * @param generator 默认生成算法；保留构造签名兼容模块注册，实际算法下拉由注册表驱动
     * @param visualizer 迷宫可视化器
     */
    public MazeController(BaseMaze<T> mazeEntity,
            BaseMazeAlgorithms<T> generator,
            BaseMazeVisualizer<BaseMaze<?>> visualizer) {
        super(visualizer, "/fxml/MazeControls.fxml");
        ensureControllerState();
        this.mazeEntity = mazeEntity;
        this.currentAlgorithm = generator;
        renderInitialMaze();
    }

    /**
     * 初始化结构下拉、算法下拉和初始画面。
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ensureControllerState();
        super.initialize(location, resources);

        bindStructureSelector();
        bindAlgorithmComboBox(generatorSelector, AlgorithmFamily.MAZE_GENERATOR);
        bindAlgorithmComboBox(pathfinderSelector, AlgorithmFamily.MAZE_PATHFINDER);
        EffectUtils.applyDynamicEffect(buildBtn, solveBtn, operationBtn);
        updateStructureControlState();

        if (mazeEntity != null) {
            renderInitialMaze();
        }
    }

    /**
     * 全局 Start 按钮默认运行当前选中的迷宫算法。
     */
    @Override
    public void handleAlgorithmStart() {
        if (!AlgorithmThreadManager.isRunning()) {
            handleGenerate();
        }
    }

    /**
     * UI 事件：运行当前选中的生成算法。
     */
    @FXML
    public void handleGenerate() {
        runGenerator(selectedAlgorithmDescriptor(AlgorithmFamily.MAZE_GENERATOR, generatorSelector));
    }

    /**
     * UI 事件：运行当前选中的求解算法。
     */
    @FXML
    public void handleSolve() {
        runPathfinder(selectedAlgorithmDescriptor(AlgorithmFamily.MAZE_PATHFINDER, pathfinderSelector));
    }

    /**
     * 打开迷宫操作弹窗。
     */
    @FXML
    private void openMazeOperationDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(I18N.text("dialog.maze.title"));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        Label valueLabel = new Label(mazeEntity.getRows() + "x" + mazeEntity.getCols());
        valueLabel.getStyleClass().add("size-value-highlight");

        Slider sizeSlider = new Slider(11, 99, mazeEntity.getRows());
        sizeSlider.setShowTickLabels(true);
        sizeSlider.setShowTickMarks(true);
        sizeSlider.setMajorTickUnit(22);
        sizeSlider.setPrefWidth(420);
        sizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int oddSize = normalizeOddSize(newVal.doubleValue());
            valueLabel.setText(oddSize + "x" + oddSize);
        });

        Button applyButton = new Button(I18N.text("action.maze.apply_size"));
        applyButton.getStyleClass().addAll("btn-ran-gold", "compact-button");
        applyButton.setOnAction(event -> updateMazeSize(normalizeOddSize(sizeSlider.getValue())));

        HBox sizeRow = new HBox(12, sizeSlider, valueLabel);

        VBox sizeSection = new VBox(10,
                new Label(I18N.text("label.maze.size")),
                sizeRow,
                new HBox(10, applyButton));
        sizeSection.getStyleClass().add("dialog-form-section");

        VBox content = new VBox(12,
                sizeSection);
        content.getStyleClass().add("operation-dialog-content");

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/style/ui_theme.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("operation-dialog-pane");
        dialog.getDialogPane().setPrefWidth(560);
        dialog.setResizable(true);
        dialog.showAndWait();
    }

    /**
     * 执行迷宫算法。
     */
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void executeAlgorithm(BaseAlgorithms<BaseMaze<?>> alg, BaseMaze<?> data) {
        if (alg instanceof BaseMazeAlgorithms) {
            ((BaseMazeAlgorithms<T>) (BaseMazeAlgorithms) alg).execute(arrayMazeEntity());
        }
    }

    /**
     * 格式化迷宫统计信息。
     */
    @Override
    protected String formatStatsMessage() {
        ensureControllerState();
        String mode = (currentMode == MazeMode.SOLVING) ? "Pathfinding" : "Generation";
        String structureKey = currentStructure == MazeStructure.GRAPH
                ? "label.maze.structure.graph"
                : "label.maze.structure.array";
        int cols = mazeEntity == null ? 0 : mazeEntity.getCols();
        int rows = mazeEntity == null ? 0 : mazeEntity.getRows();
        return String.format("%s | %s | %s | %s",
                I18N.text("stats.maze.structure", I18N.text(structureKey)),
                I18N.text("stats.maze.mode", mode),
                formatMetric("stats.action", stats.actionCount()),
                I18N.text("stats.maze.scale", cols, rows));
    }

    /**
     * 重置当前迷宫。
     */
    @Override
    protected void onResetData() {
        if (mazeEntity != null) {
            mazeEntity.initialSilent();
            visualizer.render(mazeEntity, null, null);
            refreshStatsDisplay();
            appendLog(I18N.text("message.maze.reset"));
        }
    }

    /**
     * 绑定迷宫模块文案。
     */
    @Override
    protected void setupI18n() {
        if (structureTitleLabel != null) {
            structureTitleLabel.textProperty().bind(I18N.createStringBinding("label.maze.structure"));
        }
        if (generatorTitleLabel != null) {
            generatorTitleLabel.textProperty().bind(I18N.createStringBinding("label.maze.generator"));
        }
        if (pathfinderTitleLabel != null) {
            pathfinderTitleLabel.textProperty().bind(I18N.createStringBinding("label.maze.solver"));
        }
        if (buildBtn != null) {
            buildBtn.textProperty().bind(I18N.createStringBinding("action.maze.build"));
        }
        if (solveBtn != null) {
            solveBtn.textProperty().bind(I18N.createStringBinding("action.maze.solve"));
        }
        if (operationBtn != null) {
            operationBtn.textProperty().bind(I18N.createStringBinding("action.maze.operation"));
        }
    }

    /**
     * 获取模块 ID。
     */
    @Override
    protected String moduleId() {
        return "maze";
    }

    /**
     * 使用注册表中的稳定算法 ID 写入执行记录。
     */
    @Override
    protected String executionAlgorithmId(BaseAlgorithms<BaseMaze<?>> algorithm) {
        return currentAlgorithmId == null ? super.executionAlgorithmId(algorithm) : currentAlgorithmId;
    }

    /**
     * 执行生成算法。
     */
    private void runGenerator(AlgorithmDescriptor descriptor) {
        if (descriptor == null) {
            appendLog(I18N.text("message.graph_maze.algorithm_reserved"));
            return;
        }

        stopAlgorithm();
        mazeEntity.initialSilent();
        currentMode = MazeMode.GENERATION;

        this.currentAlgorithm = createMazeAlgorithm(descriptor);

        if (this.currentAlgorithm == null) {
            visualizer.render(mazeEntity, null, null);
            refreshStatsDisplay();
            appendLog(I18N.text("message.graph_maze.ready"));
            return;
        }

        this.currentAlgorithm.setMazeEntity(arrayMazeEntity());
        startArrayMazeAlgorithm(currentAlgorithm, arrayMazeEntity());
    }

    /**
     * 执行求解算法。
     */
    private void runPathfinder(AlgorithmDescriptor descriptor) {
        if (AlgorithmThreadManager.isRunning() || mazeEntity == null) {
            return;
        }

        if (descriptor == null) {
            appendLog(I18N.text("message.graph_maze.algorithm_reserved"));
            return;
        }

        BaseMazeAlgorithms<T> solver = createMazeAlgorithm(descriptor);

        if (solver == null) {
            appendLog(I18N.text("message.graph_maze.algorithm_reserved"));
            return;
        }

        mazeEntity.setGenerated(true);
        mazeEntity.clearVisualStates();
        mazeEntity.pickRandomPointsOnAvailablePaths();
        visualizer.render(mazeEntity, null, null);
        appendLog(I18N.text("message.maze.snapshot_ready"));

        currentMode = MazeMode.SOLVING;
        currentAlgorithm = solver;
        currentAlgorithm.setMazeEntity(arrayMazeEntity());
        startArrayMazeAlgorithm(currentAlgorithm, arrayMazeEntity());
    }

    /**
     * 按尺寸重建当前结构类型的迷宫。
     */
    private void updateMazeSize(int oddSize) {
        stopAlgorithm();
        this.mazeEntity = createMazeEntity(oddSize);
        this.mazeEntity.initialSilent();
        visualizer.render(mazeEntity, null, null);
        refreshStatsDisplay();
    }

    /**
     * 把迷宫尺寸修正为奇数。
     */
    private int normalizeOddSize(double rawSize) {
        int val = (int) rawSize;
        return (val % 2 == 0) ? val + 1 : val;
    }

    /**
     * 绑定结构下拉。
     */
    private void bindStructureSelector() {
        ensureControllerState();
        if (structureSelector == null) {
            return;
        }

        structureSelector.itemsProperty().bind(Bindings.createObjectBinding(() -> {
            ObservableList<String> list = FXCollections.observableArrayList();
            list.add(I18N.text("label.maze.structure.array"));
            list.add(I18N.text("label.maze.structure.graph"));
            return list;
        }, I18N.localeProperty()));

        structureSelector.getSelectionModel().selectedIndexProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.intValue() < 0) {
                return;
            }
            switchStructure(newVal.intValue() == 1 ? MazeStructure.GRAPH : MazeStructure.ARRAY);
        });

        Platform.runLater(() -> structureSelector.getSelectionModel().selectFirst());
    }

    /**
     * 绑定算法下拉框到注册表查询结果。
     */
    private void bindAlgorithmComboBox(ComboBox<String> comboBox, AlgorithmFamily family) {
        ensureControllerState();
        if (comboBox == null) {
            return;
        }

        comboBox.itemsProperty().bind(Bindings.createObjectBinding(() -> {
            ObservableList<String> list = FXCollections.observableArrayList();
            mazeOptions(family).forEach(option -> list.add(I18N.text(option.labelKey())));
            return list;
        }, I18N.localeProperty(), algorithmStructure));

        comboBox.itemsProperty().addListener((obs, oldItems, newItems) -> {
            if (newItems == null || newItems.isEmpty()) {
                comboBox.getSelectionModel().clearSelection();
            } else {
                comboBox.getSelectionModel().selectFirst();
            }
            updateStructureControlState();
        });

        AlgorithmThreadManager.postStatus(() -> {
            if (comboBox.getItems() != null && !comboBox.getItems().isEmpty()) {
                comboBox.getSelectionModel().selectFirst();
            }
            updateStructureControlState();
        });
    }

    /**
     * 切换迷宫底层结构，并刷新当前可视化画面。
     */
    private void switchStructure(MazeStructure structure) {
        ensureControllerState();
        if (structure == currentStructure || AlgorithmThreadManager.isRunning()) {
            updateStructureControlState();
            return;
        }

        stopAlgorithm();
        currentStructure = structure;
        currentMode = MazeMode.GENERATION;
        algorithmStructure.set(toAlgorithmStructure(structure));

        if (mazeEntity == null) {
            updateStructureControlState();
            return;
        }

        int size = mazeEntity.getRows();
        mazeEntity = createMazeEntity(size);
        mazeEntity.initialSilent();
        visualizer.render(mazeEntity, null, null);
        updateStructureControlState();
        refreshStatsDisplay();
        appendLog(I18N.text(currentStructure == MazeStructure.GRAPH
                ? "message.graph_maze.reset"
                : "message.maze.reset"));
    }

    /**
     * 根据结构类型启用或禁用算法控件。
     */
    private void updateStructureControlState() {
        ensureControllerState();
        boolean hasGenerators = !mazeOptions(AlgorithmFamily.MAZE_GENERATOR).isEmpty();
        boolean hasPathfinders = !mazeOptions(AlgorithmFamily.MAZE_PATHFINDER).isEmpty();

        if (generatorSelector != null) {
            generatorSelector.setDisable(!hasGenerators);
        }
        if (pathfinderSelector != null) {
            pathfinderSelector.setDisable(!hasPathfinders);
        }
        if (buildBtn != null) {
            buildBtn.setDisable(!hasGenerators);
        }
        if (solveBtn != null) {
            solveBtn.setDisable(!hasPathfinders);
        }
    }

    /**
     * 查询当前结构下指定用途的迷宫算法。
     */
    private List<AlgorithmDescriptor> mazeOptions(AlgorithmFamily family) {
        ensureControllerState();
        return AlgorithmRegistry.find(moduleId(), family, algorithmStructure.get());
    }

    /**
     * 确保 FXML 初始化期间也能安全访问控制器状态。
     *
     * <p>{@link BaseModuleController} 会在父类构造器中加载 FXML，JavaFX 随即调用
     * {@link #initialize(URL, ResourceBundle)}。这发生在子类字段初始化器和构造器主体之前，
     * 因此这里不用字段初始化器，而是通过懒初始化保证下拉绑定、结构切换和统计刷新都不会遇到
     * 半初始化状态。</p>
     */
    private void ensureControllerState() {
        if (currentMode == null) {
            currentMode = MazeMode.GENERATION;
        }
        if (currentStructure == null) {
            currentStructure = MazeStructure.ARRAY;
        }
        if (algorithmStructure == null) {
            algorithmStructure = new SimpleObjectProperty<>(toAlgorithmStructure(currentStructure));
        }
    }

    /**
     * 在构造参数可用后渲染初始迷宫。
     */
    private void renderInitialMaze() {
        if (mazeEntity == null) {
            return;
        }
        mazeEntity.initialSilent();
        visualizer.render(mazeEntity, null, null);
        updateStructureControlState();
        refreshStatsDisplay();
    }

    /**
     * 获取当前选中的算法描述。
     */
    private AlgorithmDescriptor selectedAlgorithmDescriptor(AlgorithmFamily family, ComboBox<String> comboBox) {
        List<AlgorithmDescriptor> options = mazeOptions(family);
        int index = comboBox == null ? 0 : comboBox.getSelectionModel().getSelectedIndex();

        if (index < 0 || index >= options.size()) {
            currentAlgorithmId = null;
            return null;
        }

        return options.get(index);
    }

    /**
     * 创建当前选中的迷宫算法。
     */
    @SuppressWarnings("unchecked")
    private BaseMazeAlgorithms<T> createMazeAlgorithm(AlgorithmDescriptor descriptor) {
        currentAlgorithmId = descriptor.id();
        return (BaseMazeAlgorithms<T>) descriptor.create();
    }

    /**
     * 把 UI 结构枚举转换成注册表使用的结构分类。
     */
    private AlgorithmStructure toAlgorithmStructure(MazeStructure structure) {
        return structure == MazeStructure.GRAPH
                ? AlgorithmStructure.GRAPH_MAZE
                : AlgorithmStructure.ARRAY_MAZE;
    }

    /**
     * 按当前结构类型创建迷宫实体。
     */
    private BaseMaze<?> createMazeEntity(int size) {
        return currentStructure == MazeStructure.GRAPH
                ? new GraphMaze(size, size)
                : new ArrayMaze(size, size);
    }

    /**
     * 用于把当前迷宫安全地交给旧的二维数组迷宫算法。
     */
    @SuppressWarnings("unchecked")
    private BaseMaze<T> arrayMazeEntity() {
        return (BaseMaze<T>) mazeEntity;
    }

    /**
     * 使用受控泛型桥接启动二维数组迷宫算法。
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void startArrayMazeAlgorithm(BaseMazeAlgorithms<T> algorithm, BaseMaze<T> data) {
        startAlgorithm((BaseAlgorithms) algorithm, data);
    }
}
