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
import com.majortom.algorithms.visualization.logging.LogView;
import com.majortom.algorithms.visualization.structure.InMemoryStructureSnapshotStore;
import com.majortom.algorithms.core.domain.execution.ExecutionLifecycleEvent;
import com.majortom.algorithms.core.logging.LogEvent;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.core.runtime.ExecutionStatistics;
import com.majortom.algorithms.core.event.observation.ObservationEvent;
import com.majortom.algorithms.core.event.structure.TreeStructureEvent;
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
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Slider;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.net.URL;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

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
    private static final DateTimeFormatter EVENT_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final List<String> OFFICIAL_VALUE_TYPES = ModuleRegistry.valueTypes();
    private static final List<Integer> UI_FONT_SCALES = List.of(90, 100, 110, 125, 140);
    private static final int DEFAULT_UI_FONT_SCALE = 125;
    private static final double BASE_UI_FONT_SIZE = 13.0d;
    private static final String UI_FONT_SCALE_PREFERENCE = "ui.font.scale";
    private static final Preferences UI_PREFERENCES = Preferences.userNodeForPackage(MainController.class);
    private static final ModuleRegistry MODULE_REGISTRY = ModuleLoader.load();

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
    private Label fontScaleLabel;
    @FXML
    private ComboBox<String> fontScaleSelector;
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
    private HBox structureHistoryCards;
    @FXML
    private VBox structureSelectionOverlay;
    @FXML
    private Label selectedNodeIdLabel;
    @FXML
    private Label selectedNodeValueLabel;
    @FXML
    private Label structureInspectorBody;
    @FXML
    private Label structureOverviewLabel;
    @FXML
    private Label overviewPrimaryValue;
    @FXML
    private Label overviewSecondaryValue;
    @FXML
    private Label overviewEventsValue;
    @FXML
    private Label overviewStateValue;
    @FXML
    private VBox inspectorSnapshotCards;
    @FXML
    private Label structureNodeCountLabel;
    @FXML
    private Label structureHeightLabel;
    @FXML
    private Label structureStateLabel;
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
    private Label topContextLabel;
    @FXML
    private Label runStateLabel;
    @FXML
    private Label runIdLabel;
    @FXML
    private Label currentStepSequenceLabel;
    @FXML
    private Label currentStepKindLabel;
    @FXML
    private Label currentStepDetailLabel;
    @FXML
    private VBox currentStepOverlay;
    @FXML
    private Label eventKindLabel;
    @FXML
    private Region eventKindDot;
    @FXML
    private Label eventDetailsLabel;
    @FXML
    private Label resultLabel;
    @FXML
    private Label resultPreviewLabel;
    @FXML
    private Label runMetric1Title;
    @FXML
    private Label runMetric1Value;
    @FXML
    private Label runMetric2Title;
    @FXML
    private Label runMetric2Value;
    @FXML
    private Label runMetric3Title;
    @FXML
    private Label runMetric3Value;
    @FXML
    private Label runMetric4Title;
    @FXML
    private Label runMetric4Value;
    @FXML
    private Label timelineCursorLabel;
    @FXML
    private TabPane algorithmInspectorTabs;
    @FXML
    private Label timelinePositionLabel;
    @FXML
    private HBox timelineMarkers;
    @FXML
    private Button speed1Btn;
    @FXML
    private Button speed2Btn;
    @FXML
    private Button speed4Btn;
    @FXML
    private Button speed8Btn;
    @FXML
    private Button speed16Btn;
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
    private Button currentInputBtn;
    @FXML
    private Button savedInputBtn;
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

    private final List<WorkbenchModuleDefinition> moduleDefinitions = WorkbenchModules.available(MODULE_REGISTRY);
    private final InMemoryStructureSnapshotStore structureSnapshotStore =
            new InMemoryStructureSnapshotStore();
    private final Map<String, List<Button>> structureButtons = new LinkedHashMap<>();
    private final Map<String, Map<String, Button>> algorithmButtons = new LinkedHashMap<>();
    private final Map<String, String> selectedValueTypes = new LinkedHashMap<>();
    private String selectedHashKeyType;
    private String selectedHashValueType;
    private boolean updatingValueTypeSelectors;
    private int uiFontScale = DEFAULT_UI_FONT_SCALE;
    private BaseController<?> currentSubController;
    private WorkbenchModuleDefinition activeDefinition;
    private javafx.beans.value.ChangeListener<Number> structureRevisionListener;
    private String selectedAlgorithmId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (resources != null) {
            I18N.setLocale(resources.getLocale());
        }

        setupI18n();
        setupFontScaleSelector();
        setupValueTypeSelectors();
        setupModuleMenu();
        setupWorkspaceMode();
        setupPlaybackSpeedButtons();
        setupGlobalEffects();
        setupLayoutClips();
        setupResponsiveLayout();
        WorkbenchTheme.apply(rootPane);
        WorkbenchTheme.leftPill(structureWorkspaceBtn);
        WorkbenchTheme.rightPill(algorithmWorkspaceBtn);

        if (!moduleDefinitions.isEmpty()) {
            switchToModule(moduleDefinitions.getFirst());
        }
        appendSystemLog(I18N.text("message.system.initialized"));
    }

    private void setupI18n() {
        menuTitleLabel.textProperty().bind(I18N.createStringBinding("label.menu.title"));
        valueTypeLabel.textProperty().bind(I18N.createStringBinding("label.value_type"));
        hashValueTypeLabel.textProperty().bind(I18N.createStringBinding("label.value_type.value"));
        fontScaleLabel.textProperty().bind(I18N.createStringBinding("label.ui_font_scale"));
        structureWorkspaceBtn.setText("STRUCTURE");
        algorithmWorkspaceBtn.setText("ALGORITHM");
        structureWorkspaceTitleLabel.setText("STRUCTURE");
        algorithmWorkspaceTitleLabel.setText("ALGORITHM");
        structurePreviewTitleLabel.textProperty().bind(
                I18N.createStringBinding("label.workspace.structure.preview"));
        structurePreviewHintLabel.textProperty().bind(
                I18N.createStringBinding("label.workspace.structure.preview.hint"));
        snapshotTitleLabel.setText("SNAPSHOTS");
        structureHistoryTitleLabel.setText("STRUCTURE HISTORY");
        saveSnapshotBtn.setText("SAVE SNAPSHOT");
        algorithmViewTitleLabel.setText(I18N.text("label.workspace.algorithm.preview"));
        viewportHintLabel.textProperty().bind(
                I18N.createStringBinding("label.workspace.algorithm.preview.hint"));
        statsTitleLabel.setText("RUN SUMMARY");
        logTitleLabel.setText("LOG");
        liveLabel.setText("LIVE");
        startBtn.setText("RUN ALGORITHM");
        resetBtn.setText("✕");
        replayBtn.setText("REPLAY");
        exportBtn.textProperty().bind(I18N.createStringBinding("action.execution.export"));
        compareBtn.textProperty().bind(I18N.createStringBinding("action.execution.compare"));
        delayLabel.textProperty().bind(I18N.createStringBinding("label.execution.delay"));
        timelineLabel.setText("EXECUTION TIMELINE");
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

    private void setupFontScaleSelector() {
        if (fontScaleSelector == null || rootPane == null) {
            return;
        }

        List<String> options = new ArrayList<>();
        for (int scale : UI_FONT_SCALES) {
            options.add(scale + "%");
        }
        fontScaleSelector.getItems().setAll(options);

        int savedScale = UI_PREFERENCES.getInt(UI_FONT_SCALE_PREFERENCE, DEFAULT_UI_FONT_SCALE);
        if (!UI_FONT_SCALES.contains(savedScale)) {
            savedScale = DEFAULT_UI_FONT_SCALE;
        }
        fontScaleSelector.getSelectionModel().select(savedScale + "%");
        applyUiFontScale(savedScale, false);

        fontScaleSelector.valueProperty().addListener((observable, oldValue, newValue) -> {
            int scale = parseUiFontScale(newValue);
            applyUiFontScale(scale, true);
        });
        rootPane.addEventFilter(KeyEvent.KEY_PRESSED, this::handleUiFontScaleShortcut);
    }

    private int parseUiFontScale(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_UI_FONT_SCALE;
        }
        try {
            return Integer.parseInt(value.replace("%", "").trim());
        } catch (NumberFormatException ignored) {
            return DEFAULT_UI_FONT_SCALE;
        }
    }

    private void applyUiFontScale(int scale, boolean persist) {
        if (!UI_FONT_SCALES.contains(scale)) {
            scale = DEFAULT_UI_FONT_SCALE;
        }
        uiFontScale = scale;
        double fontSize = BASE_UI_FONT_SIZE * scale / 100.0d;
        rootPane.setStyle(String.format(Locale.ROOT, "-fx-font-size: %.2fpx;", fontSize));
        updateResponsiveLayout(rootPane.getWidth());
        rootPane.requestLayout();
        if (persist) {
            UI_PREFERENCES.putInt(UI_FONT_SCALE_PREFERENCE, scale);
        }
    }

    private void handleUiFontScaleShortcut(KeyEvent event) {
        if (!event.isControlDown()) {
            return;
        }
        KeyCode code = event.getCode();
        if (code == KeyCode.EQUALS || code == KeyCode.ADD) {
            changeUiFontScale(1);
            event.consume();
            return;
        }
        if (code == KeyCode.MINUS || code == KeyCode.SUBTRACT) {
            changeUiFontScale(-1);
            event.consume();
            return;
        }
        if (code == KeyCode.DIGIT0 || code == KeyCode.NUMPAD0) {
            selectUiFontScale(DEFAULT_UI_FONT_SCALE);
            event.consume();
        }
    }

    private void changeUiFontScale(int direction) {
        int currentScale = parseUiFontScale(fontScaleSelector.getValue());
        int index = UI_FONT_SCALES.indexOf(currentScale);
        if (index < 0) {
            index = UI_FONT_SCALES.indexOf(DEFAULT_UI_FONT_SCALE);
        }
        int nextIndex = Math.max(0, Math.min(UI_FONT_SCALES.size() - 1, index + direction));
        selectUiFontScale(UI_FONT_SCALES.get(nextIndex));
    }

    private void selectUiFontScale(int scale) {
        fontScaleSelector.getSelectionModel().select(scale + "%");
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
        if (selector == null) {
            return;
        }
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
                if (item.available()) {
                    setText(item.type());
                    setDisable(false);
                } else {
                    setText(item.type() + " · " + I18N.text("label.value_type.unavailable"));
                    setDisable(true);
                }
            }
        };
    }

    private void refreshValueTypeSelectors() {
        if (activeDefinition == null || valueTypeBox == null) {
            return;
        }
        String moduleId = activeDefinition.id();
        boolean maze = "maze".equals(moduleId);
        setControlVisibility(valueTypeBox, false);
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
                return;
            }

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
        } finally {
            updatingValueTypeSelectors = false;
        }
    }

    private void refreshHashTableTypeSelectors() {
        List<String> signatures = MODULE_REGISTRY.structureTypeSignatures("hash-table");
        List<String> keyTypes = new ArrayList<>();
        for (String signature : signatures) {
            int separator = signature.indexOf('.');
            if (separator <= 0) {
                continue;
            }
            String keyType = signature.substring(0, separator);
            if (!keyTypes.contains(keyType)) {
                keyTypes.add(keyType);
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
        if (activeDefinition == null) {
            return;
        }
        updateAlgorithmWorkspaceAvailability(activeDefinition.id());
        clearAlgorithmSelection();
        selectFirstAlgorithmButton(activeDefinition.id());
        List<AlgorithmNavigationItem> items = algorithmNavigationItems(activeDefinition.id());
        if (!items.isEmpty() && currentSubController instanceof AlgorithmSelectionSupport support) {
            support.selectAlgorithm(items.getFirst().id());
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
                Button unavailable = createFamilyRailButton(definition);
                unavailable.setDisable(true);
                algorithmNavigationBox.getChildren().add(unavailable);
                continue;
            }
            AlgorithmNavigationItem first = navigationItems.getFirst();
            Button familyButton = createAlgorithmFamilyButton(definition, first);
            algorithmNavigationBox.getChildren().add(familyButton);
            Map<String, Button> byAlgorithm = algorithmButtons.computeIfAbsent(definition.id(), ignored -> new LinkedHashMap<>());
            for (AlgorithmNavigationItem item : navigationItems) {
                byAlgorithm.put(item.id(), familyButton);
            }
        }
    }

    private Button createAlgorithmFamilyButton(WorkbenchModuleDefinition definition, AlgorithmNavigationItem first) {
        Button button = createFamilyRailButton(definition);
        button.setOnAction(event -> {
            if (activeDefinition == null || !activeDefinition.id().equals(definition.id())) {
                switchToModule(definition);
            }
            setWorkspaceMode(false);
            if (currentSubController instanceof AlgorithmSelectionSupport support) {
                support.selectAlgorithm(first.id());
                selectedAlgorithmId = first.id();
                selectAlgorithmButton(definition.id(), first.id());
                refreshTopContext();
            }
        });
        return button;
    }

    private Button createCatalogButton(WorkbenchModuleDefinition definition) {
        Button button = createFamilyRailButton(definition);
        button.setOnAction(event -> switchToModule(definition));
        structureButtons.computeIfAbsent(definition.id(), ignored -> new java.util.ArrayList<>()).add(button);
        return button;
    }

    private Button createFamilyRailButton(WorkbenchModuleDefinition definition) {
        Button button = new Button();
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("family-rail-button");
        button.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(
                () -> familyRailText(definition), I18N.localeProperty()));
        return button;
    }

    private String familyRailText(WorkbenchModuleDefinition definition) {
        return familyIndex(definition.id()) + "   " + familyGlyph(definition.id()) + "  " + familyName(definition.id());
    }

    private String familyName(String moduleId) {
        return switch (moduleId) {
            case "array" -> "ARRAY";
            case "linked-list" -> "LINKED";
            case "stack" -> "STACK";
            case "queue" -> "QUEUE";
            case "tree" -> "TREE";
            case "graph" -> "GRAPH";
            case "string" -> "STRING";
            case "maze" -> "MAZE";
            default -> moduleId.toUpperCase(Locale.ROOT);
        };
    }

    private String familyIndex(String moduleId) {
        return switch (moduleId) {
            case "array" -> "01";
            case "linked-list" -> "02";
            case "stack" -> "03";
            case "queue" -> "04";
            case "tree" -> "05";
            case "graph" -> "06";
            case "string" -> "07";
            case "maze" -> "08";
            default -> "--";
        };
    }

    private String familyGlyph(String moduleId) {
        return switch (moduleId) {
            case "array" -> "▦";
            case "linked-list" -> "⌁";
            case "stack" -> "▤";
            case "queue" -> "▥";
            case "tree" -> "⌘";
            case "graph" -> "◇";
            case "string" -> "Aa";
            case "maze" -> "▧";
            default -> "·";
        };
    }

    private Button createAlgorithmButton(
            WorkbenchModuleDefinition definition,
            AlgorithmNavigationItem item) {
        Button button = new Button();
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("sidebar-algorithm-button");
        button.getStyleClass().add(moduleAccentStyleClass(definition.id()));
        button.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(
                () -> AlgorithmLabels.text(item.id()), I18N.localeProperty()));
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
            selectedAlgorithmId = algorithmId;
            selectAlgorithmButton(definition.id(), algorithmId);
            refreshTopContext();
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
        return algorithmIds.stream().distinct().map(AlgorithmNavigationItem::new).toList();
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

    private record ValueTypeOption(String type, boolean available) {
    }

    private record AlgorithmNavigationItem(String id) {
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
        refreshTopContext();
        refreshExecutionPresentation();
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
                stepForwardBtn, exportBtn, compareBtn, saveSnapshotBtn,
                speed1Btn, speed2Btn, speed4Btn, speed8Btn, speed16Btn);
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
        double sidePanelWidth = nextNarrowLayout ? 300.0d : nextCompactLayout ? 330.0d : 360.0d;
        snapshotPanel.setPrefWidth(sidePanelWidth);
        diagnosticsPanel.setPrefWidth(sidePanelWidth);
        double controlWidth = nextNarrowLayout ? 270.0d : nextCompactLayout ? 300.0d : 320.0d;
        structureControlRail.setPrefWidth(controlWidth);
        algorithmControlRail.setPrefWidth(controlWidth);
        setControlVisibility(langBtn, false);
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
        double width = compact ? 182.0d : 196.0d;
        if (uiFontScale >= 125) {
            width += 8.0d;
        }
        if (uiFontScale >= 140) {
            width += 8.0d;
        }
        rail.setPrefWidth(Math.min(220.0d, width));
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
        selectedAlgorithmId = null;
        clearStructureSelection();
        refreshValueTypeSelectors();
        rebuildAlgorithmMenu();
        loadSubController(definition.controllerFactory().get());
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
        Map.Entry<String, Button> first = buttons.entrySet().iterator().next();
        first.getValue().pseudoClassStateChanged(SELECTED, true);
        selectedAlgorithmId = first.getKey();
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
        currentSubController.pausedProperty().addListener((observable, oldValue, newValue) -> {
            refreshPauseText();
            refreshTopContext();
            refreshExecutionPresentation();
        });
        currentSubController.runningProperty().addListener((observable, oldValue, newValue) -> {
            updateSnapshotActionState();
            updateWorkspaceInteractionState();
            refreshTopContext();
            refreshExecutionPresentation();
        });
        currentSubController.presentationEventProperty().addListener((observable, oldValue, newValue) -> {
            refreshExecutionPresentation();
            refreshTopContext();
        });
        currentSubController.setupCustomControls(customControlBox);
        distributeModuleControls();
        wireStructureSelection();
        structureRevisionListener = (observable, oldValue, newValue) -> {
            refreshSnapshotCards();
            refreshStructureSummary();
        };
        currentSubController.structureRevisionProperty().addListener(structureRevisionListener);

        BaseVisualizer<?> visualizer = newController.getVisualizer();
        if (visualizer != null) {
            attachVisualizer(isStructurePageVisible());
        }
        currentSubController.dispatchVisualizerAttached();
        refreshPauseText();
        refreshTopContext();
        refreshExecutionPresentation();
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
        algorithmViewTitleLabel.setText("CURRENT STEP");
        refreshSnapshotCards();
        refreshAlgorithmInputSource();
        refreshTopContext();
        refreshStructureSummary();
        refreshExecutionPresentation();
    }

    private void refreshSnapshotCards() {
        if (activeDefinition == null || snapshotCards == null) {
            return;
        }
        String moduleName = I18N.text(activeDefinition.labelKey());
        StructureSnapshotSupport<?> support = currentSnapshotSupport();
        if (support == null) {
            snapshotCards.getChildren().clear();
            if (inspectorSnapshotCards != null) inspectorSnapshotCards.getChildren().clear();
            snapshotCountLabel.setText(I18N.text(
                    "label.workspace.snapshot.count", 0,
                    structureSnapshotStore.maxSnapshotsPerModule()));
            updateSnapshotActionState();
            refreshStructureHistory();
            return;
        }

        snapshotCards.getChildren().clear();
        if (inspectorSnapshotCards != null) inspectorSnapshotCards.getChildren().clear();
        StructureSnapshot<?> current = support.captureStructureSnapshot();
        snapshotCards.getChildren().add(createSnapshotCard(
                moduleName, I18N.text("label.workspace.snapshot.current"), current, support, true));

        List<StructureSnapshot<?>> saved = structureSnapshotStore.snapshots(activeDefinition.id());
        for (StructureSnapshot<?> snapshot : saved) {
            snapshotCards.getChildren().add(createSnapshotCard(
                    moduleName, I18N.text("label.workspace.snapshot.saved"), snapshot,
                    support, false));
            if (inspectorSnapshotCards != null && inspectorSnapshotCards.getChildren().size() < 2) {
                inspectorSnapshotCards.getChildren().add(createInspectorSnapshotCard(
                        snapshot, support, inspectorSnapshotCards.getChildren().size() == 0));
            }
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

    private Node createInspectorSnapshotCard(
            StructureSnapshot<?> snapshot,
            StructureSnapshotSupport<?> support,
            boolean newest) {
        VBox card = new VBox(5);
        card.setMaxWidth(Double.MAX_VALUE);
        card.getStyleClass().add("snapshot-card");
        if (newest) card.getStyleClass().add("snapshot-card-current");
        HBox header = new HBox(8);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label title = new Label("SNAPSHOT / " + shortSnapshotId(snapshot));
        title.getStyleClass().add("snapshot-card-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label time = new Label(formatSnapshotTime(snapshot));
        time.getStyleClass().add("snapshot-card-detail");
        header.getChildren().addAll(title, spacer, time);
        Label detail = new Label(describeSnapshot(support, snapshot));
        detail.setWrapText(true);
        detail.getStyleClass().add("snapshot-card-detail");
        card.getChildren().addAll(header, detail);
        return card;
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
            WorkbenchTheme.applyControl(restore);
            restore.setOnAction(event -> restoreSnapshot(snapshot));
            actions.getChildren().add(restore);
        }
        if (algorithmInputSupport != null) {
            Button useInput = new Button(I18N.text(current
                    ? "action.workspace.use_current_input" : "action.workspace.use_snapshot_input"));
            useInput.getStyleClass().add("snapshot-card-action");
            WorkbenchTheme.applyControl(useInput);
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

    @FXML
    private void useCurrentStructureInput() {
        SnapshotAlgorithmInputSupport<?> support = currentAlgorithmInputSupport();
        if (support == null || currentSubController == null || currentSubController.isRunning()) {
            return;
        }
        support.useCurrentStructureAsAlgorithmInput();
        refreshAlgorithmInputSource();
        refreshSnapshotCards();
        appendSystemLog("Algorithm input: current structure snapshot");
    }

    @FXML
    private void useLatestSnapshotInput() {
        SnapshotAlgorithmInputSupport<?> support = currentAlgorithmInputSupport();
        if (support == null || activeDefinition == null || currentSubController == null || currentSubController.isRunning()) {
            return;
        }
        List<StructureSnapshot<?>> snapshots = structureSnapshotStore.snapshots(activeDefinition.id());
        if (snapshots.isEmpty()) {
            refreshAlgorithmInputSource();
            return;
        }
        StructureSnapshot<?> snapshot = snapshots.getFirst();
        useSnapshotAsAlgorithmInputUnchecked(support, snapshot);
        refreshAlgorithmInputSource();
        refreshSnapshotCards();
        appendSystemLog("Algorithm input: saved snapshot " + shortSnapshotId(snapshot));
    }

    private void refreshAlgorithmInputSource() {
        if (algorithmInputSourceLabel == null) {
            return;
        }
        SnapshotAlgorithmInputSupport<?> support = currentAlgorithmInputSupport();
        boolean hasSaved = activeDefinition != null && !structureSnapshotStore.snapshots(activeDefinition.id()).isEmpty();
        if (savedInputBtn != null) savedInputBtn.setDisable(!hasSaved || support == null);
        if (currentInputBtn != null) currentInputBtn.setDisable(support == null);
        if (activeDefinition != null && "maze".equals(activeDefinition.id())) {
            algorithmInputSourceLabel.setText(I18N.text("label.workspace.algorithm.input.maze"));
            if (currentInputBtn != null) { currentInputBtn.pseudoClassStateChanged(SELECTED, true); currentInputBtn.setText("● CURRENT STRUCTURE"); }
            if (savedInputBtn != null) { savedInputBtn.pseudoClassStateChanged(SELECTED, false); savedInputBtn.setText("○ SAVED SNAPSHOT"); }
            return;
        }
        if (support == null) {
            algorithmInputSourceLabel.setText(I18N.text("label.workspace.algorithm.input.parameters"));
            if (currentInputBtn != null) { currentInputBtn.pseudoClassStateChanged(SELECTED, false); currentInputBtn.setText("○ CURRENT STRUCTURE"); }
            if (savedInputBtn != null) { savedInputBtn.pseudoClassStateChanged(SELECTED, false); savedInputBtn.setText("○ SAVED SNAPSHOT"); }
            return;
        }
        String snapshotId = support.algorithmInputSnapshotId();
        boolean current = snapshotId == null;
        if (currentInputBtn != null) {
            currentInputBtn.pseudoClassStateChanged(SELECTED, current);
            currentInputBtn.setText((current ? "● " : "○ ") + "CURRENT STRUCTURE");
        }
        if (savedInputBtn != null) {
            savedInputBtn.pseudoClassStateChanged(SELECTED, !current);
            savedInputBtn.setText((current ? "○ " : "● ") + "SAVED SNAPSHOT");
        }
        if (current) {
            algorithmInputSourceLabel.setText("CURRENT STRUCTURE SNAPSHOT");
            return;
        }
        StructureSnapshot<?> selected = structureSnapshotStore.snapshots(activeDefinition.id()).stream()
                .filter(snapshot -> snapshot.id().equals(snapshotId)).findFirst().orElse(null);
        String detail = selected == null ? "SAVED SNAPSHOT / " + shortSnapshotId(snapshotId)
                : "SAVED SNAPSHOT / " + shortSnapshotId(snapshotId) + "\n" + formatSnapshotTime(selected);
        algorithmInputSourceLabel.setText(detail);
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
        currentSubController.recordAuxiliaryEvent(
                "snapshot-created", new SnapshotLifecycleEvent.Created(snapshot.id(), snapshot.moduleId()));
        refreshSnapshotCards();
        refreshAlgorithmInputSource();
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
            currentSubController.recordAuxiliaryEvent(
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
        if (logView != null) {
            logView.appendSystem(message);
        }
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


    private void setupPlaybackSpeedButtons() {
        bindSpeedButton(speed1Btn, 1.0d, 50.0d);
        bindSpeedButton(speed2Btn, 2.0d, 25.0d);
        bindSpeedButton(speed4Btn, 4.0d, 12.5d);
        bindSpeedButton(speed8Btn, 8.0d, 6.0d);
        bindSpeedButton(speed16Btn, 16.0d, 0.0d);
        setSelectedSpeed(speed1Btn);
    }

    private void bindSpeedButton(Button button, double speed, double delayMillis) {
        if (button == null) {
            return;
        }
        button.setOnAction(event -> {
            if (delaySlider != null) {
                delaySlider.setValue(delayMillis);
            }
            if (currentSubController != null && currentSubController.getVisualizer() != null) {
                currentSubController.getVisualizer().setPlaybackSpeed(speed);
                currentSubController.getVisualizer().setScrubbing(speed >= 16.0d);
            }
            setSelectedSpeed(button);
        });
    }

    private void setSelectedSpeed(Button selected) {
        for (Button button : List.of(speed1Btn, speed2Btn, speed4Btn, speed8Btn, speed16Btn)) {
            if (button != null) {
                button.pseudoClassStateChanged(SELECTED, button == selected);
            }
        }
    }

    private void refreshTopContext() {
        if (topContextLabel == null) {
            return;
        }
        String family = activeDefinition == null ? "WORKBENCH" : I18N.text(activeDefinition.labelKey()).toUpperCase(Locale.ROOT);
        boolean structureMode = isStructurePageVisible();
        String suffix = structureMode ? "STRUCTURE EDITOR" : algorithmContextName();
        topContextLabel.setText(family + " / " + suffix);
        if (activeDefinition != null) {
            String railFamily = familyName(activeDefinition.id());
            String familyMeta = railFamily + " / " + familyIndex(activeDefinition.id());
            if (structureWorkspaceSubtitleLabel != null) structureWorkspaceSubtitleLabel.setText(railFamily);
            if (structureControlsTitleLabel != null) structureControlsTitleLabel.setText(familyMeta);
            if (algorithmWorkspaceSubtitleLabel != null) algorithmWorkspaceSubtitleLabel.setText(algorithmContextName());
            if (algorithmControlsTitleLabel != null) algorithmControlsTitleLabel.setText(familyMeta);
        }
        if (runStateLabel != null) {
            String state = "EDITING";
            if (!structureMode) {
                if (currentSubController != null && currentSubController.isRunning()) {
                    state = currentSubController.isPaused() ? "PAUSED" : "RUNNING";
                } else if (currentSubController != null && currentSubController.hasExecutionRecord()) {
                    state = currentSubController.latestExecutionStatus();
                } else {
                    state = "IDLE";
                }
            }
            runStateLabel.setText(state);
            runStateLabel.getStyleClass().removeAll("state-running", "state-paused", "state-completed", "state-failed");
            if ("RUNNING".equals(state)) runStateLabel.getStyleClass().add("state-running");
            if ("PAUSED".equals(state)) runStateLabel.getStyleClass().add("state-paused");
            if ("COMPLETED".equals(state)) runStateLabel.getStyleClass().add("state-completed");
            if ("FAILED".equals(state) || "CANCELLED".equals(state)) runStateLabel.getStyleClass().add("state-failed");
        }
        if (runIdLabel != null) {
            String id = currentSubController == null ? null : currentSubController.latestRunId();
            runIdLabel.setText(id == null ? "" : "RUN #" + shortRunId(id));
        }
    }

    private String algorithmContextName() {
        if (selectedAlgorithmId == null || selectedAlgorithmId.isBlank()) {
            return "ALGORITHM";
        }
        return AlgorithmLabels.text(selectedAlgorithmId).toUpperCase(Locale.ROOT);
    }

    private String shortRunId(String runId) {
        if (runId == null || runId.isBlank()) return "----";
        String compact = runId.replace("-", "");
        return compact.substring(0, Math.min(4, compact.length())).toUpperCase(Locale.ROOT);
    }

    private void refreshStructureSummary() {
        if (currentSubController == null) {
            return;
        }
        String summary = currentSubController.structureSummaryText();
        if (structureOverviewLabel != null) {
            structureOverviewLabel.setText(summary == null || summary.isBlank() ? "Ready" : summary);
        }
        if (structureNodeCountLabel != null) {
            structureNodeCountLabel.setText(currentSubController.structurePrimaryCount());
        }
        if (structureHeightLabel != null) {
            structureHeightLabel.setText(currentSubController.structureSecondaryCount());
        }
        if (structureStateLabel != null) {
            structureStateLabel.setText("READY");
        }
        if (overviewPrimaryValue != null) overviewPrimaryValue.setText(currentSubController.structurePrimaryCount());
        if (overviewSecondaryValue != null) overviewSecondaryValue.setText(currentSubController.structureSecondaryCount());
        if (overviewEventsValue != null) overviewEventsValue.setText(Integer.toString(currentSubController.structureEvents().size()));
        if (overviewStateValue != null) overviewStateValue.setText("READY");
    }

    private void wireStructureSelection() {
        if (currentSubController instanceof TreeController treeController) {
            treeController.setSelectionListener(this::showTreeSelection);
        }
    }

    private void showTreeSelection(TreeController.NodeSelection selection) {
        if (selection == null) {
            clearStructureSelection();
            return;
        }
        if (structureSelectionOverlay != null) {
            structureSelectionOverlay.setManaged(true);
            structureSelectionOverlay.setVisible(true);
        }
        if (selectedNodeIdLabel != null) selectedNodeIdLabel.setText("#" + selection.id());
        if (selectedNodeValueLabel != null) selectedNodeValueLabel.setText(Integer.toString(selection.value()));
        if (structureInspectorBody != null) {
            structureInspectorBody.setText(String.format(Locale.ROOT,
                    "Node ID        #%d%nValue          %d%nParent         %s%nChildren       %d%nDepth          %d",
                    selection.id(), selection.value(), selection.parentId() == null ? "none" : "#" + selection.parentId(),
                    selection.childCount(), selection.depth()));
        }
    }

    private void clearStructureSelection() {
        if (structureSelectionOverlay != null) {
            structureSelectionOverlay.setManaged(false);
            structureSelectionOverlay.setVisible(false);
        }
        if (structureInspectorBody != null) {
            structureInspectorBody.setText("Select an element in the canvas.");
        }
    }

    private void refreshExecutionPresentation() {
        if (currentSubController == null) {
            return;
        }
        EventEnvelope current = currentSubController.currentPresentationEvent();
        if (current == null) {
            if (currentStepOverlay != null) {
                currentStepOverlay.setManaged(false);
                currentStepOverlay.setVisible(false);
            }
            if (eventKindLabel != null) eventKindLabel.setText("NO EVENT");
            if (eventDetailsLabel != null) eventDetailsLabel.setText("Run an algorithm or select a timeline event.");
            if (eventKindDot != null) setEventDotClass("event-dot-idle");
            if (timelineCursorLabel != null) { timelineCursorLabel.setManaged(false); timelineCursorLabel.setVisible(false); }
        } else {
            if (currentStepOverlay != null) {
                currentStepOverlay.setManaged(true);
                currentStepOverlay.setVisible(true);
            }
            String kind = eventDisplayName(current);
            if (currentStepSequenceLabel != null) currentStepSequenceLabel.setText(String.format(Locale.ROOT, "#%04d", current.sequence()));
            if (currentStepKindLabel != null) currentStepKindLabel.setText(kind);
            if (currentStepDetailLabel != null) currentStepDetailLabel.setText(describeCurrentStep(current));
            if (eventKindLabel != null) eventKindLabel.setText(kind);
            if (eventDetailsLabel != null) eventDetailsLabel.setText(describeEventEnvelope(current));
            if (eventKindDot != null) setEventDotClass(eventDotClass(current));
            updateTimelineCursorCallout(current);
        }
        String result = currentSubController.latestResultText();
        if (resultLabel != null) resultLabel.setText(result);
        if (resultPreviewLabel != null) resultPreviewLabel.setText(result);
        if (timelinePositionLabel != null) {
            int index = currentSubController.presentationEventIndex();
            int count = currentSubController.executionEvents().size();
            timelinePositionLabel.setText(count == 0 ? "#0000" : String.format(Locale.ROOT, "#%04d / %04d", Math.max(0, index + 1), count));
        }
        refreshRunSummary();
        rebuildTimelineMarkers();
        refreshStructureSummary();
    }

    private String eventDisplayName(EventEnvelope envelope) {
        String simple = envelope.event().getClass().getSimpleName();
        return simple.replaceAll("([a-z0-9])([A-Z])", "$1 $2").toUpperCase(Locale.ROOT);
    }

    private String describeCurrentStep(EventEnvelope envelope) {
        Object event = envelope.event();
        if (event instanceof ObservationEvent.Visited visited) {
            return "TARGET  " + formatReference(visited.ref());
        }
        if (event instanceof ObservationEvent.Examined examined) {
            return "FROM    " + formatReference(examined.fromRef()) + "\nTO      " + formatReference(examined.toRef());
        }
        if (event instanceof ObservationEvent.Compared compared) {
            return "LEFT    " + formatReference(compared.leftRef()) + "\nRIGHT   " + formatReference(compared.rightRef());
        }
        if (event instanceof ObservationEvent.Matched matched) {
            return "INDEX   " + matched.index() + "\nLENGTH  " + matched.length();
        }
        if (event instanceof ObservationEvent.Fallback fallback) {
            return "PATTERN " + fallback.fromIndex() + " → " + fallback.toIndex();
        }
        if (event instanceof ObservationEvent.Backtracked backtracked) {
            return "TARGET  " + formatReference(backtracked.ref());
        }
        if (event instanceof TreeStructureEvent.NodeInserted inserted) {
            return "NODE    #" + inserted.nodeId() + "\nVALUE   " + inserted.value();
        }
        if (event instanceof TreeStructureEvent.NodeRemoved removed) {
            return "NODE    #" + removed.nodeId() + "\nVALUE   " + removed.value();
        }
        if (event instanceof TreeStructureEvent.ValueChanged changed) {
            return "NODE    #" + changed.nodeId() + "\nVALUE   " + changed.previousValue() + " → " + changed.value();
        }
        if (event instanceof TreeStructureEvent.LeftChanged changed) {
            return "NODE    #" + changed.nodeId() + "\nLEFT    " + formatIdChange(changed.previousChildId(), changed.childId());
        }
        if (event instanceof TreeStructureEvent.RightChanged changed) {
            return "NODE    #" + changed.nodeId() + "\nRIGHT   " + formatIdChange(changed.previousChildId(), changed.childId());
        }
        if (event instanceof TreeStructureEvent.RootChanged changed) {
            return "ROOT    " + formatIdChange(changed.previousRootId(), changed.rootId());
        }
        if (event instanceof TreeStructureEvent.ChildInserted inserted) {
            return "PARENT  #" + inserted.parentId() + "\nCHILD   #" + inserted.childId() + "  @" + inserted.index();
        }
        if (event instanceof TreeStructureEvent.ChildRemoved removed) {
            return "PARENT  #" + removed.parentId() + "\nCHILD   #" + removed.childId() + "  @" + removed.index();
        }
        String text = envelope.event().toString();
        if (text.length() > 120) text = text.substring(0, 117) + "...";
        return text;
    }

    private String describeEventEnvelope(EventEnvelope envelope) {
        return String.format(Locale.ROOT,
                "Sequence     %d%nTime         %s%nKind         %s%nSource       %s%n%n%s",
                envelope.sequence(), EVENT_TIME_FORMATTER.format(envelope.timestamp().atZone(ZoneId.systemDefault())),
                eventCategory(envelope), envelope.source(), describeCurrentStep(envelope));
    }

    private String eventCategory(EventEnvelope envelope) {
        Object event = envelope.event();
        if (event instanceof com.majortom.algorithms.core.event.structure.StructureEvent) return "Structure Event";
        if (event instanceof ObservationEvent) return "Observation Event";
        if (event instanceof ExecutionLifecycleEvent) return "Runtime Event";
        return "Execution Event";
    }

    private String eventDotClass(EventEnvelope envelope) {
        if (envelope.event() instanceof ObservationEvent) return "event-dot-observation";
        if (envelope.event() instanceof com.majortom.algorithms.core.event.structure.StructureEvent) return "event-dot-structure";
        if (envelope.event() instanceof ExecutionLifecycleEvent) return "event-dot-runtime";
        return "event-dot-idle";
    }

    private void setEventDotClass(String styleClass) {
        eventKindDot.getStyleClass().removeAll("event-dot-idle", "event-dot-runtime", "event-dot-structure", "event-dot-observation");
        eventKindDot.getStyleClass().add(styleClass);
    }

    private String formatReference(ObservationEvent.Reference reference) {
        if (reference instanceof ObservationEvent.EntityRef entity) return entity.domain().toUpperCase(Locale.ROOT) + " #" + entity.id();
        if (reference instanceof ObservationEvent.IndexRef index) return index.source() + "[" + index.index() + "]";
        if (reference instanceof ObservationEvent.CoordinateRef cell) return "(" + cell.row() + ", " + cell.column() + ")";
        if (reference instanceof ObservationEvent.ValueRef value) return String.valueOf(value.value());
        return String.valueOf(reference);
    }

    private String formatIdChange(Long previous, Long next) {
        String left = previous == null ? "none" : "#" + previous;
        String right = next == null ? "none" : "#" + next;
        return left + " → " + right;
    }

    private void refreshRunSummary() {
        if (runMetric1Title == null || currentSubController == null) return;
        ExecutionStatistics statistics = currentSubController.currentExecutionStatistics();
        List<MetricDisplay> metrics = metricDisplays(statistics);
        setMetric(runMetric1Title, runMetric1Value, metrics.get(0));
        setMetric(runMetric2Title, runMetric2Value, metrics.get(1));
        setMetric(runMetric3Title, runMetric3Value, metrics.get(2));
        setMetric(runMetric4Title, runMetric4Value, metrics.get(3));
    }

    private List<MetricDisplay> metricDisplays(ExecutionStatistics statistics) {
        Map<String, Long> values = statistics.metrics();
        List<MetricDisplay> metrics = new ArrayList<>();
        addMetricIfPresent(metrics, values, "nodesVisited", "NODES VISITED");
        addMetricIfPresent(metrics, values, "edgesExamined", "EDGES EXAMINED");
        addMetricIfPresent(metrics, values, "comparisons", "COMPARISONS");
        addMetricIfPresent(metrics, values, "writes", "WRITES");
        addMetricIfPresent(metrics, values, "swaps", "SWAPS");
        addMetricIfPresent(metrics, values, "matches", "MATCHES");
        addMetricIfPresent(metrics, values, "fallbacks", "FALLBACKS");
        addMetricIfPresent(metrics, values, "backtracks", "BACKTRACKS");
        if (metrics.size() < 3) metrics.add(new MetricDisplay("DOMAIN EVENTS", Long.toString(statistics.domainEventCount())));
        if (metrics.size() < 3) metrics.add(new MetricDisplay("TOTAL EVENTS", Long.toString(statistics.totalEventCount())));
        while (metrics.size() < 3) metrics.add(new MetricDisplay("EVENTS", "0"));
        List<MetricDisplay> result = new ArrayList<>(metrics.subList(0, 3));
        result.add(new MetricDisplay("DURATION", formatDuration(statistics.duration())));
        return result;
    }

    private void addMetricIfPresent(List<MetricDisplay> metrics, Map<String, Long> values, String key, String title) {
        long value = values.getOrDefault(key, 0L);
        if (value > 0L) metrics.add(new MetricDisplay(title, Long.toString(value)));
    }

    private void setMetric(Label title, Label value, MetricDisplay metric) {
        title.setText(metric.title());
        value.setText(metric.value());
    }

    private String formatDuration(java.time.Duration duration) {
        long seconds = Math.max(0L, duration.toSeconds());
        return String.format(Locale.ROOT, "%02d:%02d:%02d", seconds / 3600L, (seconds % 3600L) / 60L, seconds % 60L);
    }

    private void updateTimelineCursorCallout(EventEnvelope current) {
        if (timelineCursorLabel == null || current == null) return;
        timelineCursorLabel.setText(String.format(Locale.ROOT, "#%04d   %s", current.sequence(), eventDisplayName(current)));
        timelineCursorLabel.setManaged(true);
        timelineCursorLabel.setVisible(true);
        int count = currentSubController.executionEvents().size();
        int index = currentSubController.presentationEventIndex();
        if (timelineSlider != null && count > 1 && timelineSlider.getWidth() > 0.0d) {
            double ratio = Math.max(0.0d, Math.min(1.0d, index / (double) (count - 1)));
            double usable = Math.max(0.0d, timelineSlider.getWidth() - 190.0d);
            timelineCursorLabel.setTranslateX((ratio - 0.5d) * usable);
        }
    }

    private record MetricDisplay(String title, String value) {}

    private void rebuildTimelineMarkers() {
        if (timelineMarkers == null || currentSubController == null) return;
        timelineMarkers.getChildren().clear();
        List<EventEnvelope> events = currentSubController.executionEvents();
        if (events.isEmpty()) return;
        int max = 15;
        int step = Math.max(1, (int) Math.ceil(events.size() / (double) max));
        int currentIndex = currentSubController.presentationEventIndex();
        for (int index = 0; index < events.size(); index += step) {
            EventEnvelope envelope = events.get(index);
            Label marker = new Label(eventMarker(envelope));
            marker.getStyleClass().add("timeline-marker");
            marker.getStyleClass().add(eventMarkerClass(envelope));
            if (index == currentIndex) marker.getStyleClass().add("timeline-marker-current");
            timelineMarkers.getChildren().add(marker);
        }
    }

    private String eventMarker(EventEnvelope envelope) {
        if (envelope.event() instanceof ExecutionLifecycleEvent) return "◆";
        if (envelope.event() instanceof com.majortom.algorithms.core.event.structure.StructureEvent) return "■";
        if (envelope.event() instanceof com.majortom.algorithms.core.event.observation.ObservationEvent) return "●";
        return "·";
    }

    private String eventMarkerClass(EventEnvelope envelope) {
        if (envelope.event() instanceof ExecutionLifecycleEvent) return "timeline-runtime";
        if (envelope.event() instanceof com.majortom.algorithms.core.event.structure.StructureEvent) return "timeline-structure";
        if (envelope.event() instanceof com.majortom.algorithms.core.event.observation.ObservationEvent) return "timeline-observation";
        return "timeline-other";
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
        pauseBtn.setText(I18N.text(key).toUpperCase(Locale.ROOT));
    }
}
