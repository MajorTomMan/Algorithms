package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.registry.ModuleLoader;
import com.majortom.algorithms.core.registry.ModuleRegistry;
import com.majortom.algorithms.utils.EffectUtils;
import com.majortom.algorithms.visualization.BaseController;
import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.WorkbenchControls;
import com.majortom.algorithms.visualization.algorithm.AlgorithmLabels;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.module.WorkbenchModuleDefinition;
import com.majortom.algorithms.visualization.module.AlgorithmSelectionSupport;
import com.majortom.algorithms.visualization.module.WorkbenchModules;
import com.majortom.algorithms.visualization.structure.InMemoryStructureSnapshotStore;
import com.majortom.algorithms.core.domain.execution.ExecutionLifecycleEvent;
import com.majortom.algorithms.core.logging.LogEvent;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.core.snapshot.SnapshotLifecycleEvent;
import com.majortom.algorithms.core.snapshot.StructureSnapshot;
import com.majortom.algorithms.visualization.structure.StructureSnapshotSupport;
import com.majortom.algorithms.visualization.structure.SnapshotAlgorithmInputSupport;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import com.majortom.algorithms.visualization.logging.LogView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * 单 Workbench JavaFX 外壳。
 *
 * <p>Structure 与 Algorithm 是同一工作区的两个互斥模式。Structure 模式负责
 * 编辑真实结构和快照，Algorithm 模式消费当前或已保存快照的隔离副本并负责
 * 执行、时间线、统计和日志。</p>
 */
public class MainController implements Initializable {

    private static final PseudoClass COMPACT_LAYOUT = PseudoClass.getPseudoClass("compact-layout");
    private static final PseudoClass NARROW_LAYOUT = PseudoClass.getPseudoClass("narrow-layout");
    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
    private static final PseudoClass WORKSPACE_FOCUS = PseudoClass.getPseudoClass("workspace-focus");
    private static final double COMPACT_LAYOUT_WIDTH = 1180.0d;
    private static final double NARROW_LAYOUT_WIDTH = 980.0d;
    private static final DateTimeFormatter SNAPSHOT_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final List<String> OFFICIAL_VALUE_TYPES = List.of(
            "Integer", "Long", "Double", "Float", "Boolean", "Character", "Byte", "Short", "String");

    @FXML
    private BorderPane rootPane;
    @FXML
    private VBox topShell;
    @FXML
    private HBox topBar;
    @FXML
    private HBox workspaceModeBox;
    @FXML
    private VBox structureNavigationBox;
    @FXML
    private VBox algorithmNavigationBox;
    @FXML
    private Button structureWorkspaceBtn;
    @FXML
    private Button algorithmWorkspaceBtn;
    @FXML
    private Button langBtn;
    @FXML
    private HBox valueTypeBox;
    @FXML
    private Label valueTypeLabel;
    @FXML
    private ComboBox<ValueTypeOption> valueTypeSelector;
    @FXML
    private Label hashValueTypeLabel;
    @FXML
    private ComboBox<ValueTypeOption> hashValueTypeSelector;
    @FXML
    private StackPane workspaceLayer;
    @FXML
    private VBox structureWorkspacePane;
    @FXML
    private VBox algorithmWorkspacePane;
    @FXML
    private HBox structureWorkspaceBody;
    @FXML
    private HBox algorithmWorkspaceBody;
    @FXML
    private VBox structureControlsHost;
    @FXML
    private VBox algorithmControlsHost;
    @FXML
    private HBox customControlBox;
    @FXML
    private VBox structureControlRail;
    @FXML
    private VBox algorithmControlRail;
    @FXML
    private VBox snapshotPanel;
    @FXML
    private VBox snapshotCards;
    @FXML
    private Label structureHistoryTitleLabel;
    @FXML
    private Label structureHistoryCountLabel;
    @FXML
    private VBox structureHistoryCards;
    @FXML
    private StackPane structurePreviewViewport;
    @FXML
    private VBox structurePreviewEmpty;
    @FXML
    private StackPane visualizationViewport;
    @FXML
    private StackPane visualizationContainer;
    @FXML
    private VBox diagnosticsPanel;
    @FXML
    private VBox bottomDock;
    @FXML
    private HBox playbackToolbar;
    @FXML
    private HBox timelineRow;
    @FXML
    private Label menuTitleLabel;
    @FXML
    private Label structureWorkspaceTitleLabel;
    @FXML
    private Label structureWorkspaceSubtitleLabel;
    @FXML
    private Label structureControlsTitleLabel;
    @FXML
    private Label algorithmWorkspaceTitleLabel;
    @FXML
    private Label algorithmWorkspaceSubtitleLabel;
    @FXML
    private Label algorithmInputSourceLabel;
    @FXML
    private Label algorithmControlsTitleLabel;
    @FXML
    private Label structurePreviewTitleLabel;
    @FXML
    private Label structurePreviewHintLabel;
    @FXML
    private Label snapshotTitleLabel;
    @FXML
    private Label snapshotCountLabel;
    @FXML
    private Button saveSnapshotBtn;
    @FXML
    private Label algorithmViewTitleLabel;
    @FXML
    private Label viewportHintLabel;
    @FXML
    private Label statsTitleLabel;
    @FXML
    private Label liveLabel;
    @FXML
    private Label logTitleLabel;
    @FXML
    private Label statsLabel;
    @FXML
    private Label delayLabel;
    @FXML
    private Label timelineLabel;
    @FXML
    private LogView logView;
    @FXML
    private Button startBtn;
    @FXML
    private Button pauseBtn;
    @FXML
    private Button resetBtn;
    @FXML
    private Button replayBtn;
    @FXML
    private Button stepBackwardBtn;
    @FXML
    private Button stepForwardBtn;
    @FXML
    private Button exportBtn;
    @FXML
    private Button compareBtn;
    @FXML
    private Slider delaySlider;
    @FXML
    private Slider timelineSlider;

    private static final ModuleRegistry MODULE_REGISTRY = ModuleLoader.load();

