package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.library.maze.ArrayMazeGenerator;
import com.majortom.algorithms.library.maze.ArrayMazePathfinder;
import com.majortom.algorithms.library.maze.GraphMazeGenerator;
import com.majortom.algorithms.library.maze.ArrayMazeGenerationInput;
import com.majortom.algorithms.library.maze.ArrayMazeGenerationOutput;
import com.majortom.algorithms.library.maze.ArrayMazePathInput;
import com.majortom.algorithms.library.maze.ArrayMazePathOutput;
import com.majortom.algorithms.library.maze.GraphMazeGenerationInput;
import com.majortom.algorithms.library.maze.GraphMazeGenerationOutput;
import com.majortom.algorithms.library.maze.GridMaze;
import com.majortom.algorithms.utils.EffectUtils;
import com.majortom.algorithms.visualization.algorithm.AlgorithmCatalog;
import com.majortom.algorithms.visualization.algorithm.AlgorithmLabels;
import com.majortom.algorithms.visualization.impl.visualizer.MazeVisualizer;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.module.AlgorithmSelectionSupport;
import com.majortom.algorithms.visualization.structure.StructureSnapshotSupport;
import com.majortom.algorithms.visualization.structure.SnapshotAlgorithmInputSupport;
import com.majortom.algorithms.visualization.runtime.maze.MazeEventReducer;
import com.majortom.algorithms.visualization.runtime.maze.MazeViewState;
import com.majortom.algorithms.core.snapshot.StructureSnapshot;
import com.majortom.algorithms.core.runtime.ExecutionResult;
import com.majortom.algorithms.core.snapshot.MazeSnapshot;
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

