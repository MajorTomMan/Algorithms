package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.snapshot.BinaryTreeSnapshot;
import com.majortom.algorithms.core.snapshot.StructureSnapshot;
import com.majortom.algorithms.visualization.structure.StructureSnapshotSupport;
import com.majortom.algorithms.visualization.structure.SnapshotAlgorithmInputSupport;
import com.majortom.algorithms.library.basic.tree.AVLTreeNode;
import com.majortom.algorithms.library.structure.MutableAvlTree;
import com.majortom.algorithms.library.tree.AvlNodeSnapshot;
import com.majortom.algorithms.library.tree.AvlTreeCommands;
import com.majortom.algorithms.library.tree.AvlTreeInput;
import com.majortom.algorithms.utils.EffectUtils;
import com.majortom.algorithms.visualization.VisualizationActionType;
import com.majortom.algorithms.visualization.algorithm.AlgorithmLabels;
import com.majortom.algorithms.visualization.impl.visualizer.TreeVisualizer;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.module.AlgorithmSelectionSupport;
import com.majortom.algorithms.visualization.runtime.tree.AvlTreeEventReducer;
import com.majortom.algorithms.visualization.runtime.tree.AvlTreeViewState;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Set;

public final class TreeController extends BaseModuleController<AvlTreeViewState>
        implements AlgorithmSelectionSupport, StructureSnapshotSupport<BinaryTreeSnapshot<Integer>>, SnapshotAlgorithmInputSupport<BinaryTreeSnapshot<Integer>> {

    private final MutableAvlTree<Integer> tree;
    private StructureSnapshot<BinaryTreeSnapshot<Integer>> algorithmInputSnapshot;

    @FXML private Label structureLabel;
    @FXML private ComboBox<String> structureSelector;
    @FXML private Label algorithmLabel;
    @FXML private Label operationsSectionLabel;
    @FXML private ComboBox<String> algorithmSelector;
    @FXML private TextField valueField;
    @FXML private TextField updateOldValueField;
    @FXML private TextField updateNewValueField;
    @FXML private Button insertBtn;
    @FXML private Button deleteBtn;
    @FXML private Button findBtn;
    @FXML private Button updateBtn;
    @FXML private Button randomBtn;

    @SuppressWarnings("unchecked")
    public TreeController() {
        super(new TreeVisualizer(), "/fxml/TreeControls.fxml");
        tree = module("structure.tree.Integer", MutableAvlTree.class);
        Random random = new Random();
        while (tree.size() < 12) {
            tree.insert(random.nextInt(100));
        }
        renderStructureState(currentStructureState());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);
        bindSelectors();
        EffectUtils.applyDynamicEffect(insertBtn, deleteBtn, findBtn, updateBtn, randomBtn);
    }

    @FXML
    private void handleInsert() {
        List<Integer> values = parseValues(valueField.getText());
        if (values.isEmpty()) {
            return;
        }
        dispatchVisualizerAction(VisualizationActionType.TREE_INSERT, java.util.Map.of("count", values.size()));
        if (executeStructureOperation("insert", () -> {
            for (Integer value : values) {
                tree.insert(value);
            }
            return null;
        })) {
            renderStructureState(currentStructureState());
            refreshStatsDisplay();
        }
    }

    @FXML
    private void handleDelete() {
        List<Integer> values = parseValues(valueField.getText());
        if (values.isEmpty()) {
            return;
        }
        dispatchVisualizerAction(VisualizationActionType.TREE_DELETE, java.util.Map.of("count", values.size()));
        if (executeStructureOperation("remove", () -> {
            for (Integer value : values) {
                tree.remove(value);
            }
            return null;
        })) {
            renderStructureState(currentStructureState());
            refreshStatsDisplay();
        }
    }

    @FXML
    private void handleFind() {
        if (isRunning()) {
            logI18n("message.error.operation_running");
            return;
        }
        Integer target = parseValue(valueField);
        if (target == null) {
            return;
        }
        AvlTreeViewState state = currentStructureState();
        SearchResult result = findNode(state.root(), target, null, new ArrayList<>());
        if (result == null) {
            logI18n("message.tree.not_found", target);
            return;
        }
        renderViewState(new AvlTreeViewState(
                state.root(), state.values(), result.id(), result.value(), result.parentId(), null,
                new LinkedHashSet<>(result.ancestors()), AvlTreeViewState.Phase.VISITING,
                null, null, null, false));
        logI18n("message.tree.found", target);
    }

    @FXML
    private void handleUpdate() {
        if (isRunning()) {
            logI18n("message.error.operation_running");
            return;
        }
        Integer oldValue = parseValue(updateOldValueField);
        Integer newValue = parseValue(updateNewValueField);
        if (oldValue == null || newValue == null) {
            return;
        }
        if (tree.find(oldValue) == null) {
            logI18n("message.tree.not_found", oldValue);
            return;
        }
        if (!oldValue.equals(newValue) && tree.find(newValue) != null) {
            logI18n("message.tree.duplicate", newValue);
            return;
        }
        if (oldValue.equals(newValue)) {
            return;
        }
        dispatchVisualizerAction(VisualizationActionType.TREE_DELETE, java.util.Map.of("count", 2));
        if (executeStructureOperation("update", () -> {
            tree.remove(oldValue);
            tree.insert(newValue);
            return null;
        })) {
            renderStructureState(currentStructureState());
            refreshStatsDisplay();
            logI18n("message.tree.updated", oldValue, newValue);
        }
    }

    @FXML
    private void handleRandom() {
        valueField.setText(String.valueOf(new Random().nextInt(100)));
        dispatchVisualizerAction(VisualizationActionType.TREE_RANDOM);
    }

    private List<Integer> parseValues(String input) {
        List<Integer> result = new ArrayList<>();
        try {
            for (String part : input.split("[,，]")) {
                if (!part.isBlank()) {
                    result.add(Integer.parseInt(part.trim()));
                }
            }
        } catch (NumberFormatException exception) {
            appendLog(I18N.text("message.error.invalid_tree_input"));
            return List.of();
        }
        if (result.isEmpty()) {
            appendLog(I18N.text("message.error.invalid_tree_input"));
            return List.of();
        }
        return List.copyOf(result);
    }

    private Integer parseValue(TextField field) {
        try {
            return Integer.valueOf(field.getText().trim());
        } catch (RuntimeException exception) {
            logI18n("message.error.invalid_tree_input");
            return null;
        }
    }

    private SearchResult findNode(AvlNodeSnapshot node, int target, Long parentId, List<Long> path) {
        if (node == null) {
            return null;
        }
        List<Long> nextPath = new ArrayList<>(path);
        nextPath.add(node.id());
        if (target == node.value()) {
            nextPath.removeLast();
            return new SearchResult(node.id(), node.value(), parentId, nextPath);
        }
        if (target < node.value()) {
            return findNode(node.left(), target, node.id(), nextPath);
        }
        return findNode(node.right(), target, node.id(), nextPath);
    }

    private record SearchResult(long id, int value, Long parentId, List<Long> ancestors) {
    }

    @Override
    public void handleAlgorithmStart() {
        if (isRunning()) {
            return;
        }
        StructureSnapshot<BinaryTreeSnapshot<Integer>> inputSnapshot =
                algorithmInputSnapshot == null ? captureStructureSnapshot() : algorithmInputSnapshot;
        AvlNodeSnapshot inputRoot = toAvlSnapshot(inputSnapshot.state().root());
        AvlTreeInput input = inputRoot == null
                ? AvlTreeInput.fromValues(List.of(), List.of())
                : AvlTreeInput.fromSnapshot(inputRoot, List.of());
        AvlTreeCommands algorithm = module("algorithm.tree.Integer.tree-avl", AvlTreeCommands.class);
        startAlgorithm("tree-avl", input, () -> algorithm.execute(input), AvlTreeEventReducer::new);
    }

    @Override
    public boolean selectAlgorithm(String algorithmId) {
        if (!"tree-avl".equals(algorithmId)) {
            return false;
        }
        if (algorithmSelector != null) {
            algorithmSelector.getSelectionModel().selectFirst();
        }
        return true;
    }

    @Override
    public StructureSnapshot<BinaryTreeSnapshot<Integer>> captureStructureSnapshot() {
        return StructureSnapshot.create(moduleId(), new BinaryTreeSnapshot<>(snapshotTreeNode(tree.raw()), tree.size()));
    }

    @Override
    public void restoreStructureSnapshot(StructureSnapshot<BinaryTreeSnapshot<Integer>> snapshot) {
        if (!moduleId().equals(snapshot.moduleId())) {
            throw new IllegalArgumentException("snapshot belongs to module " + snapshot.moduleId());
        }
        tree.restore(restoreTreeNode(snapshot.state().root()));
        invalidateExecutionForStructureChange();
        renderStructureState(currentStructureState());
        refreshStatsDisplay();
    }

    @Override
    public void useSnapshotAsAlgorithmInput(StructureSnapshot<BinaryTreeSnapshot<Integer>> snapshot) {
        if (!moduleId().equals(snapshot.moduleId())) throw new IllegalArgumentException("snapshot belongs to module " + snapshot.moduleId());
        algorithmInputSnapshot = snapshot;
        invalidateExecutionForInputChange();
    }

    @Override
    public void useCurrentStructureAsAlgorithmInput() {
        algorithmInputSnapshot = null;
        invalidateExecutionForInputChange();
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
        if (algorithmInputSnapshot == null) {
            renderViewState(currentStructureState());
            return;
        }
        AvlNodeSnapshot root = toAvlSnapshot(algorithmInputSnapshot.state().root());
        renderViewState(new AvlTreeViewState(root, values(root), null, null, null, null, Set.of(),
                AvlTreeViewState.Phase.IDLE, null, null, null, false));
    }


    @Override
    public String describeStructureSnapshot(BinaryTreeSnapshot<Integer> state) {
        int height = state.root() == null ? 0 : state.root().height();
        return I18N.text("snapshot.tree.detail", state.size(), height);
    }

    private AvlTreeViewState currentStructureState() {
        AvlNodeSnapshot root = snapshotNode(tree.raw());
        return new AvlTreeViewState(root, values(root), null, null, null, null, Set.of(),
                AvlTreeViewState.Phase.IDLE, null, null, null, false);
    }

    private BinaryTreeSnapshot.Node<Integer> snapshotTreeNode(AVLTreeNode<Integer> node) {
        if (node == null) {
            return null;
        }
        return new BinaryTreeSnapshot.Node<>(node.id, node.data, node.height,
                snapshotTreeNode(left(node)), snapshotTreeNode(right(node)));
    }

    private AvlNodeSnapshot toAvlSnapshot(BinaryTreeSnapshot.Node<Integer> snapshot) {
        if (snapshot == null) return null;
        return new AvlNodeSnapshot(snapshot.id(), snapshot.value(), snapshot.height(),
                toAvlSnapshot(snapshot.left()), toAvlSnapshot(snapshot.right()));
    }

    private AVLTreeNode<Integer> restoreTreeNode(BinaryTreeSnapshot.Node<Integer> snapshot) {
        if (snapshot == null) {
            return null;
        }
        AVLTreeNode<Integer> node = new AVLTreeNode<>(snapshot.id(), snapshot.value());
        node.height = snapshot.height();
        node.left = restoreTreeNode(snapshot.left());
        node.right = restoreTreeNode(snapshot.right());
        return node;
    }

    private AvlNodeSnapshot snapshotNode(AVLTreeNode<Integer> node) {
        if (node == null) {
            return null;
        }
        return new AvlNodeSnapshot(node.id, node.data, node.height,
                snapshotNode(left(node)), snapshotNode(right(node)));
    }

    private List<Integer> currentValues() {
        return values(snapshotNode(tree.raw()));
    }

    private List<Integer> values(AvlNodeSnapshot root) {
        List<Integer> result = new ArrayList<>();
        collectValues(root, result);
        return List.copyOf(result);
    }

    private void collectValues(AvlNodeSnapshot node, List<Integer> values) {
        if (node == null) {
            return;
        }
        collectValues(node.left(), values);
        values.add(node.value());
        collectValues(node.right(), values);
    }

    @SuppressWarnings("unchecked")
    private AVLTreeNode<Integer> left(AVLTreeNode<Integer> node) {
        return node == null ? null : (AVLTreeNode<Integer>) node.left;
    }

    @SuppressWarnings("unchecked")
    private AVLTreeNode<Integer> right(AVLTreeNode<Integer> node) {
        return node == null ? null : (AVLTreeNode<Integer>) node.right;
    }

    @Override
    protected void onAlgorithmFinished() {
        super.onAlgorithmFinished();
        logI18n("message.execution.finished");
    }

    @Override
    protected String formatStatsMessage() {
        AVLTreeNode<Integer> root = tree.raw();
        int height = root == null ? 0 : root.height;
        return String.format("%s | %s | %s",
                I18N.text("stats.size", tree.size()), I18N.text("stats.height", height),
                formatMetric("stats.action", stats.metric("nodes.inserted")
                        + stats.metric("nodes.removed") + stats.metric("rotations")));
    }

    @Override
    protected void onResetData() {
        tree.restore(null);
        renderStructureState(currentStructureState());
    }

    @Override
    protected void setupI18n() {
        if (structureLabel != null) {
            structureLabel.textProperty().bind(I18N.createStringBinding("label.common.structure"));
        }
        if (algorithmLabel != null) {
            algorithmLabel.textProperty().bind(I18N.createStringBinding("label.common.algorithm"));
        }
        if (operationsSectionLabel != null) {
            operationsSectionLabel.textProperty().bind(I18N.createStringBinding("label.panel.operations"));
        }
        bindPrompt(valueField, "prompt.tree.value");
        bindPrompt(updateOldValueField, "prompt.tree.old_value");
        bindPrompt(updateNewValueField, "prompt.tree.new_value");
        bindButton(insertBtn, "action.tree.insert");
        bindButton(deleteBtn, "action.tree.delete");
        bindButton(findBtn, "action.tree.find");
        bindButton(updateBtn, "action.tree.update");
        bindButton(randomBtn, "action.tree.random");
    }

    @Override
    protected String moduleId() {
        return "tree";
    }

    private void bindSelectors() {
        structureSelector.itemsProperty().bind(Bindings.createObjectBinding(
                () -> FXCollections.observableArrayList(I18N.text("label.tree.structure.avl")),
                I18N.localeProperty()));
        algorithmSelector.itemsProperty().bind(Bindings.createObjectBinding(
                () -> FXCollections.observableArrayList(I18N.text(AlgorithmLabels.key("tree-avl"))),
                I18N.localeProperty()));
        Platform.runLater(() -> {
            structureSelector.getSelectionModel().selectFirst();
            algorithmSelector.getSelectionModel().selectFirst();
        });
    }

    private void bindPrompt(TextField field, String key) {
        if (field != null) {
            field.promptTextProperty().bind(I18N.createStringBinding(key));
        }
    }

    private void bindButton(Button button, String key) {
        if (button != null) {
            button.textProperty().bind(I18N.createStringBinding(key));
        }
    }
}