    private final List<WorkbenchModuleDefinition> moduleDefinitions = WorkbenchModules.available(MODULE_REGISTRY);
    private final InMemoryStructureSnapshotStore structureSnapshotStore =
            new InMemoryStructureSnapshotStore();
    private final Map<String, List<Button>> structureButtons = new LinkedHashMap<>();
    private final Map<String, Map<String, Button>> algorithmButtons = new LinkedHashMap<>();
    private final Map<String, String> selectedValueTypes = new LinkedHashMap<>();
    private String selectedHashKeyType;
    private String selectedHashValueType;
    private boolean updatingValueTypeSelectors;
    private BaseController<?> currentSubController;
    private WorkbenchModuleDefinition activeDefinition;
    private javafx.beans.value.ChangeListener<Number> structureRevisionListener;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (resources != null) {
            I18N.setLocale(resources.getLocale());
        }

        setupI18n();
        setupValueTypeSelectors();
        setupModuleMenu();
        setupWorkspaceMode();
        setupGlobalEffects();
        setupLayoutClips();
        setupResponsiveLayout();

        if (!moduleDefinitions.isEmpty()) {
            switchToModule(moduleDefinitions.getFirst());
        }
        appendSystemLog(I18N.text("message.system.initialized"));
    }

    private void setupI18n() {
        menuTitleLabel.textProperty().bind(I18N.createStringBinding("label.menu.title"));
        valueTypeLabel.textProperty().bind(I18N.createStringBinding("label.value_type"));
        hashValueTypeLabel.textProperty().bind(I18N.createStringBinding("label.value_type.value"));
        structureWorkspaceBtn.textProperty().bind(I18N.createStringBinding("label.workspace.structure"));
        algorithmWorkspaceBtn.textProperty().bind(I18N.createStringBinding("label.workspace.algorithm"));
        structureWorkspaceTitleLabel.textProperty().bind(
                I18N.createStringBinding("label.workspace.structure.workspace"));
        algorithmWorkspaceTitleLabel.textProperty().bind(
                I18N.createStringBinding("label.workspace.algorithm.workspace"));
        structureControlsTitleLabel.textProperty().bind(
                I18N.createStringBinding("label.workspace.structure.controls"));
        algorithmControlsTitleLabel.textProperty().bind(
                I18N.createStringBinding("label.workspace.algorithm.controls"));
        structurePreviewTitleLabel.textProperty().bind(
                I18N.createStringBinding("label.workspace.structure.preview"));
        structurePreviewHintLabel.textProperty().bind(
                I18N.createStringBinding("label.workspace.structure.preview.hint"));
        snapshotTitleLabel.textProperty().bind(I18N.createStringBinding("label.workspace.snapshots"));
        structureHistoryTitleLabel.textProperty().bind(I18N.createStringBinding("label.workspace.structure.history"));
        saveSnapshotBtn.textProperty().bind(I18N.createStringBinding("action.workspace.save_snapshot"));
        algorithmViewTitleLabel.setText(I18N.text("label.workspace.algorithm.preview"));
        viewportHintLabel.textProperty().bind(
                I18N.createStringBinding("label.workspace.algorithm.preview.hint"));
        statsTitleLabel.textProperty().bind(I18N.createStringBinding("label.panel.stats"));
        logTitleLabel.textProperty().bind(I18N.createStringBinding("label.panel.log"));
        liveLabel.textProperty().bind(I18N.createStringBinding("label.panel.live"));
        startBtn.textProperty().bind(I18N.createStringBinding("action.execution.start"));
        resetBtn.textProperty().bind(I18N.createStringBinding("action.execution.reset"));
        replayBtn.textProperty().bind(I18N.createStringBinding("action.execution.replay"));
        exportBtn.textProperty().bind(I18N.createStringBinding("action.execution.export"));
        compareBtn.textProperty().bind(I18N.createStringBinding("action.execution.compare"));
        delayLabel.textProperty().bind(I18N.createStringBinding("label.execution.delay"));
        timelineLabel.textProperty().bind(I18N.createStringBinding("label.execution.timeline"));
        Label logPlaceholder = new Label();
        logPlaceholder.textProperty().bind(I18N.createStringBinding("label.panel.log.prompt"));
        logView.setPlaceholder(logPlaceholder);
        stepBackwardBtn.accessibleTextProperty().bind(
                I18N.createStringBinding("action.execution.step.backward"));
        stepForwardBtn.accessibleTextProperty().bind(
                I18N.createStringBinding("action.execution.step.forward"));
        I18N.localeProperty().addListener((observable, oldValue, newValue) -> {
            refreshPauseText();
            refreshWorkspaceContext();
            refreshAlgorithmInputSource();
            refreshValueTypeSelectors();
        });
        refreshPauseText();
    }

    private void setupValueTypeSelectors() {
        configureValueTypeSelector(valueTypeSelector);
        configureValueTypeSelector(hashValueTypeSelector);
        valueTypeSelector.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (updatingValueTypeSelectors || activeDefinition == null || newValue == null) {
                return;
            }
            if (!newValue.available()) {
                appendSystemLog(I18N.text("message.value_type.unavailable", newValue.type()));
                refreshValueTypeSelectors();
                return;
            }
            if ("hash-table".equals(activeDefinition.id())) {
                selectedHashKeyType = newValue.type();
            } else {
                selectedValueTypes.put(activeDefinition.id(), newValue.type());
            }
            refreshAfterValueTypeChange();
        });
        hashValueTypeSelector.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (updatingValueTypeSelectors || activeDefinition == null || newValue == null) {
                return;
            }
            if (!newValue.available()) {
                appendSystemLog(I18N.text("message.value_type.unavailable", newValue.type()));
                refreshValueTypeSelectors();
                return;
            }
            selectedHashValueType = newValue.type();
            refreshAfterValueTypeChange();
        });
    }

    private void configureValueTypeSelector(ComboBox<ValueTypeOption> selector) {
        selector.setCellFactory(ignored -> valueTypeCell());
        selector.setButtonCell(valueTypeCell());
    }

    private ListCell<ValueTypeOption> valueTypeCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(ValueTypeOption item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setDisable(false);
                    return;
                }
                setText(item.available() ? item.type() : item.type() + " · " + I18N.text("label.value_type.unavailable"));
                setDisable(!item.available());
            }
        };
    }

    private void refreshValueTypeSelectors() {
        if (activeDefinition == null) {
            return;
        }
        String moduleId = activeDefinition.id();
        boolean maze = "maze".equals(moduleId);
        setControlVisibility(valueTypeBox, !maze);
        if (maze) {
            return;
        }

        updatingValueTypeSelectors = true;
        try {
            boolean hashTable = "hash-table".equals(moduleId);
            setControlVisibility(hashValueTypeLabel, hashTable);
            setControlVisibility(hashValueTypeSelector, hashTable);
            if (hashTable) {
                valueTypeLabel.textProperty().unbind();
                valueTypeLabel.setText(I18N.text("label.value_type.key"));
                refreshHashTableTypeSelectors();
            } else {
                if (!valueTypeLabel.textProperty().isBound()) {
                    valueTypeLabel.textProperty().bind(I18N.createStringBinding("label.value_type"));
                }
                List<String> available = availableValueTypes(moduleId);
                String selected = selectedValueTypes.get(moduleId);
                if (selected == null || !available.contains(selected)) {
                    selected = available.isEmpty() ? null : available.getFirst();
                    if (selected != null) {
                        selectedValueTypes.put(moduleId, selected);
                    }
                }
                valueTypeSelector.getItems().setAll(valueTypeOptions(available));
                selectValueType(valueTypeSelector, selected);
            }
        } finally {
            updatingValueTypeSelectors = false;
        }
    }

    private void refreshHashTableTypeSelectors() {
        List<String> signatures = MODULE_REGISTRY.structureTypeSignatures("hash-table");
        List<String> keyTypes = new ArrayList<>();
        for (String signature : signatures) {
            int separator = signature.indexOf('.');
            if (separator > 0) {
                String keyType = signature.substring(0, separator);
                if (!keyTypes.contains(keyType)) {
                    keyTypes.add(keyType);
                }
            }
        }
        if (selectedHashKeyType == null || !keyTypes.contains(selectedHashKeyType)) {
            selectedHashKeyType = keyTypes.isEmpty() ? null : keyTypes.getFirst();
        }
        valueTypeSelector.getItems().setAll(valueTypeOptions(keyTypes));
        selectValueType(valueTypeSelector, selectedHashKeyType);

        List<String> valueTypes = new ArrayList<>();
        if (selectedHashKeyType != null) {
            String prefix = selectedHashKeyType + ".";
            for (String signature : signatures) {
                if (signature.startsWith(prefix)) {
                    valueTypes.add(signature.substring(prefix.length()));
                }
            }
        }
        if (selectedHashValueType == null || !valueTypes.contains(selectedHashValueType)) {
            selectedHashValueType = valueTypes.isEmpty() ? null : valueTypes.getFirst();
        }
        hashValueTypeSelector.getItems().setAll(valueTypeOptions(valueTypes));
        selectValueType(hashValueTypeSelector, selectedHashValueType);
    }

    private List<String> availableValueTypes(String moduleId) {
        List<String> available = new ArrayList<>();
        for (String signature : MODULE_REGISTRY.structureTypeSignatures(moduleId)) {
            if (!signature.contains(".") && !available.contains(signature)) {
                available.add(signature);
            }
        }
        for (String valueType : MODULE_REGISTRY.algorithmValueTypes(moduleId)) {
            if (!available.contains(valueType)) {
                available.add(valueType);
            }
        }
        return List.copyOf(available);
    }

    private List<ValueTypeOption> valueTypeOptions(List<String> available) {
        List<ValueTypeOption> options = new ArrayList<>();
        for (String type : OFFICIAL_VALUE_TYPES) {
            options.add(new ValueTypeOption(type, available.contains(type)));
        }
        return List.copyOf(options);
    }

    private void selectValueType(ComboBox<ValueTypeOption> selector, String type) {
        selector.getSelectionModel().clearSelection();
        if (type == null) {
            return;
        }
        for (ValueTypeOption option : selector.getItems()) {
            if (option.type().equals(type)) {
                selector.getSelectionModel().select(option);
                return;
            }
        }
    }

    private String selectedValueType(String moduleId) {
        String selected = selectedValueTypes.get(moduleId);
        if (selected != null) {
            return selected;
        }
        List<String> available = availableValueTypes(moduleId);
        if (available.isEmpty()) {
            return null;
        }
        selected = available.getFirst();
        selectedValueTypes.put(moduleId, selected);
        return selected;
    }

    private void refreshAfterValueTypeChange() {
        refreshValueTypeSelectors();
        rebuildAlgorithmMenu();
        if (activeDefinition != null) {
            updateAlgorithmWorkspaceAvailability(activeDefinition.id());
            clearAlgorithmSelection();
            selectFirstAlgorithmButton(activeDefinition.id());
            List<AlgorithmNavigationItem> items = algorithmNavigationItems(activeDefinition.id());
            if (!items.isEmpty() && currentSubController instanceof AlgorithmSelectionSupport support) {
                support.selectAlgorithm(items.getFirst().id());
            }
        }
    }

    private void setupModuleMenu() {
        structureNavigationBox.getChildren().clear();
        structureButtons.clear();
        for (WorkbenchModuleDefinition definition : moduleDefinitions) {
            Button structureButton = createCatalogButton(definition);
            structureNavigationBox.getChildren().add(structureButton);
        }
        rebuildAlgorithmMenu();
    }

    private void rebuildAlgorithmMenu() {
        algorithmNavigationBox.getChildren().clear();
        algorithmButtons.clear();
        for (WorkbenchModuleDefinition definition : moduleDefinitions) {
            List<AlgorithmNavigationItem> navigationItems = algorithmNavigationItems(definition.id());
            if (navigationItems.isEmpty()) {
                continue;
            }
            VBox group = new VBox(4);
            group.getStyleClass().add("sidebar-catalog-group");
            Label groupTitle = new Label();
            groupTitle.getStyleClass().add("sidebar-group-title");
            groupTitle.textProperty().bind(I18N.createStringBinding(structureLabelKey(definition.id())));
            group.getChildren().add(groupTitle);
            for (AlgorithmNavigationItem item : navigationItems) {
                group.getChildren().add(createAlgorithmButton(definition, item));
            }
            algorithmNavigationBox.getChildren().add(group);
        }
    }

    private Button createCatalogButton(WorkbenchModuleDefinition definition) {
        Button button = new Button();
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("menu-button");
        button.getStyleClass().add("module-button");
        button.getStyleClass().add("sidebar-catalog-button");
        button.getStyleClass().add(moduleAccentStyleClass(definition.id()));
        button.textProperty().bind(I18N.createStringBinding(structureLabelKey(definition.id())));
        button.setOnAction(event -> switchToModule(definition));
        structureButtons.computeIfAbsent(definition.id(), ignored -> new java.util.ArrayList<>()).add(button);
        return button;
    }

    private Button createAlgorithmButton(
            WorkbenchModuleDefinition definition,
            AlgorithmNavigationItem item) {
        Button button = new Button();
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("sidebar-algorithm-button");
        button.getStyleClass().add(moduleAccentStyleClass(definition.id()));
        button.textProperty().bind(I18N.createStringBinding(item.labelKey()));
        button.setOnAction(event -> selectAlgorithm(definition, item.id()));
        algorithmButtons.computeIfAbsent(definition.id(), ignored -> new LinkedHashMap<>())
                .put(item.id(), button);
        return button;
    }

    private void selectAlgorithm(WorkbenchModuleDefinition definition, String algorithmId) {
        if (activeDefinition == null || !activeDefinition.id().equals(definition.id())) {
            switchToModule(definition);
        }
        setWorkspaceMode(false);
        if (currentSubController instanceof AlgorithmSelectionSupport support
                && support.selectAlgorithm(algorithmId)) {
            selectAlgorithmButton(definition.id(), algorithmId);
        }
    }

    private void selectAlgorithmButton(String moduleId, String algorithmId) {
        algorithmButtons.values().forEach(buttons -> buttons.values().forEach(button ->
                button.pseudoClassStateChanged(SELECTED, false)));
        Map<String, Button> buttons = algorithmButtons.get(moduleId);
        if (buttons != null) {
            Button selectedButton = buttons.get(algorithmId);
            if (selectedButton != null) {
                selectedButton.pseudoClassStateChanged(SELECTED, true);
            }
        }
    }

    private List<AlgorithmNavigationItem> algorithmNavigationItems(String moduleId) {
        List<String> algorithmIds = new ArrayList<>();
        if ("maze".equals(moduleId)) {
            addAlgorithmsForAllTypes(algorithmIds, "maze", null);
            for (String valueType : MODULE_REGISTRY.algorithmValueTypes("graph")) {
                for (String algorithmId : MODULE_REGISTRY.algorithmIds("graph", valueType)) {
                    if (algorithmId.startsWith("graph-generator-")) {
                        algorithmIds.add(algorithmId);
                    }
                }
            }
        } else {
            String valueType = selectedValueType(moduleId);
            if (valueType != null) {
                for (String algorithmId : MODULE_REGISTRY.algorithmIds(moduleId, valueType)) {
                    if (!"graph".equals(moduleId) || !algorithmId.startsWith("graph-generator-")) {
                        algorithmIds.add(algorithmId);
                    }
                }
            }
        }

        List<AlgorithmNavigationItem> items = new ArrayList<>();
        for (String algorithmId : algorithmIds) {
            items.add(new AlgorithmNavigationItem(algorithmId, algorithmLabelKey(moduleId, algorithmId)));
        }
        return List.copyOf(items);
    }

    private void addAlgorithmsForAllTypes(List<String> target, String family, String excludedPrefix) {
        for (String valueType : MODULE_REGISTRY.algorithmValueTypes(family)) {
            for (String algorithmId : MODULE_REGISTRY.algorithmIds(family, valueType)) {
                if (excludedPrefix == null || !algorithmId.startsWith(excludedPrefix)) {
                    target.add(algorithmId);
                }
            }
        }
    }

    private String algorithmLabelKey(String moduleId, String algorithmId) {
        if ("maze".equals(moduleId) && "graph-generator-bfs".equals(algorithmId)) {
            return "algorithm.maze.generate.graph_bfs";
        }
        return AlgorithmLabels.key(algorithmId);
    }

    private record ValueTypeOption(String type, boolean available) {
    }

    private record AlgorithmNavigationItem(String id, String labelKey) {
    }

    private String structureLabelKey(String moduleId) {
        return switch (moduleId) {
            case "array" -> "label.structure.array";
            case "linked-list" -> "label.structure.linked_list";
            case "stack" -> "label.structure.stack";
            case "queue" -> "label.structure.queue";
            case "maze" -> "label.structure.grid";
            case "tree" -> "label.structure.tree";
            case "graph" -> "label.structure.graph";
            case "hash-table" -> "label.structure.hash_table";
            case "string" -> "label.structure.string";
            default -> "label.workspace.structure";
        };
    }

    private void setupWorkspaceMode() {
        setWorkspaceMode(true);
    }

    @FXML
    private void selectStructureWorkspace() {
        setWorkspaceMode(true);
    }

    @FXML
    private void selectAlgorithmWorkspace() {
        setWorkspaceMode(false);
    }

    private void setWorkspaceMode(boolean structure) {
        structureWorkspaceBtn.pseudoClassStateChanged(SELECTED, structure);
        algorithmWorkspaceBtn.pseudoClassStateChanged(SELECTED, !structure);
        structureWorkspacePane.pseudoClassStateChanged(WORKSPACE_FOCUS, structure);
        algorithmWorkspacePane.pseudoClassStateChanged(WORKSPACE_FOCUS, !structure);
        setPageVisibility(structureWorkspacePane, structure);
        setPageVisibility(algorithmWorkspacePane, !structure);
        refreshExecutionDockVisibility(structure);
        attachVisualizer(structure);
        if (structure && currentSubController != null) {
            currentSubController.showStructureState();
        }
        if (!structure && currentSubController != null) {
            currentSubController.showAlgorithmState();
        }
        if (structure) {
            structureWorkspacePane.requestFocus();
            return;
        }
        algorithmWorkspacePane.requestFocus();
    }

    private void setupGlobalEffects() {
        EffectUtils.applyDynamicEffect(
                structureWorkspaceBtn, algorithmWorkspaceBtn, langBtn,
                startBtn, pauseBtn, resetBtn, replayBtn, stepBackwardBtn,
                stepForwardBtn, exportBtn, compareBtn, saveSnapshotBtn);
    }

    private void setupLayoutClips() {
        bindClip(topShell);
        bindClip(topBar);
        bindClip(workspaceLayer);
        bindClip(structureWorkspacePane);
        bindClip(algorithmWorkspacePane);
        bindClip(bottomDock);
        bindClip(structurePreviewViewport);
        bindClip(visualizationViewport);
    }

    private void bindClip(Region region) {
        if (region == null) {
            return;
        }
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(region.widthProperty());
        clip.heightProperty().bind(region.heightProperty());
        region.setClip(clip);
    }

    private void setupResponsiveLayout() {
        if (rootPane == null) {
            return;
        }
        rootPane.widthProperty().addListener((observable, oldValue, newValue) ->
                updateResponsiveLayout(newValue.doubleValue()));
        updateResponsiveLayout(rootPane.getWidth());
    }

    private void updateResponsiveLayout(double width) {
        if (rootPane == null) {
            return;
        }
        boolean nextCompactLayout = width > 0.0d && width < COMPACT_LAYOUT_WIDTH;
        boolean nextNarrowLayout = width > 0.0d && width < NARROW_LAYOUT_WIDTH;
        rootPane.pseudoClassStateChanged(COMPACT_LAYOUT, nextCompactLayout);
        rootPane.pseudoClassStateChanged(NARROW_LAYOUT, nextNarrowLayout);
        snapshotPanel.setManaged(true);
        snapshotPanel.setVisible(true);
        snapshotPanel.setPrefWidth(nextNarrowLayout ? 140.0d : nextCompactLayout ? 160.0d : 180.0d);
        resizeRail(structureControlRail, nextCompactLayout);
        resizeRail(algorithmControlRail, nextCompactLayout);
        resizeRail(diagnosticsPanel, nextCompactLayout);
        setControlVisibility(langBtn, !nextNarrowLayout);
    }

    private void setPageVisibility(VBox page, boolean visible) {
        if (page == null) {
            return;
        }
        page.setManaged(visible);
        page.setVisible(visible);
    }

    private void resizeRail(Region rail, boolean compact) {
        if (rail == null) {
            return;
        }
        if (compact) {
            rail.setPrefWidth(200.0d);
            return;
        }
        rail.setPrefWidth(220.0d);
    }

    private void setControlVisibility(Node control, boolean visible) {
        if (control == null) {
            return;
        }
        control.setManaged(visible);
        control.setVisible(visible);
    }

    private void switchToModule(WorkbenchModuleDefinition definition) {
        activeDefinition = definition;
        loadSubController(definition.controllerFactory().get());
        refreshValueTypeSelectors();
        updateAlgorithmWorkspaceAvailability(definition.id());
        refreshWorkspaceContext();
        structureButtons.forEach((id, buttons) -> buttons.forEach(button ->
                button.pseudoClassStateChanged(SELECTED, id.equals(definition.id()))));
        clearAlgorithmSelection();
        selectFirstAlgorithmButton(definition.id());
    }


    private void updateAlgorithmWorkspaceAvailability(String moduleId) {
        boolean available = !algorithmNavigationItems(moduleId).isEmpty();
        if (!available) {
            setWorkspaceMode(true);
        }
        refreshExecutionDockVisibility(isStructurePageVisible());
        updateWorkspaceInteractionState();
    }

    private void updateWorkspaceInteractionState() {
        boolean running = currentSubController != null && currentSubController.isRunning();
        boolean algorithmAvailable = activeDefinition != null
                && !algorithmNavigationItems(activeDefinition.id()).isEmpty();
        structureWorkspaceBtn.setDisable(running);
        algorithmWorkspaceBtn.setDisable(running || !algorithmAvailable);
        structureButtons.values().forEach(buttons -> buttons.forEach(button -> button.setDisable(running)));
        algorithmButtons.values().forEach(buttons -> buttons.values().forEach(button -> button.setDisable(running)));
    }

    private void refreshExecutionDockVisibility(boolean structureMode) {
        if (bottomDock == null) {
            return;
        }
        boolean algorithmAvailable = activeDefinition != null
                && !algorithmNavigationItems(activeDefinition.id()).isEmpty();
        boolean visible = !structureMode && algorithmAvailable;
        setPageVisibility(bottomDock, visible);
        bottomDock.setDisable(!visible);
    }

    private void clearAlgorithmSelection() {
        algorithmButtons.values().forEach(buttons -> buttons.values().forEach(button ->
                button.pseudoClassStateChanged(SELECTED, false)));
    }

    private void selectFirstAlgorithmButton(String moduleId) {
        Map<String, Button> buttons = algorithmButtons.get(moduleId);
        if (buttons == null || buttons.isEmpty()) {
            return;
        }
        buttons.values().iterator().next().pseudoClassStateChanged(SELECTED, true);
    }

    private void loadSubController(BaseController<?> newController) {
        detachCurrentController();
        visualizationContainer.getChildren().clear();
        structureControlsHost.getChildren().clear();
        algorithmControlsHost.getChildren().clear();
        customControlBox.getChildren().clear();

        newController.setUIReferences(new WorkbenchControls(
                statsLabel,
                logView,
                delaySlider,
                timelineSlider,
                customControlBox,
                startBtn,
                pauseBtn,
                resetBtn,
                replayBtn,
                stepBackwardBtn,
                stepForwardBtn,
                exportBtn,
                compareBtn));

        currentSubController = newController;
        currentSubController.pausedProperty().addListener(
                (observable, oldValue, newValue) -> refreshPauseText());
        currentSubController.runningProperty().addListener((observable, oldValue, newValue) -> {
            updateSnapshotActionState();
            updateWorkspaceInteractionState();
        });
        currentSubController.setupCustomControls(customControlBox);
        distributeModuleControls();
        structureRevisionListener = (observable, oldValue, newValue) -> refreshSnapshotCards();
        currentSubController.structureRevisionProperty().addListener(structureRevisionListener);

        BaseVisualizer<?> visualizer = newController.getVisualizer();
        if (visualizer != null) {
            attachVisualizer(isStructurePageVisible());
        }
        currentSubController.dispatchVisualizerAttached();
        refreshPauseText();
        updateWorkspaceInteractionState();
    }

    private void detachCurrentController() {
        if (currentSubController == null) {
            return;
        }
        if (structureRevisionListener != null) {
            currentSubController.structureRevisionProperty().removeListener(structureRevisionListener);
            structureRevisionListener = null;
        }
        BaseVisualizer<?> previousVisualizer = currentSubController.getVisualizer();
        if (previousVisualizer != null) {
            previousVisualizer.prefWidthProperty().unbind();
            previousVisualizer.prefHeightProperty().unbind();
            visualizationContainer.getChildren().remove(previousVisualizer);
            structurePreviewViewport.getChildren().remove(previousVisualizer);
        }
        currentSubController.dispatchVisualizerDetached();
        currentSubController = null;
    }

    /**
     * Places the one module visualizer in the currently visible page.
     *
     * <p>Keeping a single visualizer avoids two controllers or two event
     * streams. Rebinding its size when the page changes also means structure
     * edits remain visible on the structure page and algorithm frames remain
     * visible on the algorithm page.</p>
     */
    private void attachVisualizer(boolean structurePage) {
        if (currentSubController == null || currentSubController.getVisualizer() == null) {
            return;
        }
        BaseVisualizer<?> visualizer = currentSubController.getVisualizer();
        visualizer.prefWidthProperty().unbind();
        visualizer.prefHeightProperty().unbind();
        visualizationContainer.getChildren().remove(visualizer);
        structurePreviewViewport.getChildren().remove(visualizer);

        StackPane target = visualizationContainer;
        if (structurePage) {
            target = structurePreviewViewport;
        }
        if (!target.getChildren().contains(visualizer)) {
            target.getChildren().add(0, visualizer);
        }
        visualizer.prefWidthProperty().bind(target.widthProperty());
        visualizer.prefHeightProperty().bind(target.heightProperty());
        if (structurePreviewEmpty != null) {
            structurePreviewEmpty.setVisible(!structurePage);
            structurePreviewEmpty.setManaged(!structurePage);
        }
    }

    private boolean isStructurePageVisible() {
        return structureWorkspacePane != null && structureWorkspacePane.isManaged();
    }

    /** Moves FXML sections into the structure and algorithm rails without duplicating controls. */
    private void distributeModuleControls() {
        if (customControlBox.getChildren().isEmpty()) {
            return;
        }
        Node modulePanel = customControlBox.getChildren().getFirst();
        customControlBox.getChildren().clear();
        if (!(modulePanel instanceof Pane pane)) {
            structureControlsHost.getChildren().add(modulePanel);
            stretchControls(structureControlsHost);
            return;
        }

        List<Node> sections = List.copyOf(pane.getChildren());
        pane.getChildren().clear();
        if (sections.isEmpty()) {
            structureControlsHost.getChildren().add(modulePanel);
        }
        for (Node section : sections) {
            VBox target = isAlgorithmSection(section)
                    ? algorithmControlsHost : structureControlsHost;
            target.getChildren().add(section);
        }
        stretchControls(structureControlsHost);
        stretchControls(algorithmControlsHost);
    }

    private boolean isAlgorithmSection(Node section) {
        return section.getStyleClass().contains("algorithm-section")
                || section.getStyleClass().contains("execution-section");
    }

    private void stretchControls(VBox host) {
        for (Node child : host.getChildren()) {
            VBox.setVgrow(child, Priority.NEVER);
            if (child instanceof Region region) {
                region.setMaxWidth(Double.MAX_VALUE);
            }
        }
    }

    private void refreshWorkspaceContext() {
        if (activeDefinition == null) {
            return;
        }
        String moduleName = I18N.text(activeDefinition.labelKey());
        structureWorkspaceSubtitleLabel.setText(moduleName);
        algorithmWorkspaceSubtitleLabel.setText(moduleName);
        algorithmViewTitleLabel.setText(moduleName);
        refreshSnapshotCards();
        refreshAlgorithmInputSource();
    }

    private void refreshSnapshotCards() {
        if (activeDefinition == null || snapshotCards == null) {
            return;
        }
        String moduleName = I18N.text(activeDefinition.labelKey());
        StructureSnapshotSupport<?> support = currentSnapshotSupport();
        if (support == null) {
            snapshotCards.getChildren().clear();
            snapshotCountLabel.setText(I18N.text(
                    "label.workspace.snapshot.count", 0,
                    structureSnapshotStore.maxSnapshotsPerModule()));
            updateSnapshotActionState();
            refreshStructureHistory();
            return;
        }

        snapshotCards.getChildren().clear();
        StructureSnapshot<?> current = support.captureStructureSnapshot();
        snapshotCards.getChildren().add(createSnapshotCard(
                moduleName, I18N.text("label.workspace.snapshot.current"), current, support, true));

        List<StructureSnapshot<?>> saved = structureSnapshotStore.snapshots(activeDefinition.id());
        for (StructureSnapshot<?> snapshot : saved) {
            snapshotCards.getChildren().add(createSnapshotCard(
                    moduleName, I18N.text("label.workspace.snapshot.saved"), snapshot,
                    support, false));
        }
        if (saved.isEmpty()) {
            Label empty = new Label(I18N.text("label.workspace.snapshot.none"));
            empty.getStyleClass().add("snapshot-empty");
            empty.setWrapText(true);
            snapshotCards.getChildren().add(empty);
        }
        snapshotCountLabel.setText(I18N.text(
                "label.workspace.snapshot.count", saved.size(),
                structureSnapshotStore.maxSnapshotsPerModule()));
        updateSnapshotActionState();
        refreshStructureHistory();
    }

    private void refreshStructureHistory() {
        if (structureHistoryCards == null || structureHistoryCountLabel == null) {
            return;
        }
        structureHistoryCards.getChildren().clear();
        if (currentSubController == null) {
            structureHistoryCountLabel.setText("0");
            return;
        }
        List<EventEnvelope> domainEvents = currentSubController.structureEvents().stream()
                .filter(event -> !(event.event() instanceof ExecutionLifecycleEvent))
                .filter(event -> !(event.event() instanceof LogEvent))
                .toList();
        structureHistoryCountLabel.setText(String.valueOf(domainEvents.size()));
        if (domainEvents.isEmpty()) {
            Label empty = new Label(I18N.text("label.workspace.structure.history.none"));
            empty.getStyleClass().add("snapshot-empty");
            empty.setWrapText(true);
            structureHistoryCards.getChildren().add(empty);
            return;
        }
        int start = Math.max(0, domainEvents.size() - 12);
        for (int index = domainEvents.size() - 1; index >= start; index--) {
            structureHistoryCards.getChildren().add(createStructureHistoryCard(domainEvents.get(index)));
        }
    }

    private Node createStructureHistoryCard(EventEnvelope envelope) {
        VBox card = new VBox(3);
        card.getStyleClass().add("snapshot-card");
        card.setMaxWidth(Double.MAX_VALUE);
        Label eventName = new Label(envelope.event().getClass().getSimpleName());
        eventName.getStyleClass().add("snapshot-card-title");
        Label operation = new Label(shortOperationId(envelope.operationId()));
        operation.getStyleClass().add("snapshot-card-state");
        Label sequence = new Label("#" + envelope.sequence());
        sequence.getStyleClass().add("snapshot-card-time");
        card.getChildren().addAll(eventName, operation, sequence);
        return card;
    }

    private String shortOperationId(String operationId) {
        int lastDot = operationId.lastIndexOf('.');
        if (lastDot < 0 || lastDot + 1 >= operationId.length()) {
            return operationId;
        }
        return operationId.substring(lastDot + 1);
    }

    private Node createSnapshotCard(
            String moduleName,
            String status,
            StructureSnapshot<?> snapshot,
            StructureSnapshotSupport<?> support,
            boolean current) {
        VBox card = new VBox(6);
        card.setMaxWidth(Double.MAX_VALUE);
        card.getStyleClass().add("snapshot-card");
        card.getStyleClass().add(current ? "snapshot-card-current" : "snapshot-card-saved");

        Label title = new Label(current ? moduleName : moduleName + " · " + shortSnapshotId(snapshot));
        title.getStyleClass().add("snapshot-card-title");
        Label state = new Label(status);
        state.getStyleClass().add("snapshot-card-state");
        Label detail = new Label(describeSnapshot(support, snapshot));
        detail.getStyleClass().add("snapshot-card-detail");
        card.getChildren().addAll(title, state, detail);

        if (!current) {
            Label createdAt = new Label(I18N.text("label.workspace.snapshot.time", formatSnapshotTime(snapshot)));
            createdAt.getStyleClass().add("snapshot-card-detail");
            card.getChildren().add(createdAt);
        }

        SnapshotAlgorithmInputSupport<?> algorithmInputSupport = currentAlgorithmInputSupport();
        if (algorithmInputSupport != null) {
            String selectedSnapshotId = algorithmInputSupport.algorithmInputSnapshotId();
            boolean selected = current ? selectedSnapshotId == null : snapshot.id().equals(selectedSnapshotId);
            if (selected) {
                card.getStyleClass().add("snapshot-card-algorithm-input");
                Label inputState = new Label(I18N.text("label.workspace.snapshot.algorithm_input"));
                inputState.getStyleClass().add("snapshot-card-input-state");
                card.getChildren().add(inputState);
            }
        }

        HBox actions = new HBox(6);
        if (!current) {
            Button restore = new Button(I18N.text("action.workspace.restore_snapshot"));
            restore.getStyleClass().add("snapshot-card-action");
            restore.setOnAction(event -> restoreSnapshot(snapshot));
            actions.getChildren().add(restore);
        }
        if (algorithmInputSupport != null) {
            Button useInput = new Button(I18N.text(current
                    ? "action.workspace.use_current_input" : "action.workspace.use_snapshot_input"));
            useInput.getStyleClass().add("snapshot-card-action");
            useInput.setOnAction(event -> {
                if (current) {
                    algorithmInputSupport.useCurrentStructureAsAlgorithmInput();
                } else {
                    useSnapshotAsAlgorithmInputUnchecked(algorithmInputSupport, snapshot);
                }
                refreshSnapshotCards();
                refreshAlgorithmInputSource();
                appendSystemLog(I18N.text(current
                        ? "message.snapshot.input_current" : "message.snapshot.input_saved",
                        current ? "" : shortSnapshotId(snapshot)));
            });
            actions.getChildren().add(useInput);
        }
        if (!actions.getChildren().isEmpty()) {
            card.getChildren().add(actions);
        }
        return card;
    }

    private void refreshAlgorithmInputSource() {
        if (algorithmInputSourceLabel == null) {
            return;
        }
        if (activeDefinition != null && "maze".equals(activeDefinition.id())) {
            algorithmInputSourceLabel.setText(I18N.text("label.workspace.algorithm.input.maze"));
            return;
        }
        SnapshotAlgorithmInputSupport<?> support = currentAlgorithmInputSupport();
        if (support == null) {
            algorithmInputSourceLabel.setText(I18N.text("label.workspace.algorithm.input.parameters"));
            return;
        }
        String snapshotId = support.algorithmInputSnapshotId();
        if (snapshotId == null) {
            algorithmInputSourceLabel.setText(I18N.text("label.workspace.algorithm.input.current"));
            return;
        }
        algorithmInputSourceLabel.setText(I18N.text(
                "label.workspace.algorithm.input.snapshot", shortSnapshotId(snapshotId)));
    }

    private String shortSnapshotId(String snapshotId) {
        if (snapshotId == null || snapshotId.isBlank()) {
            return "-";
        }
        return snapshotId.length() <= 8 ? snapshotId : snapshotId.substring(0, 8);
    }

    private StructureSnapshotSupport<?> currentSnapshotSupport() {
        if (currentSubController instanceof StructureSnapshotSupport<?> support) {
            return support;
        }
        return null;
    }

    private SnapshotAlgorithmInputSupport<?> currentAlgorithmInputSupport() {
        if (currentSubController instanceof SnapshotAlgorithmInputSupport<?> support) {
            return support;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void useSnapshotAsAlgorithmInputUnchecked(
            SnapshotAlgorithmInputSupport<?> support,
            StructureSnapshot<?> snapshot) {
        SnapshotAlgorithmInputSupport<Object> typedSupport =
                (SnapshotAlgorithmInputSupport<Object>) support;
        StructureSnapshot<Object> typedSnapshot = (StructureSnapshot<Object>) snapshot;
        typedSupport.useSnapshotAsAlgorithmInput(typedSnapshot);
    }

    @FXML
    private void saveStructureSnapshot() {
        if (currentSubController == null || currentSubController.isRunning()) {
            return;
        }
        StructureSnapshotSupport<?> support = currentSnapshotSupport();
        if (support == null || activeDefinition == null) {
            return;
        }
        StructureSnapshot<?> snapshot = support.captureStructureSnapshot();
        structureSnapshotStore.save(snapshot);
        currentSubController.recordStructureEvent(
                "snapshot-created", new SnapshotLifecycleEvent.Created(snapshot.id(), snapshot.moduleId()));
        refreshSnapshotCards();
        appendSystemLog(I18N.text("message.snapshot.saved", shortSnapshotId(snapshot)));
    }

    private void restoreSnapshot(StructureSnapshot<?> snapshot) {
        if (currentSubController == null || currentSubController.isRunning()) {
            return;
        }
        StructureSnapshotSupport<?> support = currentSnapshotSupport();
        if (support == null || activeDefinition == null
                || !activeDefinition.id().equals(snapshot.moduleId())) {
            return;
        }
        try {
            restoreSnapshotUnchecked(support, snapshot);
            currentSubController.recordStructureEvent(
                    "snapshot-restored", new SnapshotLifecycleEvent.Restored(snapshot.id(), snapshot.moduleId()));
        } catch (RuntimeException exception) {
            appendSystemLog(I18N.text("message.snapshot.restore_failed"));
            return;
        }
        refreshSnapshotCards();
        appendSystemLog(I18N.text("message.snapshot.restored", shortSnapshotId(snapshot)));
    }

    @SuppressWarnings("unchecked")
    private void restoreSnapshotUnchecked(
            StructureSnapshotSupport<?> support,
            StructureSnapshot<?> snapshot) {
        StructureSnapshotSupport<Object> typedSupport =
                (StructureSnapshotSupport<Object>) support;
        StructureSnapshot<Object> typedSnapshot =
                (StructureSnapshot<Object>) snapshot;
        typedSupport.restoreStructureSnapshot(typedSnapshot);
    }

    @SuppressWarnings("unchecked")
    private String describeSnapshot(
            StructureSnapshotSupport<?> support,
            StructureSnapshot<?> snapshot) {
        StructureSnapshotSupport<Object> typedSupport =
                (StructureSnapshotSupport<Object>) support;
        StructureSnapshot<Object> typedSnapshot =
                (StructureSnapshot<Object>) snapshot;
        return typedSupport.describeStructureSnapshot(typedSnapshot.state());
    }

    private String formatSnapshotTime(StructureSnapshot<?> snapshot) {
        return SNAPSHOT_TIME_FORMATTER.format(
                snapshot.createdAt().atZone(ZoneId.systemDefault()));
    }

    private String shortSnapshotId(StructureSnapshot<?> snapshot) {
        String id = snapshot.id();
        int length = Math.min(8, id.length());
        return id.substring(0, length);
    }

    private void updateSnapshotActionState() {
        if (saveSnapshotBtn == null) {
            return;
        }
        saveSnapshotBtn.setDisable(
                currentSubController == null
                        || currentSubController.isRunning()
                        || currentSnapshotSupport() == null);
    }

    @FXML
    private void toggleLanguage() {
        Locale newLocale = Locale.CHINESE;
        if (I18N.getLocale().getLanguage().equals("zh")) {
            newLocale = Locale.ENGLISH;
        }
        I18N.setLocale(newLocale);
        appendSystemLog(I18N.text(
                "message.system.language_switched", newLocale.getDisplayLanguage(newLocale)));
    }

    private void appendSystemLog(String message) {
        if (logView == null) {
            return;
        }
        logView.appendSystem(message);
    }

    private String moduleAccentStyleClass(String moduleId) {
        return switch (moduleId) {
            case "array", "stack" -> "btn-ran-blue";
            case "linked-list", "tree" -> "btn-ran-gold";
            case "queue", "graph" -> "btn-ran-white";
            case "maze" -> "btn-ran-red";
            case "string" -> "btn-ran-gold";
            case "hash-table" -> "btn-ran-red";
            default -> "btn-ran-blue";
        };
    }

    private void refreshPauseText() {
        if (pauseBtn == null) {
            return;
        }
        boolean paused = currentSubController != null && currentSubController.isPaused();
        String key = "action.execution.pause";
        if (paused) {
            key = "action.execution.resume";
        }
        pauseBtn.setText(I18N.text(key));
    }
}
