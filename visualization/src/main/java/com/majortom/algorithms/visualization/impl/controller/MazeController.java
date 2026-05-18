package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.base.BaseStructure;
import com.majortom.algorithms.core.maze.BaseGraphMazeAlgorithms;
import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.BaseArrayMazeAlgorithms;
import com.majortom.algorithms.core.maze.algorithms.array.generate.BFSArrayMazeGenerator;
import com.majortom.algorithms.core.maze.impl.ArrayMaze;
import com.majortom.algorithms.core.maze.impl.GraphMaze;
import com.majortom.algorithms.utils.EffectUtils;
import com.majortom.algorithms.visualization.algorithm.AlgorithmDescriptor;
import com.majortom.algorithms.visualization.algorithm.AlgorithmFamily;
import com.majortom.algorithms.visualization.algorithm.AlgorithmRegistry;
import com.majortom.algorithms.visualization.algorithm.AlgorithmStructure;
import com.majortom.algorithms.visualization.impl.visualizer.MazeModuleVisualizer;
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
 * <p>这个控制器统一管理“迷宫模块”下的两种结构：二维数组迷宫和图结构迷宫。
 * 结构切换仍然发生在同一个模块的下拉框里，但算法和数据结构本身已经分离成各自独立的实现链。</p>
 *
 * <p>也就是说，用户看到的是一个统一的迷宫入口；控制器内部则会根据当前结构，
 * 把操作分发给数组迷宫或图迷宫的独立结构与算法。</p>
 */
public class MazeController extends BaseModuleController<BaseStructure<?>> {

    private enum MazeMode {
        GENERATION, SOLVING
    }

    private enum MazeStructure {
        ARRAY, GRAPH
    }

    private BaseStructure<?> mazeEntity;
    private BaseAlgorithms<?> currentAlgorithm;
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

    public MazeController(ArrayMaze mazeEntity, BFSArrayMazeGenerator generator, MazeModuleVisualizer visualizer) {
        super(visualizer, "/fxml/MazeControls.fxml");
        ensureControllerState();
        this.mazeEntity = mazeEntity;
        this.currentAlgorithm = generator;
        renderInitialMaze();
    }

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

    @Override
    public void handleAlgorithmStart() {
        if (!AlgorithmThreadManager.isRunning()) {
            handleGenerate();
        }
    }

    @FXML
    public void handleGenerate() {
        runGenerator(selectedAlgorithmDescriptor(AlgorithmFamily.MAZE_GENERATOR, generatorSelector));
    }

    @FXML
    public void handleSolve() {
        runPathfinder(selectedAlgorithmDescriptor(AlgorithmFamily.MAZE_PATHFINDER, pathfinderSelector));
    }

    @FXML
    private void openMazeOperationDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(I18N.text("dialog.maze.title"));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        Label valueLabel = new Label(rowsOf(mazeEntity) + "x" + colsOf(mazeEntity));
        valueLabel.getStyleClass().add("size-value-highlight");

        Slider sizeSlider = new Slider(11, 99, rowsOf(mazeEntity));
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

        VBox content = new VBox(12, sizeSection);
        content.getStyleClass().add("operation-dialog-content");

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/style/ui_theme.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("operation-dialog-pane");
        dialog.getDialogPane().setPrefWidth(560);
        dialog.setResizable(true);
        dialog.showAndWait();
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void executeAlgorithm(BaseAlgorithms<BaseStructure<?>> alg, BaseStructure<?> data) {
        if (alg instanceof BaseArrayMazeAlgorithms && data instanceof BaseMaze<?> maze) {
            ((BaseArrayMazeAlgorithms) alg).execute(maze);
            return;
        }
        if (alg instanceof BaseGraphMazeAlgorithms && data instanceof GraphMaze graphMaze) {
            ((BaseGraphMazeAlgorithms) alg).execute(graphMaze);
        }
    }