public final class MazeController extends BaseModuleController<MazeViewState>
        implements AlgorithmSelectionSupport, StructureSnapshotSupport<MazeSnapshot>, SnapshotAlgorithmInputSupport<MazeSnapshot> {

    private enum Structure { ARRAY, GRAPH }

    private enum Operation { GENERATE, SOLVE }

    private final List<String> arrayGenerators = AlgorithmCatalog.arrayMazeGenerators();
    private final List<String> graphGenerators = AlgorithmCatalog.graphMazeGenerators();
    private final List<String> arrayPathfinders = AlgorithmCatalog.arrayMazePathfinders();
    private final List<String> allGenerators = java.util.stream.Stream.concat(
            arrayGenerators.stream(), graphGenerators.stream()).toList();

    private int size = 51;
    private Structure structure = Structure.ARRAY;
    private GridMaze generatedMaze;
    private MazeSnapshot algorithmResultSnapshot;
    private StructureSnapshot<MazeSnapshot> algorithmInputSnapshot;
    private boolean applyingStructureState;
    private boolean solving;
    private Operation selectedOperation = Operation.GENERATE;

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
    @FXML private Button applyResultBtn;
    @FXML private Button resetMazeBtn;

    public MazeController() {
        super(new MazeVisualizer(), "/fxml/MazeControls.fxml");
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
        EffectUtils.applyDynamicEffect(buildBtn, solveBtn, applySizeBtn, applyResultBtn, resetMazeBtn);
        updateControlState();
    }

    @Override
    public void handleAlgorithmStart() {
        if (selectedOperation == Operation.SOLVE) {
            handleSolve();
            return;
        }
        handleGenerate();
    }

    @Override
    public boolean selectAlgorithm(String algorithmId) {
        if (isRunning()) {
            return false;
        }
        if (allGenerators.contains(algorithmId)) {
            boolean selected = selectAlgorithm(generatorSelector, allGenerators, algorithmId);
            if (selected) {
                selectedOperation = Operation.GENERATE;
                updateControlState();
            }
            return selected;
        }
        if (arrayPathfinders.contains(algorithmId)) {
            boolean selected = selectAlgorithm(pathfinderSelector, arrayPathfinders, algorithmId);
            if (selected) {
                selectedOperation = Operation.SOLVE;
                updateControlState();
            }
            return selected;
        }
        return false;
    }

    @FXML
    public void handleGenerate() {
        if (isRunning()) {
            return;
        }
        solving = false;
        selectedOperation = Operation.GENERATE;
        algorithmResultSnapshot = null;
        String id = selectedId(generatorSelector, allGenerators);
        if (graphGenerators.contains(id)) {
            GraphMazeGenerationInput input = new GraphMazeGenerationInput(size, size, System.nanoTime());
            @SuppressWarnings("unchecked")
            GraphMazeGenerator<Integer> algorithm = (GraphMazeGenerator<Integer>)
                    module("algorithm.graph.Integer." + id, GraphMazeGenerator.class);
            startAlgorithm(id, input, () -> algorithm.generate(input), () -> new MazeEventReducer(size, size, true));
        } else {
            ArrayMazeGenerationInput input = new ArrayMazeGenerationInput(size, size, System.nanoTime());
            ArrayMazeGenerator algorithm = module("algorithm.maze.Boolean." + id, ArrayMazeGenerator.class);
            startAlgorithm(id, input, () -> algorithm.generate(input), () -> new MazeEventReducer(size, size, false));
        }
    }

    @FXML
    public void handleSolve() {
        MazeSnapshot selectedSnapshot = selectedAlgorithmSnapshot();
        GridMaze inputMaze = gridMaze(selectedSnapshot);
        if (isRunning() || inputMaze == null) {
            return;
        }
        solving = true;
        selectedOperation = Operation.SOLVE;
        String id = selectedId(pathfinderSelector, arrayPathfinders);
        ArrayMazePathInput input = new ArrayMazePathInput(inputMaze, inputMaze.entrance(), inputMaze.exit());
        ArrayMazePathfinder algorithm = module("algorithm.maze.Boolean." + id, ArrayMazePathfinder.class);
        startAlgorithm(id, input, () -> algorithm.findPath(input),
                () -> new MazeEventReducer(selectedSnapshot.rows(), selectedSnapshot.columns(), false));
    }

    @FXML
    private void handleApplyAlgorithmResult() {
        if (isRunning() || algorithmResultSnapshot == null) {
            return;
        }
        MazeSnapshot result = algorithmResultSnapshot;
        applyStructureState(result, false);
        updateControlState();
        refreshStatsDisplay();
        logI18n("message.maze.result_applied");
    }

    @FXML
    private void handleApplySize() {
        invalidateExecutionForInputChange();
        size = normalizeOdd((int) sizeSlider.getValue());
        sizeSlider.setValue(size);
        generatedMaze = null;
        algorithmResultSnapshot = null;
        solving = false;
        selectedOperation = Operation.GENERATE;
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
    protected void onAlgorithmFinished(ExecutionResult result) {
        super.onAlgorithmFinished(result);
        Object output = result.output().orElse(null);
        if (output instanceof ArrayMazeGenerationOutput generated) {
            algorithmResultSnapshot = snapshot(generated.maze());
            renderViewState(completedViewState(algorithmResultSnapshot, java.util.Set.of()));
        } else if (output instanceof GraphMazeGenerationOutput generated) {
            algorithmResultSnapshot = snapshot(generated);
            renderViewState(completedViewState(algorithmResultSnapshot, java.util.Set.of()));
        } else if (output instanceof ArrayMazePathOutput pathOutput) {
            MazeSnapshot input = selectedAlgorithmSnapshot();
            renderViewState(completedViewState(input, new java.util.LinkedHashSet<>(pathOutput.path())));
        }
        updateControlState();
        logI18n("message.execution.finished");
    }

    @Override
    protected String formatStatsMessage() {
        String structureName = I18N.text("label.maze.structure.array");
        if (!solving && graphGenerators.contains(selectedId(generatorSelector, allGenerators))) {
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
        algorithmResultSnapshot = null;
        algorithmInputSnapshot = null;
        solving = false;
        selectedOperation = Operation.GENERATE;
        renderEmpty();
        updateControlState();
    }

    private void renderEmpty() {
        renderStructureState(MazeViewState.empty(size, size, structure == Structure.GRAPH));
    }

    @Override
    public StructureSnapshot<MazeSnapshot> captureStructureSnapshot() {
        return StructureSnapshot.create(moduleId(), mazeSnapshot());
    }

    @Override
    public void restoreStructureSnapshot(StructureSnapshot<MazeSnapshot> snapshot) {
        if (!moduleId().equals(snapshot.moduleId())) {
            throw new IllegalArgumentException("snapshot belongs to module " + snapshot.moduleId());
        }
        invalidateExecutionForStructureChange();
        algorithmResultSnapshot = null;
        applyStructureState(snapshot.state(), true);
        refreshStatsDisplay();
    }

    @Override
    public void useSnapshotAsAlgorithmInput(StructureSnapshot<MazeSnapshot> snapshot) {
        if (!moduleId().equals(snapshot.moduleId())) {
            throw new IllegalArgumentException("snapshot belongs to module " + snapshot.moduleId());
        }
        algorithmInputSnapshot = snapshot;
        invalidateExecutionForInputChange();
        updateControlState();
    }

    @Override
    public void useCurrentStructureAsAlgorithmInput() {
        algorithmInputSnapshot = null;
        invalidateExecutionForInputChange();
        updateControlState();
    }

    @Override
    public String algorithmInputSnapshotId() {
        return algorithmInputSnapshot == null ? null : algorithmInputSnapshot.id();
    }

    @Override
    protected boolean algorithmInputTracksCurrentStructure() {
        return algorithmInputSnapshot == null;
    }

    @Override
    protected void restoreAlgorithmState() {
        if (latestViewState() != null) {
            super.restoreAlgorithmState();
            return;
        }
        renderViewState(viewState(selectedAlgorithmSnapshot()));
    }

    @Override
    public String describeStructureSnapshot(MazeSnapshot state) {
        long openCells = state.openCells().stream().filter(Boolean::booleanValue).count();
        return I18N.text("snapshot.maze.detail", state.rows(), state.columns(), openCells);
    }

    private MazeSnapshot snapshot(GridMaze maze) {
        return new MazeSnapshot(
                maze.rows(),
                maze.columns(),
                maze.openCells(),
                cell(maze.entrance()),
                cell(maze.exit()),
                List.of(),
                false);
    }

    private MazeSnapshot snapshot(GraphMazeGenerationOutput output) {
        java.util.Map<Long, Integer> valuesById = output.graph().vertices().stream()
                .collect(java.util.stream.Collectors.toMap(
                        com.majortom.algorithms.core.snapshot.GraphSnapshot.Vertex::id,
                        com.majortom.algorithms.core.snapshot.GraphSnapshot.Vertex::value));
        List<MazeSnapshot.Edge> edges = output.graph().edges().stream()
                .map(edge -> new MazeSnapshot.Edge(valuesById.get(edge.fromId()), valuesById.get(edge.toId())))
                .toList();
        return new MazeSnapshot(
                output.rows(),
                output.columns(),
                java.util.Collections.nCopies(output.rows() * output.columns(), true),
                null,
                null,
                edges,
                true);
    }

    private MazeViewState completedViewState(MazeSnapshot snapshot, java.util.Set<com.majortom.algorithms.library.maze.GridPoint> path) {
        MazeViewState state = MazeViewState.source(snapshot);
        return new MazeViewState(
                state.rows(),
                state.columns(),
                state.openCells(),
                path,
                state.entrance(),
                state.exit(),
                state.graphEdges(),
                state.graphBased(),
                true);
    }

    private MazeSnapshot mazeSnapshot() {
        if (generatedMaze != null) {
            return new MazeSnapshot(generatedMaze.rows(), generatedMaze.columns(), generatedMaze.openCells(),
                    cell(generatedMaze.entrance()), cell(generatedMaze.exit()), List.of(), false);
        }
        MazeViewState latest = latestStructureState();
        if (latest == null) latest = MazeViewState.empty(size, size, structure == Structure.GRAPH);
        return new MazeSnapshot(latest.rows(), latest.columns(), latest.openCells(),
                cell(latest.entrance()), cell(latest.exit()), latest.graphEdges().stream()
                .map(edge -> new MazeSnapshot.Edge(edge.from(), edge.to())).toList(), latest.graphBased());
    }

    private MazeSnapshot snapshotFromView(MazeViewState state) {
        return new MazeSnapshot(state.rows(), state.columns(), state.openCells(),
                cell(state.entrance()), cell(state.exit()), state.graphEdges().stream()
                .map(edge -> new MazeSnapshot.Edge(edge.from(), edge.to())).toList(), state.graphBased());
    }

    private MazeSnapshot selectedAlgorithmSnapshot() {
        return algorithmInputSnapshot == null ? mazeSnapshot() : algorithmInputSnapshot.state();
    }

    private GridMaze gridMaze(MazeSnapshot state) {
        if (state == null || state.graphBased() || state.entrance() == null || state.exit() == null) {
            return null;
        }
        return new GridMaze(state.rows(), state.columns(), state.openCells(),
                point(state.entrance()), point(state.exit()));
    }

    private void applyStructureState(MazeSnapshot state, boolean render) {
        applyingStructureState = true;
        try {
            structure = state.graphBased() ? Structure.GRAPH : Structure.ARRAY;
            if (structureSelector != null) {
                structureSelector.getSelectionModel().select(structure == Structure.GRAPH ? 1 : 0);
            }
            size = state.rows();
            if (sizeSlider != null) {
                sizeSlider.setValue(Math.max(sizeSlider.getMin(), Math.min(sizeSlider.getMax(), size)));
            }
            if (sizeValueLabel != null) {
                sizeValueLabel.setText(size + " x " + state.columns());
            }
        } finally {
            applyingStructureState = false;
        }
        generatedMaze = gridMaze(state);
        solving = false;
        MazeViewState view = viewState(state);
        if (render) {
            renderStructureState(view);
        } else {
            storeStructureState(view);
        }
        updateControlState();
    }

    private MazeViewState viewState(MazeSnapshot state) {
        return MazeViewState.source(state);
    }

    private MazeSnapshot.Cell cell(com.majortom.algorithms.library.maze.GridPoint point) {
        return point == null ? null : new MazeSnapshot.Cell(point.row(), point.column());
    }

    private com.majortom.algorithms.library.maze.GridPoint point(MazeSnapshot.Cell cell) {
        return cell == null ? null : new com.majortom.algorithms.library.maze.GridPoint(cell.row(), cell.column());
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
        if (applyResultBtn != null) applyResultBtn.textProperty().bind(I18N.createStringBinding("action.maze.apply_result"));
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
                () -> labels(allGenerators), I18N.localeProperty()));
        pathfinderSelector.itemsProperty().bind(Bindings.createObjectBinding(
                () -> labels(arrayPathfinders), I18N.localeProperty()));
        structureSelector.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.intValue() < 0 || isRunning() || applyingStructureState) {
                return;
            }
            structure = Structure.ARRAY;
            if (newValue.intValue() == 1) {
                structure = Structure.GRAPH;
            }
            invalidateExecutionForStructureChange();
            generatedMaze = null;
            algorithmResultSnapshot = null;
            solving = false;
            renderEmpty();
            updateControlState();
        });
        generatorSelector.getSelectionModel().selectedIndexProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue.intValue() >= 0 && !isRunning()) {
                        selectedOperation = Operation.GENERATE;
                        updateControlState();
                    }
                });
        pathfinderSelector.getSelectionModel().selectedIndexProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue.intValue() >= 0 && !isRunning()) {
                        selectedOperation = Operation.SOLVE;
                        updateControlState();
                    }
                });
        Platform.runLater(() -> {
            structureSelector.getSelectionModel().selectFirst();
            selectFirstAlgorithms();
        });
    }

    private javafx.collections.ObservableList<String> labels(List<String> ids) {
        javafx.collections.ObservableList<String> labels = FXCollections.observableArrayList();
        for (String id : ids) {
            labels.add(AlgorithmLabels.text(id));
        }
        return labels;
    }

    private void selectFirstAlgorithms() {
        if (!generatorSelector.getItems().isEmpty()) generatorSelector.getSelectionModel().selectFirst();
        if (!pathfinderSelector.getItems().isEmpty()) pathfinderSelector.getSelectionModel().selectFirst();
        selectedOperation = Operation.GENERATE;
    }

    private String selectedId(ComboBox<String> comboBox, List<String> ids) {
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("algorithm id list must not be empty");
        }
        if (comboBox == null) {
            return ids.getFirst();
        }
        int index = comboBox.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= ids.size()) index = 0;
        return ids.get(index);
    }

    private boolean selectAlgorithm(ComboBox<String> comboBox, List<String> ids, String algorithmId) {
        int index = ids.indexOf(algorithmId);
        if (index < 0) {
            return false;
        }
        if (comboBox != null) {
            comboBox.getSelectionModel().select(index);
        }
        return true;
    }

    private void updateControlState() {
        boolean solveAvailable = gridMaze(selectedAlgorithmSnapshot()) != null;
        pathfinderSelector.setDisable(false);
        solveBtn.setDisable(!solveAvailable);
        if (applyResultBtn != null) {
            applyResultBtn.setDisable(isRunning() || algorithmResultSnapshot == null);
        }
        if (startBtn != null) {
            startBtn.setDisable(selectedOperation == Operation.SOLVE && !solveAvailable);
        }
    }

    private int normalizeOdd(int value) {
        if (value % 2 == 0) return value + 1;
        return value;
    }
}
