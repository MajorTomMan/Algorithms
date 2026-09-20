package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.statistics.MetricKeys;
import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.visualization.render.runtime.RenderContext;
import com.majortom.algorithms.visualization.render.fx.FxDispatch;

import com.majortom.algorithms.core.registry.AlgorithmDescriptor;
import com.majortom.algorithms.core.snapshot.BinaryTreeSnapshot;
import com.majortom.algorithms.core.snapshot.GeneralTreeSnapshot;
import com.majortom.algorithms.core.snapshot.StructureSnapshot;
import com.majortom.algorithms.core.snapshot.TreeSnapshotState;
import com.majortom.algorithms.structure.tree.AVLTree;
import com.majortom.algorithms.structure.tree.AVLTreeNode;
import com.majortom.algorithms.structure.tree.GeneralTreeNode;
import com.majortom.algorithms.structure.tree.Tree;
import com.majortom.algorithms.utils.EffectUtils;
import com.majortom.algorithms.visualization.algorithm.AlgorithmCatalog;
import com.majortom.algorithms.visualization.structure.StructureCatalog;
import com.majortom.algorithms.visualization.impl.visualizer.TreeVisualizer;
import com.majortom.algorithms.visualization.impl.visualizer.presenter.TreePresenter;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.module.AlgorithmSelectionSupport;
import com.majortom.algorithms.visualization.runtime.VisualValue;
import com.majortom.algorithms.visualization.runtime.tree.TreeEventReducer;
import com.majortom.algorithms.visualization.runtime.tree.TreeViewState;
import com.majortom.algorithms.visualization.structure.SnapshotAlgorithmInputSupport;
import com.majortom.algorithms.visualization.structure.StructureSnapshotSupport;
import com.majortom.algorithms.visualization.structure.RuntimeValueTypeSupport;
import com.majortom.algorithms.visualization.runtime.value.ValueAdapter;
import com.majortom.algorithms.visualization.runtime.value.ValueAdapters;
import com.majortom.algorithms.structure.tree.AvlTreeStructure;
import com.majortom.algorithms.structure.tree.GeneralTreeStructure;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import static com.majortom.algorithms.visualization.impl.controller.TreeSnapshotMapper.*;
import static com.majortom.algorithms.visualization.impl.controller.TreeNodeQueries.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public final class TreeController extends BaseModuleController<TreeViewState>
        implements AlgorithmSelectionSupport, StructureSnapshotSupport<TreeSnapshotState<Object>>,
        SnapshotAlgorithmInputSupport<TreeSnapshotState<Object>>, RuntimeValueTypeSupport {

    private Tree<Object> generalTree;
    @SuppressWarnings("rawtypes")
    private AVLTree avlTree;
    private TreeVariant activeVariant = TreeVariant.GENERAL;
    private List<String> algorithmIds = List.of();
    private StructureSnapshot<TreeSnapshotState<Object>> algorithmInputSnapshot;
    private Consumer<NodeSelection> selectionListener = ignored -> { };
    private Consumer<String> algorithmSelectionListener = ignored -> { };
    private boolean structureSelectionEnabled = true;
    private Long selectedNodeId;
    private Long algorithmSelectedNodeId;
    private Class<?> runtimeValueType = Integer.class;
    private ValueAdapter<Object> valueAdapter = ValueAdapters.requireObjectAdapter(Integer.class);

    @FXML private Label structureLabel;
    @FXML private ComboBox<String> structureSelector;
    @FXML private Label algorithmLabel;
    @FXML private Label operationsSectionLabel;
    @FXML private ComboBox<String> algorithmSelector;
    @FXML private TextField valueField;
    @FXML private Label selectionHintLabel;
    @FXML private Button addRootBtn;
    @FXML private Button addChildBtn;
    @FXML private Button addParentBtn;
    @FXML private Button deleteBtn;
    @FXML private Button updateBtn;
    @FXML private Button randomBtn;

    public TreeController(RenderContext renderContext) {
        super(new TreeVisualizer(), new TreePresenter(), "/fxml/TreeControls.fxml", renderContext);
        @SuppressWarnings("unchecked")
        Tree<Object> resolvedGeneralTree = (Tree<Object>) structure(StructureIds.TREE, Tree.class);
        generalTree = resolvedGeneralTree;
        avlTree = structure(StructureIds.AVL_TREE, AVLTree.class);
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
        if (randomBtn != null) randomBtn.setDisable(!ValueAdapters.canGenerate(runtimeValueType));
    }

    public record NodeSelection(long id, VisualValue value, Long parentId, int childCount, int depth) {
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

    public void setStructureSelectionEnabled(boolean enabled) {
        if (structureSelectionEnabled != enabled) {
            clearNodeSelection();
        }
        structureSelectionEnabled = enabled;
    }

    private void handleVisualSelection(long nodeId) {
        requestPresentationRender();
        if (!structureSelectionEnabled) {
            handleAlgorithmSelection(nodeId);
            return;
        }
        if (activeVariant == TreeVariant.GENERAL) {
            handleGeneralSelection(nodeId);
        } else {
            handleAvlSelection(nodeId);
        }
    }

    private void handleAlgorithmSelection(long nodeId) {
        TreeViewState state = latestViewState();
        if (state == null) {
            return;
        }
        algorithmSelectedNodeId = nodeId;
        if (!publishAlgorithmSelection(state, nodeId)) {
            clearNodeSelection();
        }
    }

    private boolean publishAlgorithmSelection(TreeViewState state, long nodeId) {
        TreeViewState.Node node = state.nodes().get(nodeId);
        if (node == null) {
            return false;
        }
        Long parentId = presentationParentId(state, nodeId);
        int depth = presentationDepth(state, nodeId);
        selectionListener.accept(new NodeSelection(
                nodeId,
                node.value(),
                parentId,
                state.childrenOf(node).size(),
                depth));
        return true;
    }

    @Override
    protected void onPresentationStateChanged(TreeViewState state) {
        if (structureSelectionEnabled || algorithmSelectedNodeId == null) {
            return;
        }
        long nodeId = algorithmSelectedNodeId;
        if (!treeVisualizer().showSelection(nodeId) || !publishAlgorithmSelection(state, nodeId)) {
            clearNodeSelection();
        } else {
            requestPresentationRender();
        }
    }

    private void handleGeneralSelection(long nodeId) {
        GeneralTreeNode<Object> node = generalTree.findById(nodeId);
        if (node == null) {
            clearNodeSelection();
            return;
        }
        selectedNodeId = nodeId;
        valueField.setText(valueAdapter.format(node.getValue()));
        GeneralTreeNode<Object> parent = generalParentOf(generalTree.root(), node);
        Long parentId = null;
        if (parent != null) {
            parentId = parent.getId();
        }
        selectionListener.accept(new NodeSelection(
                nodeId,
                VisualValue.of(node.getValue()),
                parentId,
                node.getChildren().size(),
                generalDepthOf(generalTree.root(), node, 0)));
        refreshOperationAvailability();
    }

    private void handleAvlSelection(long nodeId) {
        AVLTreeNode<Object> node = avlNodeById(avlRoot(), nodeId);
        if (node == null) {
            clearNodeSelection();
            return;
        }
        selectedNodeId = nodeId;
        valueField.setText(valueAdapter.format(node.getValue()));
        AVLTreeNode<Object> parent = avlParentOf(avlRoot(), node);
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
                VisualValue.of(node.getValue()),
                parentId,
                childCount,
                avlDepthOf(avlRoot(), node, 0)));
        refreshOperationAvailability();
    }

    @FXML
    private void handleAddRoot() {
        Object value = parseValue(valueField);
        if (value == null) {
            return;
        }
        if (activeVariant == TreeVariant.AVL) {
            if (executeStructureOperation("insert", () -> {
                avlInsert(value);
                return null;
            })) {
                refreshStructureView();
                AVLTreeNode<Object> inserted = avlFind(value);
                if (inserted != null) {
                    treeVisualizer().selectNode(inserted.getId());
                }
            }
            return;
        }
        if (generalTree.root() != null) {
            logI18n("message.tree.root_exists");
            return;
        }
        if (executeStructureOperation("add-root", () -> generalTree.addRoot(value))) {
            refreshStructureView();
            treeVisualizer().selectNode(generalTree.root().getId());
        }
    }

    @FXML
    private void handleAddChild() {
        if (activeVariant != TreeVariant.GENERAL) {
            return;
        }
        Object value = parseValue(valueField);
        GeneralTreeNode<Object> parent = selectedGeneralNode();
        if (value == null || parent == null) {
            return;
        }
        long[] addedNodeId = {-1L};
        if (executeStructureOperation("add-child", () -> {
            GeneralTreeNode<Object> child = generalTree.addChild(parent, value);
            addedNodeId[0] = child.getId();
            return child;
        })) {
            refreshStructureView();
            treeVisualizer().selectNode(addedNodeId[0]);
        }
    }

    @FXML
    private void handleAddParent() {
        if (activeVariant != TreeVariant.GENERAL) {
            return;
        }
        Object value = parseValue(valueField);
        GeneralTreeNode<Object> node = selectedGeneralNode();
        if (value == null || node == null) {
            return;
        }
        long[] addedNodeId = {-1L};
        if (executeStructureOperation("add-parent", () -> {
            GeneralTreeNode<Object> parent = generalTree.addParent(node, value);
            addedNodeId[0] = parent.getId();
            return parent;
        })) {
            refreshStructureView();
            treeVisualizer().selectNode(addedNodeId[0]);
        }
    }

    @FXML
    private void handleDelete() {
        if (activeVariant == TreeVariant.GENERAL) {
            GeneralTreeNode<Object> node = selectedGeneralNode();
            if (node == null) {
                return;
            }
            List<Long> previousOrder = generalNodeOrder(generalTree.root());
            int removedIndex = previousOrder.indexOf(node.getId());
            if (executeStructureOperation("remove", () -> generalTree.remove(node))) {
                refreshStructureView();
                selectTreeAfterRemoval(previousOrder, removedIndex);
            }
            return;
        }
        AVLTreeNode<Object> node = selectedAvlNode();
        if (node == null) {
            return;
        }
        List<Long> previousOrder = avlNodeOrder(avlRoot());
        int removedIndex = previousOrder.indexOf(node.getId());
        Object value = node.getValue();
        if (executeStructureOperation("remove", () -> avlRemove(value))) {
            refreshStructureView();
            selectTreeAfterRemoval(previousOrder, removedIndex);
        }
    }

    private void selectTreeAfterRemoval(List<Long> previousOrder, int removedIndex) {
        if (previousOrder.isEmpty()) {
            clearNodeSelection();
            valueField.clear();
            return;
        }
        int startIndex = removedIndex;
        if (startIndex < 0) {
            startIndex = 0;
        }
        for (int index = startIndex; index < previousOrder.size(); index++) {
            long candidateId = previousOrder.get(index);
            if (treeNodeExists(candidateId)) {
                treeVisualizer().selectNode(candidateId);
                return;
            }
        }
        for (int index = startIndex - 1; index >= 0; index--) {
            long candidateId = previousOrder.get(index);
            if (treeNodeExists(candidateId)) {
                treeVisualizer().selectNode(candidateId);
                return;
            }
        }
        clearNodeSelection();
        valueField.clear();
    }

    private boolean treeNodeExists(long nodeId) {
        if (activeVariant == TreeVariant.GENERAL) {
            return generalTree.findById(nodeId) != null;
        }
        return avlNodeById(avlRoot(), nodeId) != null;
    }

    private List<Long> generalNodeOrder(GeneralTreeNode<Object> root) {
        List<Long> order = new ArrayList<>();
        appendGeneralNodeOrder(root, order);
        return List.copyOf(order);
    }

    private void appendGeneralNodeOrder(GeneralTreeNode<Object> node, List<Long> order) {
        if (node == null) {
            return;
        }
        order.add(node.getId());
        for (GeneralTreeNode<Object> child : node.getChildren()) {
            appendGeneralNodeOrder(child, order);
        }
    }

    private List<Long> avlNodeOrder(AVLTreeNode<Object> root) {
        List<Long> order = new ArrayList<>();
        appendAvlNodeOrder(root, order);
        return List.copyOf(order);
    }

    private void appendAvlNodeOrder(AVLTreeNode<Object> node, List<Long> order) {
        if (node == null) {
            return;
        }
        appendAvlNodeOrder(left(node), order);
        order.add(node.getId());
        appendAvlNodeOrder(right(node), order);
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
        GeneralTreeNode<Object> node = selectedGeneralNode();
        Object value = parseValue(valueField);
        if (node == null || value == null) {
            return;
        }
        if (executeStructureOperation("update", () -> generalTree.set(node, value))) {
            long nodeId = node.getId();
            refreshStructureView();
            treeVisualizer().selectNode(nodeId);
        }
    }

    @FXML
    private void handleRandom() {
        if (!ValueAdapters.canGenerate(runtimeValueType)) return;
        valueField.setText(valueAdapter.format(ValueAdapters.randomValue(runtimeValueType, new Random())));
    }

    @Override
    protected boolean supportsDataTools() {
        return true;
    }

    @Override
    protected String bulkInputPromptKey() {
        return "prompt.data.bulk.tree";
    }

    @Override
    protected void applyBulkData(String input) {
        List<Object> values = parseBatchInput(input, valueAdapter);
        if (values == null) {
            return;
        }
        if (activeVariant == TreeVariant.AVL
                && new java.util.HashSet<>(values).size() != values.size()) {
            logI18n("message.error.bulk_duplicates");
            return;
        }
        replaceTreeValues(values, "bulk-replace", "message.data.bulk_applied");
    }

    @Override
    protected boolean canGenerateRandomData() {
        return ValueAdapters.canGenerate(runtimeValueType)
                && ValueAdapters.canGenerateDistinct(runtimeValueType, 1);
    }

    @Override
    protected void randomizeData() {
        Random random = new Random();
        java.util.LinkedHashSet<Object> unique = new java.util.LinkedHashSet<>();
        int count = Math.min(10, ValueAdapters.maxDistinctSamples(runtimeValueType));
        int attempts = 0;
        while (unique.size() < count && attempts++ < 1000) {
            unique.add(ValueAdapters.randomValue(runtimeValueType, random));
        }
        for (int index = 0; unique.size() < count; index++) {
            unique.add(ValueAdapters.distinctValue(runtimeValueType, index));
        }
        replaceTreeValues(new ArrayList<>(unique), "randomize", "message.data.randomized");
    }

    private void replaceTreeValues(List<Object> values, String operationId, String messageKey) {
        if (!executeStructureOperation(operationId, () -> {
            if (activeVariant == TreeVariant.GENERAL) {
                replaceGeneralTreeValues(values);
            } else {
                replaceAvlTreeValues(values);
            }
            return null;
        })) {
            return;
        }
        clearNodeSelection();
        refreshStructureView();
        int count;
        if (activeVariant == TreeVariant.GENERAL) {
            count = generalTree.size();
            if (generalTree.root() != null) {
                treeVisualizer().selectNode(generalTree.root().getId());
            }
        } else {
            count = avlTree.size();
            AVLTreeNode<Object> selected = avlFind(values.get(0));
            if (selected != null) {
                treeVisualizer().selectNode(selected.getId());
            }
        }
        logI18n(messageKey, count);
    }

    private void replaceGeneralTreeValues(List<Object> values) {
        generalTree.initialize(generalInput(values, 0));
    }

    private GeneralTreeStructure.NodeInput<Object> generalInput(List<Object> values, int index) {
        if (index >= values.size()) {
            return null;
        }
        List<GeneralTreeStructure.NodeInput<Object>> children = new ArrayList<>(3);
        for (int offset = 1; offset <= 3; offset++) {
            GeneralTreeStructure.NodeInput<Object> child = generalInput(values, index * 3 + offset);
            if (child != null) {
                children.add(child);
            }
        }
        return new GeneralTreeStructure.NodeInput<>(values.get(index), children);
    }

    private void replaceAvlTreeValues(List<Object> values) {
        avlInitializeSorted(sortedComparableValues(values));
    }

    private List<Object> sortedComparableValues(List<Object> values) {
        List<Object> sorted = new ArrayList<>(values);
        sorted.sort(this::compareComparableValues);
        for (int index = 1; index < sorted.size(); index++) {
            if (compareComparableValues(sorted.get(index - 1), sorted.get(index)) == 0) {
                throw new IllegalArgumentException("AVL bulk values must be unique");
            }
        }
        return List.copyOf(sorted);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private int compareComparableValues(Object left, Object right) {
        return requireComparable(left).compareTo(right);
    }

    @Override
    public void handleAlgorithmStart() {
        if (!ensureReplayableValue(runtimeValueType)) return;
        String algorithmId = selectedAlgorithmId();
        if (algorithmId == null) {
            algorithmLogI18n("message.tree.no_algorithm");
            return;
        }

        TreeSnapshotState<Object> inputState = algorithmInputSnapshot == null
                ? (activeVariant == TreeVariant.GENERAL ? currentGeneralSnapshot() : currentAvlSnapshot())
                : algorithmInputSnapshot.state();
        TreeViewState initialState = viewState(inputState);
        Class<?> activeStructure = inputState instanceof GeneralTreeSnapshot<?>
                ? GeneralTreeStructure.class
                : AvlTreeStructure.class;
        AlgorithmDescriptor descriptor = AlgorithmCatalog.compatibleDescriptor(
                activeStructure, runtimeValueType, algorithmId);
        Object input = algorithmTree(inputState);

        startAlgorithm(algorithmId, inputState, () -> {
            descriptor.invoke(input);
            return null;
        }, () -> new TreeEventReducer(initialState));
    }

    private Object algorithmTree(TreeSnapshotState<Object> state) {
        if (state instanceof GeneralTreeSnapshot<?> general) {
            @SuppressWarnings("unchecked")
            GeneralTreeSnapshot<Object> typed = (GeneralTreeSnapshot<Object>) general;
            return Tree.fromSnapshot(typed);
        }
        if (state instanceof BinaryTreeSnapshot<?> binary) {
            @SuppressWarnings("unchecked")
            BinaryTreeSnapshot<Object> typed = (BinaryTreeSnapshot<Object>) binary;
            return avlFromSnapshot(typed);
        }
        throw new IllegalArgumentException("unsupported tree snapshot type: " + state.getClass().getName());
    }

    @Override
    public boolean selectAlgorithm(String algorithmId) {
        if (algorithmId == null) {
            return false;
        }
        if (!algorithmIds.contains(algorithmId)) {
            TreeVariant compatibleVariant = variantForAlgorithm(algorithmId);
            if (compatibleVariant == null) {
                return false;
            }
            activateVariant(compatibleVariant);
        }
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

    private TreeVariant variantForAlgorithm(String algorithmId) {
        if (AlgorithmCatalog.compatibleAlgorithms(GeneralTreeStructure.class, runtimeValueType).contains(algorithmId)) {
            return TreeVariant.GENERAL;
        }
        if (AlgorithmCatalog.compatibleAlgorithms(AvlTreeStructure.class, runtimeValueType).contains(algorithmId)) {
            return TreeVariant.AVL;
        }
        return null;
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
    public StructureSnapshot<TreeSnapshotState<Object>> captureStructureSnapshot() {
        TreeSnapshotState<Object> state;
        if (activeVariant == TreeVariant.GENERAL) {
            state = currentGeneralSnapshot();
        } else {
            state = currentAvlSnapshot();
        }
        return StructureSnapshot.create(moduleId(), runtimeValueType, state);
    }

    @Override
    public void restoreStructureSnapshot(StructureSnapshot<TreeSnapshotState<Object>> snapshot) {
        requireTreeSnapshot(snapshot);
        TreeSnapshotState<Object> state = snapshot.state();
        if (state instanceof GeneralTreeSnapshot<?> general) {
            @SuppressWarnings("unchecked")
            GeneralTreeSnapshot<Object> typed = (GeneralTreeSnapshot<Object>) general;
            generalTree = Tree.fromSnapshot(typed);
            activateVariant(TreeVariant.GENERAL);
        } else if (state instanceof BinaryTreeSnapshot<?> binary) {
            @SuppressWarnings("unchecked")
            BinaryTreeSnapshot<Object> typed = (BinaryTreeSnapshot<Object>) binary;
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
    public void useSnapshotAsAlgorithmInput(StructureSnapshot<TreeSnapshotState<Object>> snapshot) {
        requireTreeSnapshot(snapshot);
        TreeSnapshotState<Object> state = snapshot.state();
        if (state instanceof GeneralTreeSnapshot<?>) {
            activateVariant(TreeVariant.GENERAL);
        } else if (state instanceof BinaryTreeSnapshot<?>) {
            activateVariant(TreeVariant.AVL);
        } else {
            throw new IllegalArgumentException("unsupported tree snapshot type: " + state.getClass().getName());
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
    public void previewStructureSnapshot(StructureSnapshot<TreeSnapshotState<Object>> snapshot) {
        requireTreeSnapshot(snapshot);
        clearNodeSelection();
        renderPreviewState(viewState(snapshot.state()));
    }

    @Override
    public String describeStructureSnapshot(TreeSnapshotState<Object> state) {
        if (state instanceof GeneralTreeSnapshot<?> general) {
            @SuppressWarnings("unchecked")
            GeneralTreeSnapshot<Object> typed = (GeneralTreeSnapshot<Object>) general;
            return I18N.text("snapshot.tree.detail", typed.size(), generalHeight(typed.root()));
        }
        if (state instanceof BinaryTreeSnapshot<?> binary) {
            @SuppressWarnings("unchecked")
            BinaryTreeSnapshot<Object> typed = (BinaryTreeSnapshot<Object>) binary;
            return I18N.text("snapshot.tree.detail", typed.size(), binaryHeight(typed.root()));
        }
        throw new IllegalArgumentException("unsupported tree snapshot type: " + state.getClass().getName());
    }

    @Override
    public String snapshotPrimaryCount(TreeSnapshotState<Object> state) {
        if (state instanceof GeneralTreeSnapshot<?> general) {
            return Integer.toString(general.size());
        }
        if (state instanceof BinaryTreeSnapshot<?> binary) {
            return Integer.toString(binary.size());
        }
        return "—";
    }

    @Override
    public String snapshotSecondaryCount(TreeSnapshotState<Object> state) {
        if (state instanceof GeneralTreeSnapshot<?> general) {
            @SuppressWarnings("unchecked")
            GeneralTreeSnapshot<Object> typed = (GeneralTreeSnapshot<Object>) general;
            return Integer.toString(generalHeight(typed.root()));
        }
        if (state instanceof BinaryTreeSnapshot<?> binary) {
            @SuppressWarnings("unchecked")
            BinaryTreeSnapshot<Object> typed = (BinaryTreeSnapshot<Object>) binary;
            return Integer.toString(binaryHeight(typed.root()));
        }
        return "—";
    }

    private void initializeSampleGeneralTree() {
        replaceGeneralTreeValues(sampleGeneralValues());
    }

    private void initializeSampleAvlTree() {
        replaceAvlTreeValues(sampleAvlValues());
    }

    private List<Object> sampleGeneralValues() {
        if (runtimeValueType == String.class) {
            return List.of("root", "alpha", "beta", "gamma", "delta", "epsilon", "zeta", "eta");
        }
        if (runtimeValueType == Integer.class) return List.of(50, 30, 70, 90, 10, 40, 60, 80);
        return sampleTypedValues(8);
    }

    private List<Object> sampleAvlValues() {
        if (runtimeValueType == String.class) {
            return List.of("alpha", "beta", "delta", "epsilon", "gamma", "theta", "zeta");
        }
        if (runtimeValueType == Integer.class) return List.of(10, 30, 40, 50, 60, 70, 90);
        return sampleTypedValues(7);
    }

    private List<Object> sampleTypedValues(int requested) {
        int count = Math.min(requested, ValueAdapters.maxDistinctSamples(runtimeValueType));
        List<Object> result = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            result.add(ValueAdapters.distinctValue(runtimeValueType, index));
        }
        return List.copyOf(result);
    }

    private void activateVariant(TreeVariant variant) {
        setStructureLogScopeId(variant == TreeVariant.GENERAL ? StructureIds.TREE : StructureIds.AVL_TREE);
        if (activeVariant == variant) {
            refreshVariantControls();
            return;
        }
        algorithmInputSnapshot = null;
        invalidateExecutionForInputChange();
        activeVariant = variant;
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
        return TreeViewState.binary(currentAvlSnapshot());
    }

    private TreeViewState viewState(TreeSnapshotState<Object> state) {
        if (state instanceof GeneralTreeSnapshot<?> general) {
            @SuppressWarnings("unchecked")
            GeneralTreeSnapshot<Object> typed = (GeneralTreeSnapshot<Object>) general;
            return TreeViewState.general(typed);
        }
        if (state instanceof BinaryTreeSnapshot<?> binary) {
            @SuppressWarnings("unchecked")
            BinaryTreeSnapshot<Object> typed = (BinaryTreeSnapshot<Object>) binary;
            return TreeViewState.binary(typed);
        }
        throw new IllegalArgumentException("unsupported tree snapshot type: " + state.getClass().getName());
    }

    private GeneralTreeSnapshot<Object> currentGeneralSnapshot() {
        return new GeneralTreeSnapshot<>(snapshotGeneralNode(generalTree.root()), generalTree.size());
    }

    private BinaryTreeSnapshot<Object> currentAvlSnapshot() {
        return new BinaryTreeSnapshot<>(snapshotBinaryNode(avlRoot()), avlTree.size());
    }

    @SuppressWarnings("unchecked")
    private AVLTreeNode<Object> avlRoot() {
        return (AVLTreeNode<Object>) avlTree.root();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void avlInsert(Object value) {
        avlTree.insert(requireComparable(value));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private AVLTreeNode<Object> avlFind(Object value) {
        return (AVLTreeNode<Object>) avlTree.find(requireComparable(value));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean avlRemove(Object value) {
        return avlTree.remove(requireComparable(value));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void avlInitializeSorted(List<Object> values) {
        avlTree.initializeSorted((List) values);
    }

    @SuppressWarnings("rawtypes")
    private Comparable requireComparable(Object value) {
        if (!(value instanceof Comparable<?> comparable)) {
            throw new IllegalArgumentException("AVL value type must implement Comparable: " + value.getClass().getName());
        }
        return (Comparable) comparable;
    }

    private Object parseValue(TextField field) {
        try {
            return valueAdapter.parse(field.getText());
        } catch (RuntimeException exception) {
            logI18n("message.error.invalid_tree_input");
            return null;
        }
    }


    private GeneralTreeNode<Object> selectedGeneralNode() {
        if (selectedNodeId == null) {
            logI18n("message.tree.select_node");
            return null;
        }
        GeneralTreeNode<Object> node = generalTree.findById(selectedNodeId);
        if (node == null) {
            clearNodeSelection();
            logI18n("message.tree.select_node");
            return null;
        }
        return node;
    }

    private AVLTreeNode<Object> selectedAvlNode() {
        if (selectedNodeId == null) {
            logI18n("message.tree.select_node");
            return null;
        }
        AVLTreeNode<Object> node = avlNodeById(avlRoot(), selectedNodeId);
        if (node == null) {
            clearNodeSelection();
            logI18n("message.tree.select_node");
            return null;
        }
        return node;
    }

    private void clearNodeSelection() {
        selectedNodeId = null;
        algorithmSelectedNodeId = null;
        treeVisualizer().clearSelection();
        requestPresentationRender();
        selectionListener.accept(null);
        refreshOperationAvailability();
    }

    private void refreshVariantControls() {
        refreshAlgorithmIds();
        refreshAlgorithmSelector();
        refreshOperationVisibility();
        refreshOperationLabels();
        refreshOperationAvailability();
    }

    private void refreshAlgorithmIds() {
        if (activeVariant == TreeVariant.GENERAL) {
            algorithmIds = AlgorithmCatalog.compatibleAlgorithms(com.majortom.algorithms.structure.tree.GeneralTreeStructure.class, runtimeValueType);
        } else {
            algorithmIds = AlgorithmCatalog.compatibleAlgorithms(com.majortom.algorithms.structure.tree.AvlTreeStructure.class, runtimeValueType);
        }
    }

    private void refreshAlgorithmSelector() {
        if (algorithmSelector == null) {
            return;
        }
        javafx.collections.ObservableList<String> labels = FXCollections.observableArrayList();
        for (String id : algorithmIds) {
            labels.add(id);
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


    private void refreshOperationAvailability() {
        boolean hasSelection = selectedNodeId != null;
        if (activeVariant == TreeVariant.GENERAL && hasSelection) {
            hasSelection = generalTree.findById(selectedNodeId) != null;
        }
        if (activeVariant == TreeVariant.AVL && hasSelection) {
            hasSelection = avlNodeById(avlRoot(), selectedNodeId) != null;
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

    private void requireTreeSnapshot(StructureSnapshot<TreeSnapshotState<Object>> snapshot) {
        if (!moduleId().equals(snapshot.moduleId())) {
            throw new IllegalArgumentException("snapshot belongs to module " + snapshot.moduleId());
        }
        snapshot.requireValueType(runtimeValueType);
    }

    private boolean snapshotMatchesActiveVariant(TreeSnapshotState<Object> state) {
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
        algorithmLogI18n("message.execution.finished");
    }

    @Override
    public String structureSummaryText() {
        if (activeVariant == TreeVariant.GENERAL) {
            GeneralTreeSnapshot<Object> snapshot = currentGeneralSnapshot();
            Object rootValue = I18N.text("label.workspace.selection.none");
            if (snapshot.root() != null) {
                rootValue = snapshot.root().value();
            }
            return I18N.text("label.workspace.structure.summary.tree",
                    snapshot.size(), generalHeight(snapshot.root()), rootValue);
        }
        BinaryTreeSnapshot<Object> snapshot = currentAvlSnapshot();
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
                formatMetric("stats.action", stats.metric(MetricKeys.NODES_VISITED) + stats.metric(MetricKeys.EDGES_EXAMINED)));
    }

    @Override
    protected Object structureMemoryRoot() {
        return activeVariant == TreeVariant.GENERAL ? generalTree : avlTree;
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
    public Class<?> runtimeValueType() {
        return runtimeValueType;
    }

    @Override public boolean hasValues() { return generalTree.size() > 0 || avlTree.size() > 0; }

    @Override
    public List<Class<?>> supportedValueTypes() {
        return ValueAdapters.supportedTypes().stream()
                .filter(type -> Comparable.class.isAssignableFrom(type)).toList();
    }

    @Override
    public void setRuntimeValueType(Class<?> valueType) {
        if (!supportedValueTypes().contains(valueType)) {
            throw new IllegalArgumentException("Unsupported Tree value type: " + valueType.getName());
        }
        Class<?> resolved = valueType;
        if (resolved.equals(runtimeValueType)) {
            return;
        }
        if (!Comparable.class.isAssignableFrom(resolved)) {
            throw new IllegalArgumentException("tree runtime value type must implement Comparable: " + resolved.getName());
        }
        ValueAdapter<Object> nextAdapter = ValueAdapters.requireObjectAdapter(resolved);
        runtimeValueType = resolved;
        valueAdapter = nextAdapter;
        if (randomBtn != null) randomBtn.setDisable(!ValueAdapters.canGenerate(resolved));
        algorithmInputSnapshot = null;
        clearNodeSelection();
        generalTree = new Tree<>();
        avlTree = new AVLTree<>();
        refreshAlgorithmIds();
        invalidateExecutionForInputChange();
        if (controlPanel != null) {
            refreshVariantControls();
            refreshStructureView();
        } else {
            renderStructureState(currentStructureState());
        }
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
        bindButton(addChildBtn, "action.tree.add_child");
        bindButton(addParentBtn, "action.tree.add_parent");
        bindButton(deleteBtn, "action.tree.delete");
        bindButton(updateBtn, "action.tree.update");
        bindButton(randomBtn, "action.tree.random");
        refreshOperationLabels();
    }

    @Override
    protected String moduleId() {
        return StructureIds.TREE;
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
        structureSelector.setItems(FXCollections.observableArrayList(
                StructureIds.TREE, StructureIds.AVL_TREE));
        localizeChoiceCells(structureSelector, StructureCatalog::name);
        localizeChoiceCells(algorithmSelector, AlgorithmCatalog::name);
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
            refreshOperationLabels();
            FxDispatch.defer(this::syncStructureSelectorSelection);
        });
        FxDispatch.defer(() -> {
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