    @Override
    protected String formatStatsMessage() {
        ensureControllerState();
        String mode = (currentMode == MazeMode.SOLVING) ? "Pathfinding" : "Generation";
        String structureKey = currentStructure == MazeStructure.GRAPH
                ? "label.maze.structure.graph"
                : "label.maze.structure.array";
        return String.format("%s | %s | %s | %s",
                I18N.text("stats.maze.structure", I18N.text(structureKey)),
                I18N.text("stats.maze.mode", mode),
                formatMetric("stats.action", stats.actionCount()),
                I18N.text("stats.maze.scale", colsOf(mazeEntity), rowsOf(mazeEntity)));
    }

    @Override
    protected void onResetData() {
        if (mazeEntity == null) {
            return;
        }
        initializeStructureSilently(mazeEntity);
        visualizer.render(mazeEntity, null, null);
        refreshStatsDisplay();
        appendLog(I18N.text(currentStructure == MazeStructure.GRAPH
                ? "message.graph_maze.reset"
                : "message.maze.reset"));
    }

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

    @Override
    protected String moduleId() {
        return "maze";
    }

    @Override
    protected String executionAlgorithmId(BaseAlgorithms<BaseStructure<?>> algorithm) {
        return currentAlgorithmId == null ? super.executionAlgorithmId(algorithm) : currentAlgorithmId;
    }

    private void runGenerator(AlgorithmDescriptor descriptor) {
        if (descriptor == null) {
            appendLog(I18N.text(currentStructure == MazeStructure.GRAPH
                    ? "message.graph_maze.algorithm_reserved"
                    : "message.maze.reset"));
            return;
        }

        stopAlgorithm();
        initializeStructureSilently(mazeEntity);
        currentMode = MazeMode.GENERATION;
        currentAlgorithm = descriptor.create();
        currentAlgorithmId = descriptor.id();

        bindAlgorithmToCurrentStructure(currentAlgorithm, mazeEntity);
        startCurrentAlgorithm(currentAlgorithm, mazeEntity);
    }

    private void runPathfinder(AlgorithmDescriptor descriptor) {
        if (AlgorithmThreadManager.isRunning() || mazeEntity == null) {
            return;
        }
        if (descriptor == null) {
            appendLog(I18N.text(currentStructure == MazeStructure.GRAPH
                    ? "message.graph_maze.algorithm_reserved"
                    : "message.maze.reset"));
            return;
        }

        currentAlgorithm = descriptor.create();
        currentAlgorithmId = descriptor.id();

        prepareStructureForSolving(mazeEntity);
        visualizer.render(mazeEntity, null, null);
        appendLog(I18N.text("message.maze.snapshot_ready"));

        currentMode = MazeMode.SOLVING;
        bindAlgorithmToCurrentStructure(currentAlgorithm, mazeEntity);
        startCurrentAlgorithm(currentAlgorithm, mazeEntity);
    }

    private void updateMazeSize(int oddSize) {
        stopAlgorithm();
        mazeEntity = createMazeEntity(oddSize);
        initializeStructureSilently(mazeEntity);
        visualizer.render(mazeEntity, null, null);
        refreshStatsDisplay();
    }

    private int normalizeOddSize(double rawSize) {
        int val = (int) rawSize;
        return (val % 2 == 0) ? val + 1 : val;
    }

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

