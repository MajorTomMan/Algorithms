package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.library.maze.ArrayMazeGenerationInput;
import com.majortom.algorithms.library.maze.ArrayMazePathInput;
import com.majortom.algorithms.library.maze.GraphMazeGenerationInput;
import com.majortom.algorithms.library.maze.GridMaze;
import com.majortom.algorithms.utils.EffectUtils;
import com.majortom.algorithms.visualization.algorithm.AlgorithmLabels;
import com.majortom.algorithms.visualization.impl.visualizer.MazeModuleVisualizer;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.runtime.maze.MazeEventReducer;
import com.majortom.algorithms.visualization.runtime.maze.MazeViewState;
import com.majortom.algorithms.visualization.structure.StructureSnapshot;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public final class MazeController extends BaseModuleController<MazeViewState> {

    private enum Structure { ARRAY, GRAPH }

    private static final List<String> ARRAY_GENERATORS = List.of(
            "maze-generator-bfs", "maze-generator-dfs", "maze-generator-union-find");
    private static final List<String> ARRAY_PATHFINDERS = List.of(
            "maze-pathfinder-astar", "maze-pathfinder-dfs");
    private static final List<String> GRAPH_GENERATORS = List.of("graph-generator-bfs");

    private int size = 51;
    private Structure structure = Structure.ARRAY;
    private GridMaze generatedMaze;
    private boolean solving;

    @FXML private ComboBox<String> structureSelector;
    @FXML private ComboBox<String> generatorSelector;
    @FXML private ComboBox<String> pathfinderSelector;
    @FXML private Label structureTitleLabel;
    @FXML private Label generatorTitleLabel;
    @FXML private Label pathfinderTitleLabel;
    @FXML private Label sizeSectionLabel;
    @FXML private Label operationsSectionLabel;
    @FXML private Label sizeLabel;
    @FXML private Label sizeValueLabel;
    @FXML private Label operationHintLabel;
    @FXML private Slider sizeSlider;
    @FXML private Button buildBtn;
    @FXML private Button solveBtn;
    @FXML private Button applySizeBtn;
    @FXML private Button resetMazeBtn;

    public MazeController() {
        super(new MazeModuleVisualizer(), "/fxml/MazeControls.fxml");
        renderEmpty();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);
        bindSelectors();
        sizeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            int next = normalizeOdd(newValue.intValue());
            sizeValueLabel.setText(next + " x " + next);
        });
        sizeValueLabel.setText(size + " x " + size);
        EffectUtils.applyDynamicEffect(buildBtn, solveBtn, applySizeBtn, resetMazeBtn);
        updateControlState();
    }

    @Override
    public void handleAlgorithmStart() {
        handleGenerate();
    }

    @FXML
    public void handleGenerate() {
        if (isRunning()) {
            return;
        }
        solving = false;
        generatedMaze = null;
        String id = selectedId(generatorSelector, generatorIds());
        dispatchVisualizerAction(
                com.majortom.algorithms.visualization.VisualizationActionType.MAZE_BUILD,
                java.util.Map.of("algorithmId", id, "size", size));
        if (structure == Structure.GRAPH) {
            GraphMazeGenerationInput input = new GraphMazeGenerationInput(size, size, System.nanoTime());
            startAlgorithm(id, input, () -> new MazeEventReducer(size, size, true));
        } else {
            ArrayMazeGenerationInput input = new ArrayMazeGenerationInput(size, size, System.nanoTime());
            startAlgorithm(id, input, () -> new MazeEventReducer(size, size, false));
        }
    }

    @FXML
    public void handleSolve() {
        if (isRunning() || structure == Structure.GRAPH || generatedMaze == null) {
            return;
        }
        solving = true;
        String id = selectedId(pathfinderSelector, ARRAY_PATHFINDERS);
        dispatchVisualizerAction(
                com.majortom.algorithms.visualization.VisualizationActionType.MAZE_SOLVE,
                java.util.Map.of("algorithmId", id, "size", size));
        ArrayMazePathInput input = new ArrayMazePathInput(
                generatedMaze, generatedMaze.entrance(), generatedMaze.exit());
        startAlgorithm(id, input, () -> new MazeEventReducer(size, size, false));
    }

    @FXML
    private void handleApplySize() {
        invalidateExecutionForInputChange();
        size = normalizeOdd((int) sizeSlider.getValue());
        sizeSlider.setValue(size);
        generatedMaze = null;
        solving = false;
        renderEmpty();
        updateControlState();
        refreshStatsDisplay();
        logI18n("message.maze.size_set", size, size);
    }

    @FXML
    private void handleResetMaze() {
        reset();
        logI18n("message.maze.reset");
    }

    @Override
    protected void onAlgorithmFinished() {
        super.onAlgorithmFinished();
        MazeViewState state = latestViewState();
        if (!solving && structure == Structure.ARRAY && state != null
                && state.entrance() != null && state.exit() != null) {
            generatedMaze = new GridMaze(
                    state.rows(), state.columns(), state.openCells(), state.entrance(), state.exit());
        }
        if (!solving && state != null) {
            renderStructureState(structureSnapshotState(state));
        }
        updateControlState();
        logI18n("message.execution.finished");
    }

    @Override
    protected String formatStatsMessage() {
        String structureName = I18N.text("label.maze.structure.array");
        if (structure == Structure.GRAPH) {
            structureName = I18N.text("label.maze.structure.graph");
        }
        String mode = "Generation";
        if (solving) {
            mode = "Pathfinding";
        }
        return String.format("%s | %s | %s | %s",
                I18N.text("stats.maze.structure", structureName),
                I18N.text("stats.maze.mode", mode),
                formatMetric("stats.action", mazeActionCount()),
                I18N.text("stats.maze.scale", size, size));
    }

    private long mazeActionCount() {
        return stats.metric("cells.opened") + stats.metric("edges.added")
                + stats.metric("cells.visited") + stats.metric("backtracks")
                + stats.metric("path.cells");
    }

    @Override
    protected void onResetData() {
        generatedMaze = null;
        solving = false;
        renderEmpty();
        updateControlState();
    }

    private void renderEmpty() {
        renderStructureState(MazeViewState.empty(size, size, structure == Structure.GRAPH));
    }

    @Override
    public StructureSnapshot<MazeViewState> captureStructureSnapshot() {
        return StructureSnapshot.create(moduleId(), structureSnapshotState());
    }

    @Override
    public void restoreStructureSnapshot(StructureSnapshot<MazeViewState> snapshot) {
        if (!moduleId().equals(snapshot.moduleId())) {
            throw new IllegalArgumentException("snapshot belongs to module " + snapshot.moduleId());
        }
        MazeViewState state = snapshot.state();
        structure = Structure.ARRAY;
        if (state.graphBased()) {
            structure = Structure.GRAPH;
        }
        if (structureSelector != null) {
            int index = 0;
            if (structure == Structure.GRAPH) {
                index = 1;
            }
            structureSelector.getSelectionModel().select(index);
        }
        size = state.rows();
        if (sizeSlider != null) {
            double sliderValue = Math.max(sizeSlider.getMin(), Math.min(sizeSlider.getMax(), size));
            sizeSlider.setValue(sliderValue);
        }
        if (sizeValueLabel != null) {
            sizeValueLabel.setText(size + " x " + size);
        }
        generatedMaze = null;
        if (!state.graphBased() && state.entrance() != null && state.exit() != null) {
            generatedMaze = new GridMaze(
                    state.rows(), state.columns(), state.openCells(), state.entrance(), state.exit());
        }
        solving = false;
        invalidateExecutionForInputChange();
        renderStructureState(structureSnapshotState(state));
        updateControlState();
        refreshStatsDisplay();
    }

    @Override
    public String describeStructureSnapshot(MazeViewState state) {
        long openCells = state.openCells().stream().filter(Boolean::booleanValue).count();
        return I18N.text("snapshot.maze.detail", state.rows(), state.columns(), openCells);
    }

    private MazeViewState structureSnapshotState() {
        if (generatedMaze != null) {
            return new MazeViewState(
                    generatedMaze.rows(), generatedMaze.columns(), generatedMaze.openCells(),
                    java.util.Set.of(), java.util.Set.of(), java.util.Set.of(), java.util.Set.of(),
                    java.util.Set.of(), java.util.Map.of(), generatedMaze.entrance(), generatedMaze.exit(),
                    null, java.util.List.of(), false, MazeViewState.Phase.IDLE, false);
        }
        MazeViewState latest = latestStructureState();
        if (latest != null) {
            return structureSnapshotState(latest);
        }
        return MazeViewState.empty(size, size, structure == Structure.GRAPH);
    }

    private MazeViewState structureSnapshotState(MazeViewState state) {
        return new MazeViewState(
                state.rows(), state.columns(), state.openCells(),
                java.util.Set.of(), java.util.Set.of(), java.util.Set.of(), java.util.Set.of(),
                java.util.Set.of(), java.util.Map.of(), state.entrance(), state.exit(), null,
                state.graphEdges(), state.graphBased(), MazeViewState.Phase.IDLE, false);
    }

    @Override
    protected void setupI18n() {
        if (structureTitleLabel != null) structureTitleLabel.textProperty().bind(I18N.createStringBinding("label.maze.structure"));
        if (generatorTitleLabel != null) generatorTitleLabel.textProperty().bind(I18N.createStringBinding("label.maze.generator"));
        if (pathfinderTitleLabel != null) pathfinderTitleLabel.textProperty().bind(I18N.createStringBinding("label.maze.solver"));
        if (sizeSectionLabel != null) sizeSectionLabel.textProperty().bind(I18N.createStringBinding("label.maze.size"));
        if (sizeLabel != null) sizeLabel.textProperty().bind(I18N.createStringBinding("label.maze.size"));
        if (operationsSectionLabel != null) {
            operationsSectionLabel.textProperty().bind(I18N.createStringBinding("label.panel.operations"));
        }
        if (operationHintLabel != null) {
            operationHintLabel.textProperty().bind(I18N.createStringBinding("label.maze.operation_hint"));
        }
        if (buildBtn != null) buildBtn.textProperty().bind(I18N.createStringBinding("action.maze.build"));
        if (solveBtn != null) solveBtn.textProperty().bind(I18N.createStringBinding("action.maze.solve"));
        if (applySizeBtn != null) applySizeBtn.textProperty().bind(I18N.createStringBinding("action.maze.apply_size"));
        if (resetMazeBtn != null) resetMazeBtn.textProperty().bind(I18N.createStringBinding("action.maze.reset"));
    }

    @Override
    protected String moduleId() {
        return "maze";
    }

    private void bindSelectors() {
        structureSelector.itemsProperty().bind(Bindings.createObjectBinding(() -> FXCollections.observableArrayList(
                I18N.text("label.maze.structure.array"), I18N.text("label.maze.structure.graph")),
                I18N.localeProperty()));
        generatorSelector.itemsProperty().bind(Bindings.createObjectBinding(
                () -> labels(generatorIds()), I18N.localeProperty(), structureSelector.getSelectionModel().selectedIndexProperty()));
        pathfinderSelector.itemsProperty().bind(Bindings.createObjectBinding(
                () -> labels(pathfinderIds()), I18N.localeProperty(), structureSelector.getSelectionModel().selectedIndexProperty()));
        structureSelector.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.intValue() < 0 || isRunning()) {
                return;
            }
            structure = Structure.ARRAY;
            if (newValue.intValue() == 1) {
                structure = Structure.GRAPH;
            }
            invalidateExecutionForInputChange();
            generatedMaze = null;
            solving = false;
            renderEmpty();
            selectFirstAlgorithms();
            updateControlState();
        });
        Platform.runLater(() -> {
            structureSelector.getSelectionModel().selectFirst();
            selectFirstAlgorithms();
        });
    }

    private javafx.collections.ObservableList<String> labels(List<String> ids) {
        javafx.collections.ObservableList<String> labels = FXCollections.observableArrayList();
        for (String id : ids) {
            labels.add(I18N.text(AlgorithmLabels.key(id)));
        }
        return labels;
    }

    private void selectFirstAlgorithms() {
        if (!generatorSelector.getItems().isEmpty()) generatorSelector.getSelectionModel().selectFirst();
        if (!pathfinderSelector.getItems().isEmpty()) pathfinderSelector.getSelectionModel().selectFirst();
    }

    private List<String> generatorIds() {
        if (structure == Structure.GRAPH) return GRAPH_GENERATORS;
        return ARRAY_GENERATORS;
    }

    private List<String> pathfinderIds() {
        if (structure == Structure.GRAPH) return List.of();
        return ARRAY_PATHFINDERS;
    }

    private String selectedId(ComboBox<String> comboBox, List<String> ids) {
        int index = comboBox.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= ids.size()) index = 0;
        return ids.get(index);
    }

    private void updateControlState() {
        boolean graph = structure == Structure.GRAPH;
        pathfinderSelector.setDisable(graph);
        solveBtn.setDisable(graph || generatedMaze == null);
    }

    private int normalizeOdd(int value) {
        if (value % 2 == 0) return value + 1;
        return value;
    }
}
