package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.snapshot.GeneralTreeSnapshot;
import com.majortom.algorithms.core.snapshot.StructureSnapshot;
import com.majortom.algorithms.library.basic.tree.GeneralTreeNode;
import com.majortom.algorithms.library.basic.tree.Tree;
import com.majortom.algorithms.library.basic.tree.AVLTree;
import com.majortom.algorithms.library.tree.TreeAlgorithm;
import com.majortom.algorithms.library.tree.AvlTreeCommands;
import com.majortom.algorithms.utils.EffectUtils;
import com.majortom.algorithms.visualization.algorithm.AlgorithmCatalog;
import com.majortom.algorithms.visualization.algorithm.AlgorithmLabels;
import com.majortom.algorithms.visualization.impl.visualizer.TreeVisualizer;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.module.AlgorithmSelectionSupport;
import com.majortom.algorithms.visualization.runtime.tree.TreeEventReducer;
import com.majortom.algorithms.visualization.runtime.tree.TreeViewState;
import com.majortom.algorithms.visualization.structure.SnapshotAlgorithmInputSupport;
import com.majortom.algorithms.visualization.structure.StructureSnapshotSupport;
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
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public final class TreeController extends BaseModuleController<TreeViewState>
        implements AlgorithmSelectionSupport, StructureSnapshotSupport<GeneralTreeSnapshot<Integer>>,
        SnapshotAlgorithmInputSupport<GeneralTreeSnapshot<Integer>> {

    private final List<String> algorithmIds = AlgorithmCatalog.treeAlgorithms();
    private Tree<Integer> tree;
    private StructureSnapshot<GeneralTreeSnapshot<Integer>> algorithmInputSnapshot;
    private Consumer<NodeSelection> selectionListener = ignored -> { };

    @FXML private Label structureLabel;
    @FXML private ComboBox<String> structureSelector;
    @FXML private Label algorithmLabel;
    @FXML private Label operationsSectionLabel;
    @FXML private ComboBox<String> algorithmSelector;
    @FXML private TextField valueField;
    @FXML private TextField parentIdField;
    @FXML private TextField nodeIdField;
    @FXML private Button addRootBtn;
    @FXML private Button addChildBtn;
    @FXML private Button deleteBtn;
    @FXML private Button findBtn;
    @FXML private Button updateBtn;
    @FXML private Button randomBtn;

    @SuppressWarnings("unchecked")
    public TreeController() {
        super(new TreeVisualizer(), "/fxml/TreeControls.fxml");
        tree = module("structure.tree.Integer", Tree.class);
        initializeSampleTree();
        ((TreeVisualizer) visualizer).setSelectionListener(this::handleVisualSelection);
        renderStructureState(currentStructureState());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);
        bindSelectors();
        EffectUtils.applyDynamicEffect(addRootBtn, addChildBtn, deleteBtn, findBtn, updateBtn, randomBtn);
    }

    public record NodeSelection(long id, int value, Long parentId, int childCount, int depth) { }

    public void setSelectionListener(Consumer<NodeSelection> listener) {
        selectionListener = listener == null ? ignored -> { } : listener;
    }

    private void handleVisualSelection(long nodeId) {
        GeneralTreeNode<Integer> node = tree.findById(nodeId);
        if (node == null) {
            return;
        }
        nodeIdField.setText(Long.toString(nodeId));
        valueField.setText(Integer.toString(node.getValue()));
        GeneralTreeNode<Integer> parent = parentOf(tree.root(), node);
        if (parent != null) {
            parentIdField.setText(Long.toString(parent.getId()));
        }
        selectionListener.accept(new NodeSelection(
                nodeId, node.getValue(), parent == null ? null : parent.getId(),
                node.getChildren().size(), depthOf(tree.root(), node, 0)));
    }

    private GeneralTreeNode<Integer> parentOf(GeneralTreeNode<Integer> root, GeneralTreeNode<Integer> target) {
        if (root == null) return null;
        for (GeneralTreeNode<Integer> child : root.getChildren()) {
            if (child == target) return root;
            GeneralTreeNode<Integer> found = parentOf(child, target);
            if (found != null) return found;
        }
        return null;
    }

    private int depthOf(GeneralTreeNode<Integer> root, GeneralTreeNode<Integer> target, int depth) {
        if (root == null) return -1;
        if (root == target) return depth;
        for (GeneralTreeNode<Integer> child : root.getChildren()) {
            int found = depthOf(child, target, depth + 1);
            if (found >= 0) return found;
        }
        return -1;
    }

    @FXML
    private void handleAddRoot() {
        Integer value = parseValue(valueField);
        if (value == null || tree.root() != null) {
            if (tree.root() != null) {
                logI18n("message.tree.root_exists");
            }
            return;
        }
        if (executeStructureOperation("add-root", () -> tree.addRoot(value))) {
            refreshStructureView();
        }
    }

    @FXML
    private void handleAddChild() {
        Integer value = parseValue(valueField);
        Long parentId = parseId(parentIdField);
        if (value == null || parentId == null) {
            return;
        }
        GeneralTreeNode<Integer> parent = tree.findById(parentId);
        if (parent == null) {
            logI18n("message.tree.node_id_not_found", parentId);
            return;
        }
        if (executeStructureOperation("add-child", () -> tree.addChild(parent, value))) {
            refreshStructureView();
        }
    }

    @FXML
    private void handleDelete() {
        Long nodeId = parseId(nodeIdField);
        if (nodeId == null) {
            return;
        }
        GeneralTreeNode<Integer> node = tree.findById(nodeId);
        if (node == null) {
            logI18n("message.tree.node_id_not_found", nodeId);
            return;
        }
        if (executeStructureOperation("remove", () -> tree.remove(node))) {
            refreshStructureView();
        }
    }

    @FXML
    private void handleFind() {
        if (isRunning()) {
            logI18n("message.error.operation_running");
            return;
        }
        Integer value = parseValue(valueField);
        if (value == null) {
            return;
        }
        GeneralTreeNode<Integer> node = tree.findFirstByValue(value);
        if (node == null) {
            logI18n("message.tree.not_found", value);
            return;
        }
        logI18n("message.tree.found_with_id", value, node.getId());
    }

    @FXML
    private void handleUpdate() {
        if (isRunning()) {
            logI18n("message.error.operation_running");
            return;
        }
        Long nodeId = parseId(nodeIdField);
        Integer value = parseValue(valueField);
        if (nodeId == null || value == null) {
            return;
        }
        GeneralTreeNode<Integer> node = tree.findById(nodeId);
        if (node == null) {
            logI18n("message.tree.node_id_not_found", nodeId);
            return;
        }
        if (executeStructureOperation("update", () -> tree.set(node, value))) {
            refreshStructureView();
        }
    }

    @FXML
    private void handleRandom() {
        valueField.setText(Integer.toString(new Random().nextInt(100)));
    }

    @Override
    public void handleAlgorithmStart() {
        if (isRunning()) {
            return;
        }
        StructureSnapshot<GeneralTreeSnapshot<Integer>> inputSnapshot =
                algorithmInputSnapshot == null ? captureStructureSnapshot() : algorithmInputSnapshot;
        List<Integer> values = snapshotValues(inputSnapshot.state());
        String algorithmId = selectedAlgorithmId();
        if (algorithmId == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        TreeAlgorithm<Integer> algorithm = (TreeAlgorithm<Integer>)
                module("algorithm.tree.Integer." + algorithmId, TreeAlgorithm.class);
        if (!(algorithm instanceof AvlTreeCommands avl)) {
            throw new IllegalStateException("Unsupported tree algorithm: " + algorithm.getClass().getName());
        }
        startAlgorithm(algorithmId, values, () -> {
            AVLTree<Integer> runtimeTree = new AVLTree<>();
            for (Integer value : values) {
                runtimeTree.insert(value);
            }
            avl.execute(runtimeTree, List.of());
            return null;
        }, () -> new TreeEventReducer(TreeViewState.empty(TreeViewState.Kind.BINARY)));
    }

    @Override
    public boolean selectAlgorithm(String algorithmId) {
        int index = algorithmIds.indexOf(algorithmId);
        if (index < 0) {
            return false;
        }
        if (algorithmSelector != null) {
            algorithmSelector.getSelectionModel().select(index);
        }
        return true;
    }

    @Override
    public StructureSnapshot<GeneralTreeSnapshot<Integer>> captureStructureSnapshot() {
        return StructureSnapshot.create(moduleId(), currentSnapshot());
    }

    @Override
    public void restoreStructureSnapshot(StructureSnapshot<GeneralTreeSnapshot<Integer>> snapshot) {
        if (!moduleId().equals(snapshot.moduleId())) {
            throw new IllegalArgumentException("snapshot belongs to module " + snapshot.moduleId());
        }
        tree = Tree.fromSnapshot(snapshot.state());
        invalidateExecutionForStructureChange();
        refreshStructureView();
    }

    @Override
    public void useSnapshotAsAlgorithmInput(StructureSnapshot<GeneralTreeSnapshot<Integer>> snapshot) {
        if (!moduleId().equals(snapshot.moduleId())) {
            throw new IllegalArgumentException("snapshot belongs to module " + snapshot.moduleId());
        }
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
        renderViewState(TreeViewState.general(algorithmInputSnapshot.state()));
    }

    @Override
    public String describeStructureSnapshot(GeneralTreeSnapshot<Integer> state) {
        return I18N.text("snapshot.tree.detail", state.size(), height(state.root()));
    }

    private void initializeSampleTree() {
        GeneralTreeNode<Integer> root = tree.addRoot(50);
        GeneralTreeNode<Integer> left = tree.addChild(root, 30);
        GeneralTreeNode<Integer> middle = tree.addChild(root, 70);
        tree.addChild(root, 90);
        tree.addChild(left, 10);
        tree.addChild(left, 40);
        tree.addChild(middle, 60);
        tree.addChild(middle, 80);
    }

    private void refreshStructureView() {
        renderStructureState(currentStructureState());
        refreshStatsDisplay();
    }

    private TreeViewState currentStructureState() {
        return TreeViewState.general(currentSnapshot());
    }

    private GeneralTreeSnapshot<Integer> currentSnapshot() {
        return new GeneralTreeSnapshot<>(snapshotNode(tree.root()), tree.size());
    }

    private GeneralTreeSnapshot.Node<Integer> snapshotNode(GeneralTreeNode<Integer> node) {
        if (node == null) {
            return null;
        }
        List<GeneralTreeSnapshot.Node<Integer>> children = node.getChildren().stream().map(this::snapshotNode).toList();
        return new GeneralTreeSnapshot.Node<>(node.getId(), node.getValue(), children);
    }

    private List<Integer> snapshotValues(GeneralTreeSnapshot<Integer> snapshot) {
        List<Integer> values = new ArrayList<>();
        collectSnapshotValues(snapshot.root(), values);
        return List.copyOf(values);
    }

    private void collectSnapshotValues(GeneralTreeSnapshot.Node<Integer> node, List<Integer> values) {
        if (node == null) {
            return;
        }
        values.add(node.value());
        for (GeneralTreeSnapshot.Node<Integer> child : node.children()) {
            collectSnapshotValues(child, values);
        }
    }

    private int height(GeneralTreeSnapshot.Node<Integer> node) {
        if (node == null) {
            return 0;
        }
        int maxChildHeight = 0;
        for (GeneralTreeSnapshot.Node<Integer> child : node.children()) {
            maxChildHeight = Math.max(maxChildHeight, height(child));
        }
        return maxChildHeight + 1;
    }

    private Integer parseValue(TextField field) {
        try {
            return Integer.valueOf(field.getText().trim());
        } catch (RuntimeException exception) {
            logI18n("message.error.invalid_tree_input");
            return null;
        }
    }

    private Long parseId(TextField field) {
        try {
            long value = Long.parseLong(field.getText().trim());
            if (value <= 0) {
                throw new NumberFormatException("id must be positive");
            }
            return value;
        } catch (RuntimeException exception) {
            logI18n("message.error.invalid_tree_node_id");
            return null;
        }
    }

    @Override
    protected void onAlgorithmFinished(com.majortom.algorithms.core.runtime.ExecutionResult result) {
        super.onAlgorithmFinished(result);
        logI18n("message.execution.finished");
    }

    @Override
    public String structureSummaryText() {
        GeneralTreeSnapshot<Integer> snapshot = currentSnapshot();
        return String.format("Nodes          %d%nHeight         %d%nRoot           %s",
                snapshot.size(), height(snapshot.root()), snapshot.root() == null ? "none" : snapshot.root().value());
    }

    @Override
    public String structurePrimaryCount() {
        return Integer.toString(tree.size());
    }

    @Override
    public String structureSecondaryCount() {
        return Integer.toString(height(currentSnapshot().root()));
    }

    @Override
    protected String formatStatsMessage() {
        return String.format("%s | %s | %s",
                I18N.text("stats.size", tree.size()),
                I18N.text("stats.height", height(currentSnapshot().root())),
                formatMetric("stats.action", stats.metric("nodes.inserted") + stats.metric("nodes.removed")));
    }

    @Override
    protected void onResetData() {
        tree = new Tree<>();
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
        bindPrompt(parentIdField, "prompt.tree.parent_id");
        bindPrompt(nodeIdField, "prompt.tree.node_id");
        bindButton(addRootBtn, "action.tree.add_root");
        bindButton(addChildBtn, "action.tree.add_child");
        bindButton(deleteBtn, "action.tree.delete");
        bindButton(findBtn, "action.tree.find");
        bindButton(updateBtn, "action.tree.update");
        bindButton(randomBtn, "action.tree.random");
    }

    @Override
    protected String moduleId() {
        return "tree";
    }

    private String selectedAlgorithmId() {
        int index = algorithmSelector == null ? 0 : algorithmSelector.getSelectionModel().getSelectedIndex();
        if (index < 0) {
            index = 0;
        }
        return algorithmIds.isEmpty() ? null : algorithmIds.get(Math.min(index, algorithmIds.size() - 1));
    }

    private void bindSelectors() {
        structureSelector.itemsProperty().bind(Bindings.createObjectBinding(
                () -> FXCollections.observableArrayList(I18N.text("label.tree.structure.general")),
                I18N.localeProperty()));
        algorithmSelector.itemsProperty().bind(Bindings.createObjectBinding(() -> {
            javafx.collections.ObservableList<String> labels = FXCollections.observableArrayList();
            for (String id : algorithmIds) {
                labels.add(AlgorithmLabels.text(id));
            }
            return labels;
        }, I18N.localeProperty()));
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
            button.textProperty().bind(Bindings.createStringBinding(
                    () -> I18N.text(key).toUpperCase(java.util.Locale.ROOT), I18N.localeProperty()));
        }
    }
}