        Platform.runLater(() -> structureSelector.getSelectionModel().select(currentStructure == MazeStructure.GRAPH ? 1 : 0));
    }

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

        int size = rowsOf(mazeEntity);
        mazeEntity = createMazeEntity(size);
        initializeStructureSilently(mazeEntity);
        visualizer.render(mazeEntity, null, null);
        updateStructureControlState();
        refreshStatsDisplay();
        appendLog(I18N.text(currentStructure == MazeStructure.GRAPH
                ? "message.graph_maze.reset"
                : "message.maze.reset"));
    }

    private void updateStructureControlState() {
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

    private List<AlgorithmDescriptor> mazeOptions(AlgorithmFamily family) {
        return AlgorithmRegistry.find(moduleId(), family, algorithmStructure.get());
    }

    private void ensureControllerState() {
        if (currentMode == null) {
            currentMode = MazeMode.GENERATION;
        }
        if (currentStructure == null) {
            currentStructure = MazeStructure.ARRAY;
        }
        if (algorithmStructure == null) {
            algorithmStructure = new SimpleObjectProperty<>(AlgorithmStructure.ARRAY_MAZE);
        }
    }

    private void renderInitialMaze() {
        if (mazeEntity == null) {
            return;
        }
        initializeStructureSilently(mazeEntity);
        visualizer.render(mazeEntity, null, null);
        updateStructureControlState();
        refreshStatsDisplay();
    }

    private AlgorithmDescriptor selectedAlgorithmDescriptor(AlgorithmFamily family, ComboBox<String> comboBox) {
        List<AlgorithmDescriptor> options = mazeOptions(family);
        int index = comboBox == null ? 0 : comboBox.getSelectionModel().getSelectedIndex();

        if (index < 0 || index >= options.size()) {
            currentAlgorithmId = null;
            return null;
        }
        return options.get(index);
    }

    private AlgorithmStructure toAlgorithmStructure(MazeStructure structure) {
        return structure == MazeStructure.GRAPH
                ? AlgorithmStructure.GRAPH_MAZE
                : AlgorithmStructure.ARRAY_MAZE;
    }

    private BaseStructure<?> createMazeEntity(int size) {
        return currentStructure == MazeStructure.GRAPH
                ? new GraphMaze(size, size)
                : new ArrayMaze(size, size);
    }

    private void initializeStructureSilently(BaseStructure<?> structure) {
        if (structure instanceof BaseMaze<?> maze) {
            maze.initialSilent();
            return;
        }
        if (structure instanceof GraphMaze graphMaze) {
            graphMaze.initialSilent();
        }
    }

    private void prepareStructureForSolving(BaseStructure<?> structure) {
        if (structure instanceof BaseMaze<?> maze) {
            maze.setGenerated(true);
            maze.clearVisualStates();
            maze.pickRandomPointsOnAvailablePaths();
            return;
        }
        if (structure instanceof GraphMaze graphMaze) {
            graphMaze.setGenerated(true);
            graphMaze.clearVisualStates();
            graphMaze.pickRandomPointsOnAvailablePaths();
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void bindAlgorithmToCurrentStructure(BaseAlgorithms<?> algorithm, BaseStructure<?> structure) {
        if (algorithm instanceof BaseArrayMazeAlgorithms<?> mazeAlgorithm && structure instanceof BaseMaze<?> maze) {
            ((BaseArrayMazeAlgorithms) mazeAlgorithm).setMazeEntity(maze);
            return;
        }
        if (algorithm instanceof BaseGraphMazeAlgorithms<?> graphAlgorithm && structure instanceof GraphMaze graphMaze) {
            ((BaseGraphMazeAlgorithms) graphAlgorithm).setMazeEntity(graphMaze);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void startCurrentAlgorithm(BaseAlgorithms<?> algorithm, BaseStructure<?> structure) {
        startAlgorithm((BaseAlgorithms) algorithm, structure);
    }

    private int rowsOf(BaseStructure<?> structure) {
        if (structure instanceof BaseMaze<?> maze) {
            return maze.getRows();
        }
        if (structure instanceof GraphMaze graphMaze) {
            return graphMaze.getRows();
        }
        return 0;
    }

    private int colsOf(BaseStructure<?> structure) {
        if (structure instanceof BaseMaze<?> maze) {
            return maze.getCols();
        }
        if (structure instanceof GraphMaze graphMaze) {
            return graphMaze.getCols();
        }
        return 0;
    }
}
