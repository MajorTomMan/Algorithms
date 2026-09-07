package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.snapshot.BinaryTreeSnapshot;
import com.majortom.algorithms.core.snapshot.GeneralTreeSnapshot;
import com.majortom.algorithms.core.snapshot.StructureSnapshot;
import com.majortom.algorithms.core.snapshot.TreeSnapshotState;
import com.majortom.algorithms.library.basic.tree.AVLTree;
import com.majortom.algorithms.library.basic.tree.AVLTreeNode;
import com.majortom.algorithms.library.basic.tree.GeneralTreeNode;
import com.majortom.algorithms.library.basic.tree.Tree;
import com.majortom.algorithms.library.tree.AvlCommand;
import com.majortom.algorithms.library.tree.AvlCommandAlgorithm;
import com.majortom.algorithms.library.tree.AvlNodeSnapshot;
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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public final class TreeController extends BaseModuleController<TreeViewState>
        implements AlgorithmSelectionSupport, StructureSnapshotSupport<TreeSnapshotState<Integer>>,
        SnapshotAlgorithmInputSupport<TreeSnapshotState<Integer>> {

    private Tree<Integer> generalTree;
    private AVLTree<Integer> avlTree;
    private TreeVariant activeVariant = TreeVariant.GENERAL;
    private List<String> algorithmIds = List.of();
    private StructureSnapshot<TreeSnapshotState<Integer>> algorithmInputSnapshot;
    private Consumer<NodeSelection> selectionListener = ignored -> { };
    private Consumer<String> algorithmSelectionListener = ignored -> { };
    private Long selectedNodeId;

    @FXML private Label structureLabel;
    @FXML private ComboBox<String> structureSelector;
    @FXML private Label algorithmLabel;
    @FXML private Label operationsSectionLabel;
    @FXML private ComboBox<String> algorithmSelector;
    @FXML private Label algorithmCommandsHintLabel;
    @FXML private TextArea algorithmCommandsField;
    @FXML private TextField valueField;
    @FXML private Label selectionHintLabel;
    @FXML private Button addRootBtn;
    @FXML private Button addChildBtn;
    @FXML private Button addParentBtn;
    @FXML private Button deleteBtn;
    @FXML private Button updateBtn;
    @FXML private Button randomBtn;

    public TreeController() {
        super(new TreeVisualizer(), "/fxml/TreeControls.fxml");
        generalTree = module("structure.tree.Integer", Tree.class);
        avlTree = module("structure.tree.avl.Integer", AVLTree.class);
        initializeSampleGeneralTree();
        initializeSampleAvlTree();
        treeVisualizer().setSelectionListener(this::handleVisualSelection);
        refreshAlgorithmIds();
        renderStructureState(currentStructureState());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        super.initialize(location, resources);
        bindSelectors();
        EffectUtils.applyDynamicEffect(addRootBtn, addChildBtn, addParentBtn, deleteBtn, updateBtn, randomBtn);
        refreshVariantControls();
    }

    public record NodeSelection(long id, int value, Long parentId, int childCount, int depth) {
    }

    private enum TreeVariant {
        GENERAL,
        AVL
    }

    public void setSelectionListener(Consumer<NodeSelection> listener) {
        if (listener == null) {
            selectionListener = ignored -> { };
        } else {
            selectionListener = listener;
        }
    }

    private void handleVisualSelection(long nodeId) {
        if (activeVariant == TreeVariant.GENERAL) {
            handleGeneralSelection(nodeId);
        } else {
            handleAvlSelection(nodeId);
        }
    }

    private void handleGeneralSelection(long nodeId) {
        GeneralTreeNode<Integer> node = generalTree.findById(nodeId);
        if (node == null) {
            clearNodeSelection();
            return;
        }
        selectedNodeId = nodeId;
        valueField.setText(Integer.toString(node.getValue()));
        GeneralTreeNode<Integer> parent = generalParentOf(generalTree.root(), node);
        Long parentId = null;
        if (parent != null) {
            parentId = parent.getId();
        }
        selectionListener.accept(new NodeSelection(
                nodeId,
                node.getValue(),
                parentId,
                node.getChildren().size(),
                generalDepthOf(generalTree.root(), node, 0)));
        refreshOperationAvailability();
    }

    private void handleAvlSelection(long nodeId) {
        AVLTreeNode<Integer> node = avlNodeById(avlTree.root(), nodeId);
        if (node == null) {
            clearNodeSelection();
            return;
        }
        selectedNodeId = nodeId;
        valueField.setText(Integer.toString(node.getValue()));
        AVLTreeNode<Integer> parent = avlParentOf(avlTree.root(), node);
        Long parentId = null;
        if (parent != null) {
            parentId = parent.getId();
        }
        int childCount = 0;
        if (node.getLeft() != null) {
            childCount++;
        }
        if (node.getRight() != null) {
            childCount++;
        }
        selectionListener.accept(new NodeSelection(
                nodeId,
                node.getValue(),
                parentId,
                childCount,
                avlDepthOf(avlTree.root(), node, 0)));
        refreshOperationAvailability();
    }

    @FXML
    private void handleAddRoot() {
        Integer value = parseValue(valueField);
        if (value == null) {
            return;
        }
        if (activeVariant == TreeVariant.AVL) {
            if (executeStructureOperation("insert", () -> {
                avlTree.insert(value);
                return null;
            })) {
                refreshStructureView();
            }
            return;
        }
        if (generalTree.root() != null) {
            logI18n("message.tree.root_exists");
            return;
        }
        if (executeStructureOperation("add-root", () -> generalTree.addRoot(value))) {
            refreshStructureView();
        }
    }

    @FXML
    private void handleAddChild() {
        if (activeVariant != TreeVariant.GENERAL) {
            return;
        }
        Integer value = parseValue(valueField);
        GeneralTreeNode<Integer> parent = selectedGeneralNode();
        if (value == null || parent == null) {
            return;
        }
        if (executeStructureOperation("add-child", () -> generalTree.addChild(parent, value))) {
            refreshStructureView();
        }
    }

    @FXML
    private void handleAddParent() {
        if (activeVariant != TreeVariant.GENERAL) {
            return;
        }
        Integer value = parseValue(valueField);
        GeneralTreeNode<Integer> node = selectedGeneralNode();
        if (value == null || node == null) {
            return;
        }
        if (executeStructureOperation("add-parent", () -> generalTree.addParent(node, value))) {
            refreshStructureView();
        }
    }

    @FXML
    private void handleDelete() {
        if (activeVariant == TreeVariant.GENERAL) {
            GeneralTreeNode<Integer> node = selectedGeneralNode();
            if (node == null) {
                return;
            }
            if (executeStructureOperation("remove", () -> generalTree.remove(node))) {
                clearNodeSelection();
                refreshStructureView();
            }
            return;
        }
        AVLTreeNode<Integer> node = selectedAvlNode();
        if (node == null) {
            return;
        }
        int value = node.getValue();
        if (executeStructureOperation("remove", () -> avlTree.remove(value))) {
            clearNodeSelection();
            refreshStructureView();
        }
    }

    @FXML
    private void handleUpdate() {
        if (activeVariant != TreeVariant.GENERAL) {
            return;
        }
        if (isRunning()) {
            logI18n("message.error.operation_running");
            return;
        }
        GeneralTreeNode<Integer> node = selectedGeneralNode();
        Integer value = parseValue(valueField);
        if (node == null || value == null) {
            return;
        }
        if (executeStructureOperation("update", () -> generalTree.set(node, value))) {
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
        String algorithmId = selectedAlgorithmId();
        if (algorithmId == null) {
            return;
        }
        if (activeVariant != TreeVariant.AVL) {
            throw new IllegalStateException("No executable general-tree algorithm is registered");
        }
        BinaryTreeSnapshot<Integer> inputSnapshot = selectedAvlAlgorithmSnapshot();
        AVLTree<Integer> runtimeTree = avlFromSnapshot(inputSnapshot);
        @SuppressWarnings("unchecked")
        AvlCommandAlgorithm<Integer> algorithm = (AvlCommandAlgorithm<Integer>) module(
                "algorithm.tree.Integer." + algorithmId,
                AvlCommandAlgorithm.class);
        TreeViewState initialState = TreeViewState.binary(avlNodeSnapshot(runtimeTree.root()));
        List<AvlCommand> commands = parseAvlCommands();
        if (commands.isEmpty()) {
            logI18n("message.tree.avl.commands_required");
            return;
        }
        startAlgorithm(
                algorithmId,
                inputSnapshot,
                () -> {
                    algorithm.execute(runtimeTree, commands);
                    return null;
                },
                () -> new TreeEventReducer(initialState));
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
        notifyAlgorithmSelection();
        return true;
    }

    @Override
    public List<String> algorithmIds() {
        return List.copyOf(algorithmIds);
    }

    @Override
    public void setAlgorithmSelectionListener(Consumer<String> listener) {
        if (listener == null) {
            algorithmSelectionListener = ignored -> { };
        } else {
            algorithmSelectionListener = listener;
        }
        notifyAlgorithmSelection();
    }

    @Override
    public StructureSnapshot<TreeSnapshotState<Integer>> captureStructureSnapshot() {
        TreeSnapshotState<Integer> state;
        if (activeVariant == TreeVariant.GENERAL) {
            state = currentGeneralSnapshot();
        } else {
            state = currentAvlSnapshot();
        }
        return StructureSnapshot.create(moduleId(), state);
    }

    @Override
    public void restoreStructureSnapshot(StructureSnapshot<TreeSnapshotState<Integer>> snapshot) {
        requireTreeSnapshot(snapshot);
        TreeSnapshotState<Integer> state = snapshot.state();
        if (state instanceof GeneralTreeSnapshot<?> general) {
            @SuppressWarnings("unchecked")
            GeneralTreeSnapshot<Integer> typed = (GeneralTreeSnapshot<Integer>) general;
            generalTree = Tree.fromSnapshot(typed);
            activateVariant(TreeVariant.GENERAL);
        } else if (state instanceof BinaryTreeSnapshot<?> binary) {
            @SuppressWarnings("unchecked")
            BinaryTreeSnapshot<Integer> typed = (BinaryTreeSnapshot<Integer>) binary;
            avlTree = avlFromSnapshot(typed);
            activateVariant(TreeVariant.AVL);
        } else {
            throw new IllegalArgumentException("unsupported tree snapshot type: " + state.getClass().getName());
        }
        clearNodeSelection();
        algorithmInputSnapshot = null;
        invalidateExecutionForStructureChange();
        refreshStructureView();
    }

    @Override
    public void useSnapshotAsAlgorithmInput(StructureSnapshot<TreeSnapshotState<Integer>> snapshot) {
        requireTreeSnapshot(snapshot);
        if (!snapshotMatchesActiveVariant(snapshot.state())) {
            throw new IllegalArgumentException("snapshot variant does not match the active tree structure");
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
        if (algorithmInputSnapshot == null) {
            return null;
        }
        return algorithmInputSnapshot.id();
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
        renderViewState(viewState(algorithmInputSnapshot.state()));
    }

    @Override
    public String describeStructureSnapshot(TreeSnapshotState<Integer> state) {
        if (state instanceof GeneralTreeSnapshot<?> general) {
            @SuppressWarnings("unchecked")
            GeneralTreeSnapshot<Integer> typed = (GeneralTreeSnapshot<Integer>) general;
            return I18N.text("snapshot.tree.detail", typed.size(), generalHeight(typed.root()));
        }
        if (state instanceof BinaryTreeSnapshot<?> binary) {
            @SuppressWarnings("unchecked")
            BinaryTreeSnapshot<Integer> typed = (BinaryTreeSnapshot<Integer>) binary;
            return I18N.text("snapshot.tree.detail", typed.size(), binaryHeight(typed.root()));
        }
        throw new IllegalArgumentException("unsupported tree snapshot type: " + state.getClass().getName());
    }

    private void initializeSampleGeneralTree() {
        GeneralTreeNode<Integer> root = generalTree.addRoot(50);
        GeneralTreeNode<Integer> left = generalTree.addChild(root, 30);
        GeneralTreeNode<Integer> middle = generalTree.addChild(root, 70);
        generalTree.addChild(root, 90);
        generalTree.addChild(left, 10);
        generalTree.addChild(left, 40);
        generalTree.addChild(middle, 60);
        generalTree.addChild(middle, 80);
    }

    private void initializeSampleAvlTree() {
        for (Integer value : List.of(50, 30, 70, 10, 40, 60, 90)) {
            avlTree.insert(value);
        }
    }

    private void activateVariant(TreeVariant variant) {
        if (activeVariant == variant) {
            refreshVariantControls();
            return;
        }
        stopAlgorithm();
        activeVariant = variant;
        algorithmInputSnapshot = null;
        clearNodeSelection();
        refreshAlgorithmIds();
        syncStructureSelectorSelection();
        refreshVariantControls();
        renderStructureState(currentStructureState());
        refreshStatsDisplay();
    }

    private void refreshStructureView() {
        renderStructureState(currentStructureState());
        refreshStatsDisplay();
        refreshOperationAvailability();
    }

    private TreeViewState currentStructureState() {
        if (activeVariant == TreeVariant.GENERAL) {
            return TreeViewState.general(currentGeneralSnapshot());
        }
        return TreeViewState.binary(avlNodeSnapshot(avlTree.root()));
    }

    private TreeViewState viewState(TreeSnapshotState<Integer> state) {
        if (state instanceof GeneralTreeSnapshot<?> general) {
            @SuppressWarnings("unchecked")
            GeneralTreeSnapshot<Integer> typed = (GeneralTreeSnapshot<Integer>) general;
            return TreeViewState.general(typed);
        }
        if (state instanceof BinaryTreeSnapshot<?> binary) {
            @SuppressWarnings("unchecked")
            BinaryTreeSnapshot<Integer> typed = (BinaryTreeSnapshot<Integer>) binary;
            AVLTree<Integer> restored = avlFromSnapshot(typed);
            return TreeViewState.binary(avlNodeSnapshot(restored.root()));
        }
        throw new IllegalArgumentException("unsupported tree snapshot type: " + state.getClass().getName());
    }

    private GeneralTreeSnapshot<Integer> currentGeneralSnapshot() {
        return new GeneralTreeSnapshot<>(snapshotGeneralNode(generalTree.root()), generalTree.size());
    }

    private GeneralTreeSnapshot.Node<Integer> snapshotGeneralNode(GeneralTreeNode<Integer> node) {
        if (node == null) {
            return null;
        }
        List<GeneralTreeSnapshot.Node<Integer>> children = node.getChildren().stream()
                .map(this::snapshotGeneralNode)
                .toList();
        return new GeneralTreeSnapshot.Node<>(node.getId(), node.getValue(), children);
    }

    private BinaryTreeSnapshot<Integer> currentAvlSnapshot() {
        return new BinaryTreeSnapshot<>(snapshotBinaryNode(avlTree.root()), avlTree.size());
    }

    private BinaryTreeSnapshot.Node<Integer> snapshotBinaryNode(AVLTreeNode<Integer> node) {
        if (node == null) {
            return null;
        }
        return new BinaryTreeSnapshot.Node<>(
                node.getId(),
                node.getValue(),
                snapshotBinaryNode(left(node)),
                snapshotBinaryNode(right(node)));
    }

    private BinaryTreeSnapshot<Integer> selectedAvlAlgorithmSnapshot() {
        if (algorithmInputSnapshot == null) {
            return currentAvlSnapshot();
        }
        if (!(algorithmInputSnapshot.state() instanceof BinaryTreeSnapshot<?> binary)) {
            throw new IllegalStateException("AVL algorithm input is not a binary-tree snapshot");
        }
        @SuppressWarnings("unchecked")
        BinaryTreeSnapshot<Integer> typed = (BinaryTreeSnapshot<Integer>) binary;
        return typed;
    }

    private AVLTree<Integer> avlFromSnapshot(BinaryTreeSnapshot<Integer> snapshot) {
        AVLTreeNode<Integer> root = restoreAvlNode(snapshot.root());
        return AVLTree.fromRestoredRoot(root);
    }

    private AVLTreeNode<Integer> restoreAvlNode(BinaryTreeSnapshot.Node<Integer> node) {
        if (node == null) {
            return null;
        }
        AVLTreeNode<Integer> left = restoreAvlNode(node.left());
        AVLTreeNode<Integer> right = restoreAvlNode(node.right());
        int height = Math.max(avlHeight(left), avlHeight(right)) + 1;
        return new AVLTreeNode<>(node.id(), node.value(), height, left, right);
    }

    private AvlNodeSnapshot avlNodeSnapshot(AVLTreeNode<Integer> node) {
        if (node == null) {
            return null;
        }
        return new AvlNodeSnapshot(
                node.getId(),
                node.getValue(),
                node.getHeight(),
                avlNodeSnapshot(left(node)),
                avlNodeSnapshot(right(node)));
    }

    @SuppressWarnings("unchecked")
    private AVLTreeNode<Integer> left(AVLTreeNode<Integer> node) {
        if (node == null || node.getLeft() == null) {
            return null;
        }
        return (AVLTreeNode<Integer>) node.getLeft();
    }

    @SuppressWarnings("unchecked")
    private AVLTreeNode<Integer> right(AVLTreeNode<Integer> node) {
        if (node == null || node.getRight() == null) {
            return null;
        }
        return (AVLTreeNode<Integer>) node.getRight();
    }

    private int avlHeight(AVLTreeNode<Integer> node) {
        if (node == null) {
            return 0;
        }
        return node.getHeight();
    }

    private Integer parseValue(TextField field) {
        try {
            return Integer.valueOf(field.getText().trim());
        } catch (RuntimeException exception) {
            logI18n("message.error.invalid_tree_input");
            return null;
        }
    }

    private List<AvlCommand> parseAvlCommands() {
        if (algorithmCommandsField == null) {
            return List.of();
        }
        String source = algorithmCommandsField.getText();
        if (source == null || source.isBlank()) {
            return List.of();
        }
        java.util.ArrayList<AvlCommand> commands = new java.util.ArrayList<>();
        String[] lines = source.split("\\R");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] parts = trimmed.split("\\s+");
            if (parts.length != 2) {
                logI18n("message.tree.avl.command_invalid", trimmed);
                return List.of();
            }
            AvlCommand.Operation operation;
            if ("INSERT".equalsIgnoreCase(parts[0])) {
                operation = AvlCommand.Operation.INSERT;
            } else if ("REMOVE".equalsIgnoreCase(parts[0])) {
                operation = AvlCommand.Operation.REMOVE;
            } else {
                logI18n("message.tree.avl.command_invalid", trimmed);
                return List.of();
            }
            try {
                commands.add(new AvlCommand(operation, Integer.parseInt(parts[1])));
            } catch (NumberFormatException exception) {
                logI18n("message.tree.avl.command_invalid", trimmed);
                return List.of();
            }
        }
        return List.copyOf(commands);
    }

    private GeneralTreeNode<Integer> selectedGeneralNode() {
        if (selectedNodeId == null) {
            logI18n("message.tree.select_node");
            return null;
        }
        GeneralTreeNode<Integer> node = generalTree.findById(selectedNodeId);
        if (node == null) {
            clearNodeSelection();
            logI18n("message.tree.select_node");
            return null;
        }
        return node;
    }

    private AVLTreeNode<Integer> selectedAvlNode() {
        if (selectedNodeId == null) {
            logI18n("message.tree.select_node");
            return null;
        }
        AVLTreeNode<Integer> node = avlNodeById(avlTree.root(), selectedNodeId);
        if (node == null) {
            clearNodeSelection();
            logI18n("message.tree.select_node");
            return null;
        }
        return node;
    }

    private AVLTreeNode<Integer> avlNodeById(AVLTreeNode<Integer> node, long id) {
        if (node == null) {
            return null;
        }
        if (node.getId() == id) {
            return node;
        }
        AVLTreeNode<Integer> found = avlNodeById(left(node), id);
        if (found != null) {
            return found;
        }
        return avlNodeById(right(node), id);
    }

    private GeneralTreeNode<Integer> generalParentOf(
            GeneralTreeNode<Integer> root,
            GeneralTreeNode<Integer> target) {
        if (root == null) {
            return null;
        }
        for (GeneralTreeNode<Integer> child : root.getChildren()) {
            if (child == target) {
                return root;
            }
            GeneralTreeNode<Integer> found = generalParentOf(child, target);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private AVLTreeNode<Integer> avlParentOf(AVLTreeNode<Integer> root, AVLTreeNode<Integer> target) {
        if (root == null) {
            return null;
        }
        if (left(root) == target || right(root) == target) {
            return root;
        }
        AVLTreeNode<Integer> found = avlParentOf(left(root), target);
        if (found != null) {
            return found;
        }
        return avlParentOf(right(root), target);
    }

    private int generalDepthOf(GeneralTreeNode<Integer> root, GeneralTreeNode<Integer> target, int depth) {
        if (root == null) {
            return -1;
        }
        if (root == target) {
            return depth;
        }
        for (GeneralTreeNode<Integer> child : root.getChildren()) {
            int found = generalDepthOf(child, target, depth + 1);
            if (found >= 0) {
                return found;
            }
        }
        return -1;
    }

    private int avlDepthOf(AVLTreeNode<Integer> root, AVLTreeNode<Integer> target, int depth) {
        if (root == null) {
            return -1;
        }
        if (root == target) {
            return depth;
        }
        int leftDepth = avlDepthOf(left(root), target, depth + 1);
        if (leftDepth >= 0) {
            return leftDepth;
        }
        return avlDepthOf(right(root), target, depth + 1);
    }

    private int generalHeight(GeneralTreeSnapshot.Node<Integer> node) {
        if (node == null) {
            return 0;
        }
        int maxChildHeight = 0;
        for (GeneralTreeSnapshot.Node<Integer> child : node.children()) {
            maxChildHeight = Math.max(maxChildHeight, generalHeight(child));
        }
        return maxChildHeight + 1;
    }

    private int binaryHeight(BinaryTreeSnapshot.Node<Integer> node) {
        if (node == null) {
            return 0;
        }
        return Math.max(binaryHeight(node.left()), binaryHeight(node.right())) + 1;
    }

    private void clearNodeSelection() {
        selectedNodeId = null;
        treeVisualizer().clearSelection();
        selectionListener.accept(null);
        refreshOperationAvailability();
    }

    private void refreshVariantControls() {
        refreshAlgorithmIds();
        refreshAlgorithmSelector();
        refreshOperationVisibility();
        refreshOperationLabels();
        refreshAlgorithmCommandVisibility();
        refreshOperationAvailability();
    }

    private void refreshAlgorithmIds() {
        if (activeVariant == TreeVariant.GENERAL) {
            algorithmIds = AlgorithmCatalog.generalTreeAlgorithms();
        } else {
            algorithmIds = AlgorithmCatalog.avlTreeAlgorithms();
        }
    }

    private void refreshAlgorithmSelector() {
        if (algorithmSelector == null) {
            return;
        }
        javafx.collections.ObservableList<String> labels = FXCollections.observableArrayList();
        for (String id : algorithmIds) {
            labels.add(AlgorithmLabels.text(id));
        }
        algorithmSelector.setItems(labels);
        if (algorithmIds.isEmpty()) {
            algorithmSelector.getSelectionModel().clearSelection();
        } else {
            algorithmSelector.getSelectionModel().selectFirst();
        }
        notifyAlgorithmSelection();
    }

    private void notifyAlgorithmSelection() {
        algorithmSelectionListener.accept(selectedAlgorithmId());
    }

    private void refreshOperationVisibility() {
        boolean general = activeVariant == TreeVariant.GENERAL;
        setVisibleManaged(addChildBtn, general);
        setVisibleManaged(addParentBtn, general);
        setVisibleManaged(updateBtn, general);
    }

    private void refreshOperationLabels() {
        if (addRootBtn != null) {
            addRootBtn.textProperty().unbind();
            String key;
            if (activeVariant == TreeVariant.GENERAL) {
                key = "action.tree.add_root";
            } else {
                key = "action.tree.insert";
            }
            bindButton(addRootBtn, key);
        }
        if (selectionHintLabel != null) {
            selectionHintLabel.textProperty().unbind();
            String key;
            if (activeVariant == TreeVariant.GENERAL) {
                key = "label.tree.selection_hint";
            } else {
                key = "label.tree.selection_hint.avl";
            }
            selectionHintLabel.textProperty().bind(I18N.createStringBinding(key));
        }
    }

    private void refreshAlgorithmCommandVisibility() {
        boolean visible = activeVariant == TreeVariant.AVL;
        setVisibleManaged(algorithmCommandsHintLabel, visible);
        setVisibleManaged(algorithmCommandsField, visible);
    }

    private void refreshOperationAvailability() {
        boolean hasSelection = selectedNodeId != null;
        if (activeVariant == TreeVariant.GENERAL && hasSelection) {
            hasSelection = generalTree.findById(selectedNodeId) != null;
        }
        if (activeVariant == TreeVariant.AVL && hasSelection) {
            hasSelection = avlNodeById(avlTree.root(), selectedNodeId) != null;
        }
        if (addRootBtn != null) {
            if (activeVariant == TreeVariant.GENERAL) {
                addRootBtn.setDisable(generalTree.root() != null);
            } else {
                addRootBtn.setDisable(false);
            }
        }
        if (addChildBtn != null) {
            addChildBtn.setDisable(!hasSelection || activeVariant != TreeVariant.GENERAL);
        }
        if (addParentBtn != null) {
            addParentBtn.setDisable(!hasSelection || activeVariant != TreeVariant.GENERAL);
        }
        if (deleteBtn != null) {
            deleteBtn.setDisable(!hasSelection);
        }
        if (updateBtn != null) {
            updateBtn.setDisable(!hasSelection || activeVariant != TreeVariant.GENERAL);
        }
    }

    private void setVisibleManaged(javafx.scene.Node node, boolean visible) {
        if (node == null) {
            return;
        }
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private void requireTreeSnapshot(StructureSnapshot<TreeSnapshotState<Integer>> snapshot) {
        if (!moduleId().equals(snapshot.moduleId())) {
            throw new IllegalArgumentException("snapshot belongs to module " + snapshot.moduleId());
        }
    }

    private boolean snapshotMatchesActiveVariant(TreeSnapshotState<Integer> state) {
        if (activeVariant == TreeVariant.GENERAL) {
            return state instanceof GeneralTreeSnapshot<?>;
        }
        return state instanceof BinaryTreeSnapshot<?>;
    }

    private TreeVisualizer treeVisualizer() {
        return (TreeVisualizer) visualizer;
    }

    @Override
    protected void onAlgorithmFinished(com.majortom.algorithms.core.runtime.ExecutionResult result) {
        super.onAlgorithmFinished(result);
        logI18n("message.execution.finished");
    }

    @Override
    public String structureSummaryText() {
        if (activeVariant == TreeVariant.GENERAL) {
            GeneralTreeSnapshot<Integer> snapshot = currentGeneralSnapshot();
            Object rootValue = I18N.text("label.workspace.selection.none");
            if (snapshot.root() != null) {
                rootValue = snapshot.root().value();
            }
            return I18N.text("label.workspace.structure.summary.tree",
                    snapshot.size(), generalHeight(snapshot.root()), rootValue);
        }
        BinaryTreeSnapshot<Integer> snapshot = currentAvlSnapshot();
        Object rootValue = I18N.text("label.workspace.selection.none");
        if (snapshot.root() != null) {
            rootValue = snapshot.root().value();
        }
        return I18N.text("label.workspace.structure.summary.tree",
                snapshot.size(), binaryHeight(snapshot.root()), rootValue);
    }

    @Override
    public String structurePrimaryCount() {
        if (activeVariant == TreeVariant.GENERAL) {
            return Integer.toString(generalTree.size());
        }
        return Integer.toString(avlTree.size());
    }

    @Override
    public String structureSecondaryCount() {
        if (activeVariant == TreeVariant.GENERAL) {
            return Integer.toString(generalHeight(currentGeneralSnapshot().root()));
        }
        return Integer.toString(binaryHeight(currentAvlSnapshot().root()));
    }

    @Override
    protected String formatStatsMessage() {
        int size;
        int height;
        if (activeVariant == TreeVariant.GENERAL) {
            size = generalTree.size();
            height = generalHeight(currentGeneralSnapshot().root());
        } else {
            size = avlTree.size();
            height = binaryHeight(currentAvlSnapshot().root());
        }
        return String.format("%s | %s | %s",
                I18N.text("stats.size", size),
                I18N.text("stats.height", height),
                formatMetric("stats.action", stats.metric("nodes.inserted") + stats.metric("nodes.removed")));
    }

    @Override
    protected void onResetData() {
        clearNodeSelection();
        if (activeVariant == TreeVariant.GENERAL) {
            generalTree = new Tree<>();
        } else {
            avlTree = new AVLTree<>();
        }
        refreshStructureView();
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
        if (algorithmCommandsField != null) {
            algorithmCommandsField.promptTextProperty().bind(I18N.createStringBinding("prompt.tree.avl.commands"));
        }
        if (algorithmCommandsHintLabel != null) {
            algorithmCommandsHintLabel.textProperty().bind(I18N.createStringBinding("label.tree.avl.commands"));
        }
        bindButton(addChildBtn, "action.tree.add_child");
        bindButton(addParentBtn, "action.tree.add_parent");
        bindButton(deleteBtn, "action.tree.delete");
        bindButton(updateBtn, "action.tree.update");
        bindButton(randomBtn, "action.tree.random");
        refreshOperationLabels();
    }

    @Override
    protected String moduleId() {
        return "tree";
    }

    @Override
    public String selectedAlgorithmId() {
        if (algorithmIds.isEmpty()) {
            return null;
        }
        int index = 0;
        if (algorithmSelector != null) {
            index = algorithmSelector.getSelectionModel().getSelectedIndex();
        }
        if (index < 0 || index >= algorithmIds.size()) {
            index = 0;
        }
        return algorithmIds.get(index);
    }

    private void bindSelectors() {
        structureSelector.itemsProperty().bind(Bindings.createObjectBinding(
                () -> FXCollections.observableArrayList(
                        I18N.text("label.tree.structure.general"),
                        I18N.text("label.tree.structure.avl")),
                I18N.localeProperty()));
        structureSelector.getSelectionModel().selectedIndexProperty().addListener((observable, previous, current) -> {
            if (current == null || current.intValue() < 0) {
                return;
            }
            if (current.intValue() == 0) {
                activateVariant(TreeVariant.GENERAL);
            } else {
                activateVariant(TreeVariant.AVL);
            }
        });
        algorithmSelector.getSelectionModel().selectedIndexProperty().addListener(
                (observable, previous, current) -> notifyAlgorithmSelection());
        I18N.localeProperty().addListener((observable, previous, current) -> {
            refreshAlgorithmSelector();
            refreshOperationLabels();
            Platform.runLater(this::syncStructureSelectorSelection);
        });
        Platform.runLater(() -> {
            syncStructureSelectorSelection();
            refreshAlgorithmSelector();
        });
    }

    private void syncStructureSelectorSelection() {
        if (structureSelector == null) {
            return;
        }
        if (activeVariant == TreeVariant.GENERAL) {
            structureSelector.getSelectionModel().select(0);
        } else {
            structureSelector.getSelectionModel().select(1);
        }
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
