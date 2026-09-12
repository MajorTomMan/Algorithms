package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.algorithm.discovery.ComponentDiscovery;
import com.majortom.algorithms.core.registry.ComponentRegistry;
import com.majortom.algorithms.practice.runtime.PracticeProblemRegistry;
import com.majortom.algorithms.practice.runtime.model.ProblemDescriptor;
import com.majortom.algorithms.utils.EffectUtils;
import com.majortom.algorithms.visualization.BaseController;
import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.WorkbenchControls;
import com.majortom.algorithms.visualization.algorithm.AlgorithmCatalog;
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
import com.majortom.algorithms.visualization.structure.RuntimeValueTypeSupport;
import com.majortom.algorithms.visualization.runtime.value.ValueAdapters;
import com.majortom.algorithms.visualization.settings.FontSettings;
import com.majortom.algorithms.visualization.settings.FontSettingsService;
import atlantafx.base.theme.Styles;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;
import javafx.geometry.Bounds;
import javafx.scene.input.KeyCode;

import java.net.URL;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

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
    private static final double COMPACT_LAYOUT_WIDTH = 1500.0d;
    private static final double COMPACT_LAYOUT_HEIGHT = 820.0d;
    private static final double NARROW_LAYOUT_WIDTH = 1120.0d;
    private static final double NARROW_LAYOUT_HEIGHT = 680.0d;
    private static final DateTimeFormatter SNAPSHOT_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter EVENT_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final ComponentRegistry COMPONENTS = ComponentDiscovery.discover();
    private static final List<String> OFFICIAL_VALUE_TYPES = ValueAdapters.supportedTypeNames();
    private static final FontSettingsService FONT_SETTINGS_SERVICE = new FontSettingsService();
    private static final PracticeProblemRegistry PRACTICE_PROBLEMS =
            PracticeProblemRegistry.discover("com.majortom.algorithms");

    @FXML
    private BorderPane rootPane;
    @FXML
    private VBox topShell;
    @FXML
    private HBox topBar;
    @FXML
    private HBox brandZone;
    @FXML
    private Label brandSubtitle;
    @FXML
    private HBox workspaceModeBox;
    @FXML
    private HBox topContextZone;
    @FXML
    private VBox structureNavigationBox;
    @FXML
    private VBox algorithmNavigationBox;
    @FXML
    private Button structureWorkspaceBtn;
    @FXML
    private Button algorithmWorkspaceBtn;
    @FXML
    private Button practiceWorkspaceBtn;
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
    private Button fontSettingsBtn;
    @FXML
    private StackPane workspaceLayer;
    @FXML
    private VBox structureWorkspacePane;
    @FXML
    private VBox algorithmWorkspacePane;
    @FXML
    private VBox practiceWorkspacePane;
    @FXML
    private Label practiceWorkspaceTitleLabel;
    @FXML
    private Label practiceCatalogTitleLabel;
    @FXML
    private Label practiceCatalogHintLabel;
    @FXML
    private ListView<ProblemDescriptor> practiceProblemList;
    @FXML
    private Label practiceProblemNameLabel;
    @FXML
    private Label practiceProblemMetaLabel;
    @FXML
    private Label practiceProblemEntryLabel;
    @FXML
    private Label practiceEmptyLabel;
    @FXML
    private HBox structureWorkspaceBody;
    @FXML
    private HBox algorithmWorkspaceBody;
    @FXML
    private VBox structureFamilyRail;
    @FXML
    private VBox algorithmFamilyRail;
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
    private HBox structureSnapshotPreviewBadge;
    @FXML
    private Label structureSnapshotPreviewBadgeLabel;
    @FXML
    private Label structureSnapshotPreviewIdLabel;
    @FXML
    private VBox snapshotPreviewNotice;
    @FXML
    private Label snapshotPreviewNoticeTitleLabel;
    @FXML
    private Label snapshotPreviewNoticeIdLabel;
    @FXML
    private Label snapshotPreviewNoticeHintLabel;
    @FXML
    private Button snapshotPreviewRestoreBtn;
    @FXML
    private VBox snapshotCards;
    @FXML
    private Label structureHistoryTitleLabel;
    @FXML
    private Label structureHistoryCountLabel;
    @FXML
    private HBox structureHistoryCards;
    @FXML
    private VBox structureHistoryDock;
    @FXML
    private ScrollPane structureHistoryDetails;
    @FXML
    private Button structureHistoryToggleBtn;
    @FXML
    private VBox structureSelectionOverlay;
    @FXML
    private Label selectedEntityTitleLabel;
    @FXML
    private Label selectedEntityHintLabel;
    @FXML
    private Label selectedNodeIdLabel;
    @FXML
    private Label selectedNodeValueLabel;
    @FXML
    private Label selectedValueCaptionLabel;
    @FXML
    private Label structureInspectorBody;
    @FXML
    private Label currentSelectionHeadingLabel;
    @FXML
    private Label structureOverviewHeadingLabel;
    @FXML
    private Label structureOverviewLabel;
    @FXML
    private Label overviewPrimaryTitleLabel;
    @FXML
    private Label overviewPrimaryValue;
    @FXML
    private Label overviewSecondaryTitleLabel;
    @FXML
    private Label overviewSecondaryValue;
    @FXML
    private Label overviewEventsTitleLabel;
    @FXML
    private Label overviewEventsValue;
    @FXML
    private Label overviewStateTitleLabel;
    @FXML
    private Label overviewStateValue;
    @FXML
    private Label inspectorSnapshotsHeadingLabel;
    @FXML
    private Tab structureInspectorTab;
    @FXML
    private Tab structureSnapshotsTab;
    @FXML
    private VBox inspectorSnapshotCards;
    @FXML
    private Label structurePrimaryMetricTitleLabel;
    @FXML
    private Label structureNodeCountLabel;
    @FXML
    private Label structureSecondaryMetricTitleLabel;
    @FXML
    private Label structureHeightLabel;
    @FXML
    private Label structureStateMetricTitleLabel;
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
    private VBox algorithmSelectionOverlay;
    @FXML
    private Label algorithmSelectedEntityTitleLabel;
    @FXML
    private Label algorithmSelectedEntityHintLabel;
    @FXML
    private Label algorithmSelectedNodeIdLabel;
    @FXML
    private Label algorithmSelectedNodeValueLabel;
    @FXML
    private Label algorithmSelectedValueCaptionLabel;
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
    private Tab statisticsTab;
    @FXML
    private Tab eventTab;
    @FXML
    private Tab logTab;
    @FXML
    private Tab resultTab;
    @FXML
    private Label currentEventHeadingLabel;
    @FXML
    private Label eventRunSummaryHeadingLabel;
    @FXML
    private Label resultPreviewHeadingLabel;
    @FXML
    private Label resultHeadingLabel;
    @FXML
    private Label timelinePositionLabel;
    @FXML
    private Pane timelineMarkers;
    @FXML
    private VBox timelineDetails;
    @FXML
    private Button timelineToggleBtn;
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
    private Label structureLiveLabel;
    @FXML
    private Label algorithmWorkspaceTitleLabel;
    @FXML
    private Label algorithmWorkspaceSubtitleLabel;
    @FXML
    private Label algorithmInputTitleLabel;
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
    private Label snapshotQuickTitleLabel;
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
    private Label speedLabel;
    @FXML
    private Label timelineRuntimeLegendLabel;
    @FXML
    private Label timelineStructureLegendLabel;
    @FXML
    private Label timelineObservationLegendLabel;
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

    private final List<WorkbenchModuleDefinition> moduleDefinitions = WorkbenchModules.available(COMPONENTS);
    private final InMemoryStructureSnapshotStore structureSnapshotStore =
            new InMemoryStructureSnapshotStore();
    private final Map<String, List<Button>> structureButtons = new LinkedHashMap<>();
    private final Map<String, Map<String, Button>> algorithmButtons = new LinkedHashMap<>();
    private final Map<String, String> selectedValueTypes = new LinkedHashMap<>();
    /** Snapshot-card selection is independent from restore and algorithm execution. Null means live/current. */
    private final Map<String, String> selectedSnapshotIds = new LinkedHashMap<>();
    private String selectedHashKeyType;
    private String selectedHashValueType;
    private boolean updatingValueTypeSelectors;
    private FontSettings appliedFontSettings;
    private Popup fontSettingsPopup;
    private BaseController<?> currentSubController;
    private WorkbenchModuleDefinition activeDefinition;
    private javafx.beans.value.ChangeListener<Number> structureRevisionListener;
    private String selectedAlgorithmId;
    private final Set<Node> responsiveAddedSmall =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<Node> responsiveAddedDense =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private boolean compactLayout;
    private boolean narrowLayout;
    private boolean structureHistoryExpanded;
    private boolean timelineRuntimeVisible = true;
    private boolean timelineStructureVisible = true;
    private boolean timelineObservationVisible = true;
    /** True only while Structure mode is showing a saved snapshot as a read-only preview. */
    private boolean structureSnapshotPreviewActive;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (resources != null) {
            I18N.setLocale(resources.getLocale());
        }

        setupI18n();
        setupSnapshotPreviewPresentation();
        setupFontSettings();
        setupValueTypeSelectors();
        setupModuleMenu();
        setupPracticeWorkspace();
        setupWorkspaceMode();
        setupPlaybackSpeedButtons();
        setupTimelinePresentation();
        setupGlobalEffects();
        setupLayoutClips();
        setupResponsiveLayout();
        setStructureHistoryExpanded(false);
        WorkbenchTheme.apply(rootPane);
        WorkbenchTheme.leftPill(structureWorkspaceBtn);
        WorkbenchTheme.rightPill(practiceWorkspaceBtn);

        if (!moduleDefinitions.isEmpty()) {
            switchToModule(moduleDefinitions.getFirst());
        }
        appendSystemLog(I18N.text("message.system.initialized"));
    }

    private void setupI18n() {
        menuTitleLabel.textProperty().bind(I18N.createStringBinding("label.menu.title"));
        valueTypeLabel.textProperty().bind(I18N.createStringBinding("label.value_type"));
        hashValueTypeLabel.textProperty().bind(I18N.createStringBinding("label.value_type.value"));
        if (fontSettingsBtn != null) {
            fontSettingsBtn.accessibleTextProperty().bind(I18N.createStringBinding("settings.text.open"));
        }
        structureWorkspaceBtn.textProperty().bind(I18N.createStringBinding("label.workspace.structure"));
        algorithmWorkspaceBtn.textProperty().bind(I18N.createStringBinding("label.workspace.algorithm"));
        practiceWorkspaceBtn.textProperty().bind(I18N.createStringBinding("label.workspace.practice"));
        structureWorkspaceTitleLabel.textProperty().bind(I18N.createStringBinding("label.workspace.structure"));
        algorithmWorkspaceTitleLabel.textProperty().bind(I18N.createStringBinding("label.workspace.algorithm"));
        practiceWorkspaceTitleLabel.textProperty().bind(I18N.createStringBinding("label.workspace.practice"));
        practiceCatalogTitleLabel.textProperty().bind(I18N.createStringBinding("label.workspace.practice.catalog"));
        practiceCatalogHintLabel.textProperty().bind(I18N.createStringBinding("label.workspace.practice.hint"));
        practiceEmptyLabel.textProperty().bind(I18N.createStringBinding("label.workspace.practice.empty"));
        structureLiveLabel.setText(I18N.text("label.workspace.structure.live"));
        algorithmInputTitleLabel.textProperty().bind(I18N.createStringBinding("label.workspace.algorithm.input_source"));
        structurePreviewTitleLabel.textProperty().bind(
                I18N.createStringBinding("label.workspace.structure.preview"));
        structurePreviewHintLabel.textProperty().bind(
                I18N.createStringBinding("label.workspace.structure.preview.hint"));
        snapshotTitleLabel.textProperty().bind(I18N.createStringBinding("label.workspace.snapshots"));
        snapshotQuickTitleLabel.textProperty().bind(I18N.createStringBinding("label.workspace.snapshot.quick"));
        structurePrimaryMetricTitleLabel.textProperty().bind(I18N.createStringBinding("label.workspace.metric.nodes"));
        structureSecondaryMetricTitleLabel.textProperty().bind(I18N.createStringBinding("label.workspace.metric.height"));
        structureStateMetricTitleLabel.textProperty().bind(I18N.createStringBinding("label.workspace.metric.state"));
        selectedValueCaptionLabel.textProperty().bind(I18N.createStringBinding("label.workspace.selection.value"));
        algorithmSelectedValueCaptionLabel.textProperty().bind(I18N.createStringBinding("label.workspace.selection.value"));
        structureInspectorTab.textProperty().bind(I18N.createStringBinding("label.workspace.inspector"));
        structureSnapshotsTab.textProperty().bind(I18N.createStringBinding("label.workspace.snapshots"));
        inspectorSnapshotsHeadingLabel.textProperty().bind(I18N.createStringBinding("label.workspace.snapshots"));
        structureSnapshotPreviewBadgeLabel.textProperty().bind(
                I18N.createStringBinding("label.workspace.snapshot.preview_read_only"));
        snapshotPreviewNoticeTitleLabel.textProperty().bind(
                I18N.createStringBinding("label.workspace.snapshot.preview_read_only"));
        snapshotPreviewNoticeHintLabel.textProperty().bind(
                I18N.createStringBinding("label.workspace.snapshot.preview_hint"));
        snapshotPreviewRestoreBtn.textProperty().bind(
                I18N.createStringBinding("action.workspace.restore_snapshot"));
        overviewPrimaryTitleLabel.textProperty().bind(I18N.createStringBinding("label.workspace.metric.nodes"));
        overviewSecondaryTitleLabel.textProperty().bind(I18N.createStringBinding("label.workspace.metric.height"));
        overviewEventsTitleLabel.textProperty().bind(I18N.createStringBinding("label.workspace.metric.events"));
        overviewStateTitleLabel.textProperty().bind(I18N.createStringBinding("label.workspace.metric.state"));
        structureHistoryTitleLabel.textProperty().bind(I18N.createStringBinding("label.workspace.structure.history"));
        saveSnapshotBtn.textProperty().bind(I18N.createStringBinding("action.workspace.save_snapshot"));
        currentSelectionHeadingLabel.textProperty().bind(I18N.createStringBinding("label.workspace.selection.current"));
        structureOverviewHeadingLabel.textProperty().bind(I18N.createStringBinding("label.workspace.structure.overview"));
        algorithmViewTitleLabel.textProperty().bind(I18N.createStringBinding("label.workspace.algorithm.current_step"));
        viewportHintLabel.textProperty().bind(
                I18N.createStringBinding("label.workspace.algorithm.preview.hint"));
        statsTitleLabel.textProperty().bind(I18N.createStringBinding("label.workspace.run_summary"));
        statisticsTab.textProperty().bind(I18N.createStringBinding("label.workspace.statistics"));
        eventTab.textProperty().bind(I18N.createStringBinding("label.workspace.event"));
        logTab.textProperty().bind(I18N.createStringBinding("label.panel.log"));
        resultTab.textProperty().bind(I18N.createStringBinding("label.workspace.result"));
        currentEventHeadingLabel.textProperty().bind(I18N.createStringBinding("label.workspace.event.current"));
        eventRunSummaryHeadingLabel.textProperty().bind(I18N.createStringBinding("label.workspace.run_summary"));
        resultPreviewHeadingLabel.textProperty().bind(I18N.createStringBinding("label.workspace.result.preview"));
        resultHeadingLabel.textProperty().bind(I18N.createStringBinding("label.workspace.result"));
        logTitleLabel.textProperty().bind(I18N.createStringBinding("label.panel.log"));
        liveLabel.textProperty().bind(I18N.createStringBinding("label.panel.live"));
        startBtn.textProperty().bind(I18N.createStringBinding("action.execution.run_algorithm"));
        resetBtn.setText("✕");
        replayBtn.textProperty().bind(I18N.createStringBinding("action.execution.replay"));
        exportBtn.textProperty().bind(I18N.createStringBinding("action.execution.export"));
        compareBtn.textProperty().bind(I18N.createStringBinding("action.execution.compare"));
        delayLabel.textProperty().bind(I18N.createStringBinding("label.execution.delay"));
        timelineLabel.textProperty().bind(I18N.createStringBinding("label.execution.timeline.full"));
        speedLabel.textProperty().bind(I18N.createStringBinding("label.execution.speed"));
        timelineRuntimeLegendLabel.textProperty().bind(I18N.createStringBinding("label.execution.legend.runtime"));
        timelineStructureLegendLabel.textProperty().bind(I18N.createStringBinding("label.execution.legend.structure"));
        timelineObservationLegendLabel.textProperty().bind(I18N.createStringBinding("label.execution.legend.observation"));
        Label logPlaceholder = new Label();
        logPlaceholder.textProperty().bind(I18N.createStringBinding("label.panel.log.prompt"));
        logView.setPlaceholder(logPlaceholder);
        stepBackwardBtn.accessibleTextProperty().bind(
                I18N.createStringBinding("action.execution.step.backward"));
        stepForwardBtn.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(
                () -> I18N.text("action.execution.step.forward").toUpperCase(Locale.ROOT) + "  ▶|",
                I18N.localeProperty()));
        stepForwardBtn.accessibleTextProperty().bind(
                I18N.createStringBinding("action.execution.step.forward"));
        I18N.localeProperty().addListener((observable, oldValue, newValue) -> {
            if (fontSettingsPopup != null && fontSettingsPopup.isShowing()) {
                fontSettingsPopup.hide();
            }
            refreshPauseText();
            refreshWorkspaceContext();
            refreshTopContext();
            refreshAlgorithmInputSource();
            refreshSnapshotPreviewPresentation();
            refreshValueTypeSelectors();
            refreshStructureSummary();
            refreshExecutionPresentation();
            boolean selectionVisible = structureSelectionOverlay != null && structureSelectionOverlay.isVisible();
            if (algorithmSelectionOverlay != null && algorithmSelectionOverlay.isVisible()) {
                selectionVisible = true;
            }
            if (!selectionVisible) {
                clearStructureSelection();
            }
        });
        refreshPauseText();
    }

    private void setupSnapshotPreviewPresentation() {
        if (structureSnapshotPreviewBadge == null) {
            return;
        }
        structureSnapshotPreviewBadge.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    }

    private void setupFontSettings() {
        if (rootPane == null) {
            return;
        }
        appliedFontSettings = FONT_SETTINGS_SERVICE.load();
        FONT_SETTINGS_SERVICE.apply(rootPane, appliedFontSettings);
    }

    @FXML
    private void toggleFontSettings() {
        if (fontSettingsBtn == null) {
            return;
        }
        if (fontSettingsPopup != null && fontSettingsPopup.isShowing()) {
            fontSettingsPopup.hide();
            return;
        }
        Popup popup = createFontSettingsPopup();
        Bounds anchor = fontSettingsBtn.localToScreen(fontSettingsBtn.getBoundsInLocal());
        if (anchor == null) {
            return;
        }
        fontSettingsPopup = popup;
        popup.setOnHidden(event -> {
            if (fontSettingsPopup == popup) {
                fontSettingsPopup = null;
            }
        });
        popup.show(fontSettingsBtn, anchor.getMaxX(), anchor.getMaxY());
    }

    private Popup createFontSettingsPopup() {
        Popup popup = new Popup();
        popup.setAutoFix(true);
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);
        popup.setAnchorLocation(javafx.stage.PopupWindow.AnchorLocation.CONTENT_TOP_RIGHT);

        VBox popupShell = new VBox(0.0d);
        popupShell.setAlignment(javafx.geometry.Pos.TOP_RIGHT);
        popupShell.getStyleClass().add("font-settings-popup-shell");
        popupShell.getStylesheets().addAll(rootPane.getStylesheets());

        Region arrow = new Region();
        arrow.getStyleClass().add("font-settings-arrow");
        VBox.setMargin(arrow, new javafx.geometry.Insets(0.0d, 18.0d, 0.0d, 0.0d));

        VBox content = new VBox(14.0d);
        content.getStyleClass().add("font-settings-popover");

        FontSettings initial = appliedFontSettings;
        if (initial == null) {
            initial = FONT_SETTINGS_SERVICE.load();
        }
        FontSettings[] draft = new FontSettings[] { initial };
        Locale[] draftLocale = new Locale[] { I18N.getLocale() };
        boolean[] updatingControls = new boolean[] { false };

        Label title = new Label();
        title.textProperty().bind(I18N.createStringBinding("settings.text.title"));
        title.getStyleClass().add("font-settings-title");

        Label languageLabel = new Label();
        languageLabel.textProperty().bind(I18N.createStringBinding("settings.language"));
        languageLabel.getStyleClass().add("font-settings-row-label");
        ComboBox<String> languageSelector = new ComboBox<>();
        languageSelector.setMaxWidth(Double.MAX_VALUE);
        languageSelector.getStyleClass().addAll("font-settings-combo", "font-settings-language-combo");
        languageSelector.getItems().setAll("中文", "English");
        if (I18N.getLocale().getLanguage().equals("zh")) {
            languageSelector.getSelectionModel().select("中文");
        } else {
            languageSelector.getSelectionModel().select("English");
        }
        HBox languageRow = fontSettingsRow(languageLabel, languageSelector);

        Label familyLabel = new Label();
        familyLabel.textProperty().bind(I18N.createStringBinding("settings.text.font"));
        familyLabel.getStyleClass().add("font-settings-row-label");
        ComboBox<String> familySelector = new ComboBox<>();
        familySelector.setMaxWidth(Double.MAX_VALUE);
        familySelector.getStyleClass().add("font-settings-combo");
        String projectDefault = I18N.text("settings.text.fontDefault");
        List<String> families = new ArrayList<>();
        families.add(projectDefault);
        families.addAll(FONT_SETTINGS_SERVICE.availableFamilies());
        familySelector.getItems().setAll(families);
        if (initial.family().isBlank()) {
            familySelector.getSelectionModel().select(projectDefault);
        } else {
            familySelector.getSelectionModel().select(initial.family());
        }
        HBox familyRow = fontSettingsRow(familyLabel, familySelector);

        Label sizeLabel = new Label();
        sizeLabel.textProperty().bind(I18N.createStringBinding("settings.text.fontSize"));
        sizeLabel.getStyleClass().add("font-settings-row-label");
        Button decreaseSize = new Button("−");
        decreaseSize.getStyleClass().add("font-settings-size-button");
        decreaseSize.setAccessibleText(I18N.text("settings.text.decrease"));
        Button increaseSize = new Button("+");
        increaseSize.getStyleClass().add("font-settings-size-button");
        increaseSize.setAccessibleText(I18N.text("settings.text.increase"));
        Label sizeValue = new Label(formatFontSize(initial.size()));
        sizeValue.setMaxWidth(Double.MAX_VALUE);
        sizeValue.getStyleClass().add("font-settings-size-value");
        HBox.setHgrow(sizeValue, Priority.ALWAYS);
        HBox sizeControl = new HBox(0.0d, decreaseSize, sizeValue, increaseSize);
        sizeControl.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        sizeControl.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(sizeControl, Priority.ALWAYS);
        sizeControl.getStyleClass().add("font-settings-size-control");
        HBox sizeRow = fontSettingsRow(sizeLabel, sizeControl);

        Label colorLabel = new Label();
        colorLabel.textProperty().bind(I18N.createStringBinding("settings.text.fontColor"));
        colorLabel.getStyleClass().add("font-settings-row-label");
        ColorPicker colorPicker = new ColorPicker(FONT_SETTINGS_SERVICE.colorForPicker(initial));
        colorPicker.setMaxWidth(Double.MAX_VALUE);
        colorPicker.getStyleClass().add("font-settings-color-picker");
        HBox colorRow = fontSettingsRow(colorLabel, colorPicker);

        Label previewTitle = new Label();
        previewTitle.textProperty().bind(I18N.createStringBinding("settings.text.preview"));
        previewTitle.getStyleClass().add("font-settings-preview-title");
        Label previewPrimary = new Label();
        previewPrimary.textProperty().bind(I18N.createStringBinding("settings.text.preview.primary"));
        previewPrimary.getStyleClass().add("font-settings-preview-primary");
        Label previewArray = new Label();
        previewArray.textProperty().bind(I18N.createStringBinding("settings.text.preview.array"));
        Label previewStep = new Label();
        previewStep.textProperty().bind(I18N.createStringBinding("settings.text.preview.step"));
        VBox preview = new VBox(4.0d, previewPrimary, previewArray, previewStep);
        preview.getStyleClass().add("font-settings-preview");

        Runnable refreshPreview = () -> FONT_SETTINGS_SERVICE.applyPreview(preview, draft[0]);

        languageSelector.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (updatingControls[0] || newValue == null) {
                return;
            }
            Locale selectedLocale = Locale.CHINESE;
            if ("English".equals(newValue)) {
                selectedLocale = Locale.ENGLISH;
            }
            draftLocale[0] = selectedLocale;
        });

        familySelector.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (updatingControls[0] || newValue == null) {
                return;
            }
            String family = newValue;
            if (familySelector.getSelectionModel().getSelectedIndex() == 0) {
                family = "";
            }
            draft[0] = new FontSettings(family, draft[0].size(), draft[0].color());
            refreshPreview.run();
        });
        decreaseSize.setOnAction(event -> {
            double size = FONT_SETTINGS_SERVICE.clampSize(draft[0].size() - 1.0d);
            draft[0] = new FontSettings(draft[0].family(), size, draft[0].color());
            sizeValue.setText(formatFontSize(size));
            refreshPreview.run();
        });
        increaseSize.setOnAction(event -> {
            double size = FONT_SETTINGS_SERVICE.clampSize(draft[0].size() + 1.0d);
            draft[0] = new FontSettings(draft[0].family(), size, draft[0].color());
            sizeValue.setText(formatFontSize(size));
            refreshPreview.run();
        });
        colorPicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (updatingControls[0] || newValue == null) {
                return;
            }
            draft[0] = new FontSettings(
                    draft[0].family(), draft[0].size(), FONT_SETTINGS_SERVICE.toCssColor(newValue));
            refreshPreview.run();
        });

        Button reset = new Button();
        reset.textProperty().bind(I18N.createStringBinding("settings.text.reset"));
        reset.getStyleClass().add("font-settings-reset-button");
        reset.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(reset, Priority.ALWAYS);
        Button apply = new Button();
        apply.textProperty().bind(I18N.createStringBinding("settings.text.apply"));
        apply.getStyleClass().add("font-settings-apply-button");
        HBox footer = new HBox(10.0d, reset, apply);
        footer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        footer.getStyleClass().add("font-settings-footer");

        reset.setOnAction(event -> {
            FontSettings defaults = FONT_SETTINGS_SERVICE.defaults();
            updatingControls[0] = true;
            familySelector.getSelectionModel().select(projectDefault);
            sizeValue.setText(formatFontSize(defaults.size()));
            colorPicker.setValue(FONT_SETTINGS_SERVICE.colorForPicker(defaults));
            draftLocale[0] = I18N.getLocale();
            if ("zh".equals(draftLocale[0].getLanguage())) {
                languageSelector.getSelectionModel().select("中文");
            } else {
                languageSelector.getSelectionModel().select("English");
            }
            updatingControls[0] = false;
            draft[0] = defaults;
            refreshPreview.run();
        });
        apply.setOnAction(event -> {
            FontSettings normalized = FONT_SETTINGS_SERVICE.normalize(draft[0]);
            Locale selectedLocale = draftLocale[0];
            boolean languageChanged = selectedLocale != null
                    && !I18N.getLocale().getLanguage().equals(selectedLocale.getLanguage());
            FONT_SETTINGS_SERVICE.apply(rootPane, normalized);
            FONT_SETTINGS_SERVICE.save(normalized);
            appliedFontSettings = normalized;
            if (languageChanged) {
                I18N.setLocale(selectedLocale);
                appendSystemLog(I18N.text(
                        "message.system.language_switched",
                        selectedLocale.getDisplayLanguage(selectedLocale)));
            }
            rootPane.applyCss();
            rootPane.layout();
            updateResponsiveLayout(rootPane.getWidth(), rootPane.getHeight());
            boolean timelineExpanded = timelineDetails != null && timelineDetails.isVisible();
            setTimelineExpanded(timelineExpanded);
            rootPane.requestLayout();
            javafx.application.Platform.runLater(() -> {
                if (rootPane == null) {
                    return;
                }
                rootPane.applyCss();
                rootPane.layout();
                updateResponsiveLayout(rootPane.getWidth(), rootPane.getHeight());
            });
            popup.hide();
        });

        content.getChildren().setAll(
                title,
                languageRow,
                familyRow,
                sizeRow,
                colorRow,
                new Separator(),
                previewTitle,
                preview,
                new Separator(),
                footer);
        popupShell.getChildren().setAll(arrow, content);
        FONT_SETTINGS_SERVICE.apply(popupShell, initial);
        FONT_SETTINGS_SERVICE.applyPreview(preview, initial);
        WorkbenchTheme.apply(popupShell);
        popup.getContent().setAll(popupShell);
        return popup;
    }

    private HBox fontSettingsRow(Label label, Node control) {
        label.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        HBox.setHgrow(control, Priority.ALWAYS);
        HBox row = new HBox(12.0d, label, control);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.getStyleClass().add("font-settings-row");
        return row;
    }

    private String formatFontSize(double size) {
        double rounded = Math.rint(size);
        if (Math.abs(size - rounded) < 0.001d) {
            return String.format(Locale.ROOT, "%.0f px", rounded);
        }
        return String.format(Locale.ROOT, "%.2f px", size);
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
                return;
            }

            if (!valueTypeLabel.textProperty().isBound()) {
                valueTypeLabel.textProperty().bind(I18N.createStringBinding("label.value_type"));
            }
            List<String> available = availableValueTypes(moduleId);
            String selected = selectedValueTypes.get(moduleId);
            if (selected == null || !available.contains(selected)) {
                if (available.isEmpty()) {
                    selected = null;
                } else {
                    selected = available.getFirst();
                }
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
        selectedHashKeyType = null;
        selectedHashValueType = null;
        valueTypeSelector.getItems().clear();
        hashValueTypeSelector.getItems().clear();
    }

    private List<String> availableValueTypes(String moduleId) {
        return switch (moduleId) {
            case "array", "linked-list", "stack", "queue", "tree", "graph" -> ValueAdapters.supportedTypeNames();
            case "string" -> List.of(String.class.getSimpleName());
            default -> COMPONENTS.algorithmValueTypes(moduleId);
        };
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
        if (activeDefinition == null) {
            return;
        }
        configureRuntimeValueType(activeDefinition.id(), currentSubController);
        rebuildAlgorithmMenu();
        updateAlgorithmWorkspaceAvailability(activeDefinition.id());
        clearAlgorithmSelection();
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
        button.setMinWidth(0.0d);
        button.setPrefWidth(Region.USE_COMPUTED_SIZE);
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
        return moduleDefinitions.stream()
                .filter(definition -> definition.id().equals(moduleId))
                .map(WorkbenchModuleDefinition::name)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown Workbench module: " + moduleId))
                .toUpperCase(Locale.ROOT);
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
        button.setText(AlgorithmCatalog.name(item.id()));
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
        if (currentSubController instanceof AlgorithmSelectionSupport support) {
            support.selectAlgorithm(algorithmId);
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
            algorithmIds.addAll(AlgorithmCatalog.forWorkbenchModule(moduleId));
        } else {
            String selected = selectedValueType(moduleId);
            if (selected == null) {
                return List.of();
            }
            Class<?> valueType = ValueAdapters.requireType(selected);
            if (activeDefinition != null
                    && activeDefinition.id().equals(moduleId)
                    && currentSubController instanceof AlgorithmSelectionSupport support) {
                algorithmIds.addAll(support.algorithmIds());
            } else {
                algorithmIds.addAll(AlgorithmCatalog.forWorkbenchModule(moduleId, valueType));
            }
            List<String> registered = COMPONENTS.algorithmIds(moduleId, valueType.getSimpleName());
            algorithmIds.removeIf(id -> !registered.contains(id));
        }
        return algorithmIds.stream().distinct().map(AlgorithmNavigationItem::new).toList();
    }

    private void addAlgorithmsForAllTypes(List<String> target, String family, String excludedPrefix) {
        for (String valueType : COMPONENTS.algorithmValueTypes(family)) {
            for (String algorithmId : COMPONENTS.algorithmIds(family, valueType)) {
                if (excludedPrefix == null || !algorithmId.startsWith(excludedPrefix)) {
                    target.add(algorithmId);
                }
            }
        }
    }

    private enum WorkspaceMode {
        STRUCTURE,
        ALGORITHM,
        PRACTICE
    }

    private record ValueTypeOption(String type, boolean available) {
    }

    private record AlgorithmNavigationItem(String id) {
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
        setWorkspaceMode(structure ? WorkspaceMode.STRUCTURE : WorkspaceMode.ALGORITHM);
    }

    @FXML
    private void selectPracticeWorkspace() {
        setWorkspaceMode(WorkspaceMode.PRACTICE);
    }

    private void setWorkspaceMode(WorkspaceMode mode) {
        boolean structure = mode == WorkspaceMode.STRUCTURE;
        boolean algorithm = mode == WorkspaceMode.ALGORITHM;
        boolean practice = mode == WorkspaceMode.PRACTICE;

        structureWorkspaceBtn.pseudoClassStateChanged(SELECTED, structure);
        algorithmWorkspaceBtn.pseudoClassStateChanged(SELECTED, algorithm);
        practiceWorkspaceBtn.pseudoClassStateChanged(SELECTED, practice);
        structureWorkspacePane.pseudoClassStateChanged(WORKSPACE_FOCUS, structure);
        algorithmWorkspacePane.pseudoClassStateChanged(WORKSPACE_FOCUS, algorithm);
        practiceWorkspacePane.pseudoClassStateChanged(WORKSPACE_FOCUS, practice);
        setPageVisibility(structureWorkspacePane, structure);
        setPageVisibility(algorithmWorkspacePane, algorithm);
        setPageVisibility(practiceWorkspacePane, practice);
        refreshExecutionDockVisibility(!algorithm);
        if (!practice) {
            attachVisualizer(structure);
        }
        structureSnapshotPreviewActive = false;
        if (currentSubController instanceof TreeController treeController) {
            treeController.setStructureSelectionEnabled(structure);
        }
        if (currentSubController instanceof ArrayController arrayController) {
            arrayController.setStructureSelectionEnabled(structure);
        }
        if (currentSubController instanceof StringController stringController) {
            stringController.setStructureSelectionEnabled(structure);
        }
        if (currentSubController instanceof MazeController mazeController) {
            mazeController.setStructureSelectionEnabled(structure);
        }
        if (currentSubController instanceof GraphController graphController) {
            graphController.setStructureSelectionEnabled(structure);
        }
        if (currentSubController instanceof LinkedListController linkedController) {
            linkedController.setStructureSelectionEnabled(structure);
        }
        if (currentSubController instanceof LinearStructureController linearController) {
            linearController.setStructureSelectionEnabled(structure);
        }
        if (structure && currentSubController != null) {
            if (activeDefinition != null) {
                selectedSnapshotIds.remove(activeDefinition.id());
            }
            clearStructureSelection();
            currentSubController.showStructureState();
        }
        if (algorithm && currentSubController != null) {
            syncSnapshotSelectionFromAlgorithmInput();
            currentSubController.showAlgorithmState();
        }
        refreshSnapshotCards();
        refreshAlgorithmInputSource();
        updateWorkspaceInteractionState();
        refreshTopContext();
        refreshExecutionPresentation();
        if (practice) {
            practiceWorkspacePane.requestFocus();
        } else if (structure) {
            structureWorkspacePane.requestFocus();
        } else {
            algorithmWorkspacePane.requestFocus();
        }
    }

    private void setupPracticeWorkspace() {
        List<ProblemDescriptor> problems = PRACTICE_PROBLEMS.problems();
        practiceProblemList.getItems().setAll(problems);
        practiceProblemList.setCellFactory(ignored -> new ListCell<>() {
            @Override
            protected void updateItem(ProblemDescriptor item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                String number = item.number().isBlank() ? item.id() : item.number();
                setText(item.name() + "  ·  " + item.source().name() + " #" + number);
            }
        });
        practiceProblemList.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> showPracticeProblem(selected));
        if (problems.isEmpty()) {
            showPracticeProblem(null);
        } else {
            practiceProblemList.getSelectionModel().selectFirst();
        }
    }

    private void showPracticeProblem(ProblemDescriptor problem) {
        boolean selected = problem != null;
        practiceProblemNameLabel.setVisible(selected);
        practiceProblemNameLabel.setManaged(selected);
        practiceProblemMetaLabel.setVisible(selected);
        practiceProblemMetaLabel.setManaged(selected);
        practiceProblemEntryLabel.setVisible(selected);
        practiceProblemEntryLabel.setManaged(selected);
        practiceEmptyLabel.setVisible(!selected);
        practiceEmptyLabel.setManaged(!selected);
        if (!selected) {
            practiceProblemNameLabel.setText("—");
            practiceProblemMetaLabel.setText("");
            practiceProblemEntryLabel.setText("");
            return;
        }
        String number = problem.number().isBlank() ? problem.id() : problem.number();
        String tags = problem.tags().isEmpty() ? "" : "  ·  " + String.join(", ", problem.tags());
        practiceProblemNameLabel.setText(problem.name());
        practiceProblemMetaLabel.setText(
                problem.source().name() + " #" + number + "  ·  " + problem.difficulty().name() + tags);
        practiceProblemEntryLabel.setText(
                problem.implementation().getName() + "#" + problem.entryPoint().getName());
    }

    private void setupTimelinePresentation() {
        if (timelineMarkers != null) {
            timelineMarkers.widthProperty().addListener((observable, oldValue, newValue) -> rebuildTimelineMarkers());
        }
        configureTimelineLegendToggle(timelineRuntimeLegendLabel, TimelineMarkerCategory.RUNTIME);
        configureTimelineLegendToggle(timelineStructureLegendLabel, TimelineMarkerCategory.STRUCTURE);
        configureTimelineLegendToggle(timelineObservationLegendLabel, TimelineMarkerCategory.OBSERVATION);
        refreshTimelineLegendState();
        setTimelineExpanded(false);
    }

    private void configureTimelineLegendToggle(Label label, TimelineMarkerCategory category) {
        if (label == null) {
            return;
        }
        label.getStyleClass().add("timeline-legend-toggle");
        label.setFocusTraversable(true);
        label.setOnMouseClicked(event -> {
            toggleTimelineMarkerCategory(category);
            event.consume();
        });
        label.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
                toggleTimelineMarkerCategory(category);
                event.consume();
            }
        });
    }

    private void toggleTimelineMarkerCategory(TimelineMarkerCategory category) {
        switch (category) {
            case RUNTIME -> timelineRuntimeVisible = !timelineRuntimeVisible;
            case STRUCTURE -> timelineStructureVisible = !timelineStructureVisible;
            case OBSERVATION -> timelineObservationVisible = !timelineObservationVisible;
        }
        refreshTimelineLegendState();
        rebuildTimelineMarkers();
    }

    private void refreshTimelineLegendState() {
        updateTimelineLegendState(timelineRuntimeLegendLabel, timelineRuntimeVisible);
        updateTimelineLegendState(timelineStructureLegendLabel, timelineStructureVisible);
        updateTimelineLegendState(timelineObservationLegendLabel, timelineObservationVisible);
    }

    private void updateTimelineLegendState(Label label, boolean visible) {
        if (label == null) {
            return;
        }
        if (visible) {
            label.getStyleClass().remove("timeline-legend-muted");
        } else if (!label.getStyleClass().contains("timeline-legend-muted")) {
            label.getStyleClass().add("timeline-legend-muted");
        }
    }

    @FXML
    private void toggleTimelineDetails() {
        boolean expanded = timelineDetails != null && !timelineDetails.isVisible();
        setTimelineExpanded(expanded);
        if (expanded) {
            rebuildTimelineMarkers();
        }
    }

    private void setTimelineExpanded(boolean expanded) {
        if (timelineDetails != null) {
            timelineDetails.setManaged(expanded);
            timelineDetails.setVisible(expanded);
        }
        if (bottomDock != null) {
            FontSettingsService.LayoutTier tier = currentTypographyTier();
            double collapsedHeight = 62.0d;
            double expandedMinHeight = 96.0d;
            double expandedPrefHeight = 104.0d;
            if (tier == FontSettingsService.LayoutTier.LARGE) {
                collapsedHeight = 70.0d;
                expandedMinHeight = 110.0d;
                expandedPrefHeight = 120.0d;
            } else if (tier == FontSettingsService.LayoutTier.XLARGE) {
                collapsedHeight = 78.0d;
                expandedMinHeight = 124.0d;
                expandedPrefHeight = 136.0d;
            }
            if (expanded) {
                bottomDock.setMinHeight(expandedMinHeight);
                bottomDock.setPrefHeight(expandedPrefHeight);
            } else {
                bottomDock.setMinHeight(collapsedHeight);
                bottomDock.setPrefHeight(collapsedHeight);
            }
            bottomDock.getStyleClass().removeAll("timeline-collapsed", "timeline-expanded");
            if (expanded) {
                bottomDock.getStyleClass().add("timeline-expanded");
            } else {
                bottomDock.getStyleClass().add("timeline-collapsed");
            }
        }
        if (timelineToggleBtn != null) {
            if (expanded) {
                timelineToggleBtn.setText("▼");
            } else {
                timelineToggleBtn.setText("▲");
            }
        }
    }

    @FXML
    private void toggleStructureHistory() {
        setStructureHistoryExpanded(!structureHistoryExpanded);
    }

    private void setStructureHistoryExpanded(boolean expanded) {
        structureHistoryExpanded = expanded;
        if (structureHistoryDetails != null) {
            structureHistoryDetails.setManaged(expanded);
            structureHistoryDetails.setVisible(expanded);
        }
        if (structureHistoryToggleBtn != null) {
            if (expanded) {
                structureHistoryToggleBtn.setText("▼");
            } else {
                structureHistoryToggleBtn.setText("▲");
            }
        }
        updateStructureHistoryGeometry();
    }

    private void updateStructureHistoryGeometry() {
        if (structureHistoryDock == null || narrowLayout) {
            return;
        }
        FontSettingsService.LayoutTier tier = currentTypographyTier();
        double collapsedHeight = 52.0d;
        double compactExpandedHeight = 124.0d;
        double expandedHeight = 168.0d;
        if (tier == FontSettingsService.LayoutTier.LARGE) {
            collapsedHeight = 60.0d;
            compactExpandedHeight = 146.0d;
            expandedHeight = 190.0d;
        } else if (tier == FontSettingsService.LayoutTier.XLARGE) {
            collapsedHeight = 68.0d;
            compactExpandedHeight = 168.0d;
            expandedHeight = 214.0d;
        }
        double height;
        if (structureHistoryExpanded) {
            if (compactLayout) {
                height = compactExpandedHeight;
            } else {
                height = expandedHeight;
            }
        } else {
            height = collapsedHeight;
        }
        structureHistoryDock.setMinHeight(height);
        structureHistoryDock.setPrefHeight(height);
        structureHistoryDock.setMaxHeight(height);
        structureHistoryDock.getStyleClass().removeAll("history-collapsed", "history-expanded");
        if (structureHistoryExpanded) {
            structureHistoryDock.getStyleClass().add("history-expanded");
        } else {
            structureHistoryDock.getStyleClass().add("history-collapsed");
        }
    }

    private void setupGlobalEffects() {
        EffectUtils.applyDynamicEffect(
                structureWorkspaceBtn, algorithmWorkspaceBtn, fontSettingsBtn,
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
                updateResponsiveLayout(newValue.doubleValue(), rootPane.getHeight()));
        rootPane.heightProperty().addListener((observable, oldValue, newValue) ->
                updateResponsiveLayout(rootPane.getWidth(), newValue.doubleValue()));
        updateResponsiveLayout(rootPane.getWidth(), rootPane.getHeight());
    }

    private void updateResponsiveLayout(double width, double height) {
        if (rootPane == null) {
            return;
        }
        boolean hasWidth = width > 0.0d;
        boolean hasHeight = height > 0.0d;
        boolean nextCompactLayout = (hasWidth && width < COMPACT_LAYOUT_WIDTH)
                || (hasHeight && height < COMPACT_LAYOUT_HEIGHT);
        boolean nextNarrowLayout = (hasWidth && width < NARROW_LAYOUT_WIDTH)
                || (hasHeight && height < NARROW_LAYOUT_HEIGHT);

        compactLayout = nextCompactLayout;
        narrowLayout = nextNarrowLayout;
        rootPane.pseudoClassStateChanged(COMPACT_LAYOUT, compactLayout);
        rootPane.pseudoClassStateChanged(NARROW_LAYOUT, narrowLayout);

        ResponsiveGeometry geometry = responsiveGeometry(nextCompactLayout, nextNarrowLayout);
        double familyWidth = geometry.familyWidth();
        double controlWidth = geometry.controlWidth();
        double inspectorWidth = geometry.inspectorWidth();
        double topBarHeight = geometry.topBarHeight();
        double brandWidth = geometry.brandWidth();
        double modeWidth = geometry.modeWidth();
        double contextWidth = geometry.contextWidth();

        setFixedWidth(structureFamilyRail, familyWidth);
        setFixedWidth(algorithmFamilyRail, familyWidth);
        setFixedWidth(structureControlRail, controlWidth);
        setFixedWidth(algorithmControlRail, controlWidth);
        setFixedWidth(snapshotPanel, inspectorWidth);
        setFixedWidth(diagnosticsPanel, inspectorWidth);
        setFixedWidth(brandZone, brandWidth);
        setFixedWidth(workspaceModeBox, modeWidth);
        setFixedWidth(topContextZone, contextWidth);
        setFixedHeight(topBar, topBarHeight);
        setOverlayGeometry(structureSelectionOverlay, geometry.overlayWidth());
        setOverlayGeometry(currentStepOverlay, geometry.overlayWidth());
        setOverlayGeometry(algorithmSelectionOverlay, geometry.overlayWidth());

        snapshotPanel.setManaged(true);
        snapshotPanel.setVisible(true);
        setPageVisibility(diagnosticsPanel, !narrowLayout);
        setPageVisibility(structureHistoryDock, !narrowLayout);
        updateStructureHistoryGeometry();

        setControlVisibility(brandSubtitle, !nextNarrowLayout);
        setControlVisibility(topContextLabel, !nextNarrowLayout);
        setControlVisibility(runIdLabel, !nextCompactLayout);
        setControlVisibility(fontSettingsBtn, true);
        applyResponsiveControlDensity(nextCompactLayout);
    }

    private FontSettingsService.LayoutTier currentTypographyTier() {
        return FONT_SETTINGS_SERVICE.layoutTier(appliedFontSettings);
    }

    private ResponsiveGeometry responsiveGeometry(boolean compact, boolean narrow) {
        FontSettingsService.LayoutTier tier = currentTypographyTier();
        if (narrow) {
            if (tier == FontSettingsService.LayoutTier.XLARGE) {
                return new ResponsiveGeometry(132.0d, 300.0d, 280.0d, 64.0d, 220.0d, 250.0d, 220.0d, 270.0d);
            }
            if (tier == FontSettingsService.LayoutTier.LARGE) {
                return new ResponsiveGeometry(108.0d, 280.0d, 240.0d, 58.0d, 220.0d, 250.0d, 220.0d, 240.0d);
            }
            return new ResponsiveGeometry(84.0d, 220.0d, 196.0d, 52.0d, 220.0d, 250.0d, 220.0d, 220.0d);
        }
        if (compact) {
            if (tier == FontSettingsService.LayoutTier.XLARGE) {
                return new ResponsiveGeometry(166.0d, 340.0d, 360.0d, 72.0d, 300.0d, 300.0d, 320.0d, 290.0d);
            }
            if (tier == FontSettingsService.LayoutTier.LARGE) {
                return new ResponsiveGeometry(138.0d, 300.0d, 320.0d, 64.0d, 300.0d, 300.0d, 320.0d, 260.0d);
            }
            return new ResponsiveGeometry(104.0d, 250.0d, 260.0d, 56.0d, 300.0d, 300.0d, 320.0d, 220.0d);
        }
        if (tier == FontSettingsService.LayoutTier.XLARGE) {
            return new ResponsiveGeometry(208.0d, 420.0d, 460.0d, 86.0d, 430.0d, 420.0d, 500.0d, 320.0d);
        }
        if (tier == FontSettingsService.LayoutTier.LARGE) {
            return new ResponsiveGeometry(172.0d, 360.0d, 410.0d, 78.0d, 430.0d, 420.0d, 500.0d, 280.0d);
        }
        return new ResponsiveGeometry(142.0d, 320.0d, 360.0d, 72.0d, 430.0d, 420.0d, 500.0d, 220.0d);
    }

    private static void setOverlayGeometry(Region overlay, double width) {
        if (overlay == null) {
            return;
        }
        overlay.setMinWidth(width);
        overlay.setPrefWidth(width);
        overlay.setMaxWidth(width);
        overlay.setMinHeight(Region.USE_COMPUTED_SIZE);
        overlay.setPrefHeight(Region.USE_COMPUTED_SIZE);
        overlay.setMaxHeight(Region.USE_PREF_SIZE);
    }

    private record ResponsiveGeometry(
            double familyWidth,
            double controlWidth,
            double inspectorWidth,
            double topBarHeight,
            double brandWidth,
            double modeWidth,
            double contextWidth,
            double overlayWidth) {
    }

    private static void setFixedWidth(Region region, double width) {
        if (region == null) {
            return;
        }
        region.setMinWidth(width);
        region.setPrefWidth(width);
        region.setMaxWidth(width);
    }

    private static void setFixedHeight(Region region, double height) {
        if (region == null) {
            return;
        }
        region.setMinHeight(height);
        region.setPrefHeight(height);
        region.setMaxHeight(height);
    }

    private void applyResponsiveControlDensity(boolean compact) {
        applyResponsiveControlDensity(rootPane, compact);
    }

    private void applyResponsiveControlDensity(Node node, boolean compact) {
        if (node == null) {
            return;
        }
        if (node instanceof TabPane) {
            setResponsiveStyleClass(node, Styles.DENSE, compact, responsiveAddedDense);
        } else if (node instanceof Button
                || node instanceof ComboBoxBase<?>
                || node instanceof TextInputControl
                || node instanceof Spinner<?>
                || node instanceof Slider) {
            setResponsiveStyleClass(node, Styles.SMALL, compact, responsiveAddedSmall);
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                applyResponsiveControlDensity(child, compact);
            }
        }
    }

    private static void setResponsiveStyleClass(
            Node node,
            String styleClass,
            boolean enabled,
            Set<Node> ownedNodes) {
        if (enabled) {
            if (!node.getStyleClass().contains(styleClass)) {
                node.getStyleClass().add(styleClass);
                ownedNodes.add(node);
            }
            return;
        }
        if (ownedNodes.remove(node)) {
            node.getStyleClass().remove(styleClass);
        }
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
        double width;
        if (compact) {
            width = 190.0d;
        } else {
            width = 204.0d;
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
        structureSnapshotPreviewActive = false;
        clearStructureSelection();
        refreshValueTypeSelectors();
        BaseController<?> nextController = definition.controllerFactory().get();
        configureRuntimeValueType(definition.id(), nextController);
        loadSubController(nextController);
        rebuildAlgorithmMenu();
        syncAlgorithmSelectionFromController();
        updateAlgorithmWorkspaceAvailability(definition.id());
        refreshWorkspaceContext();
        structureButtons.forEach((id, buttons) -> buttons.forEach(button ->
                button.pseudoClassStateChanged(SELECTED, id.equals(definition.id()))));
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
        practiceWorkspaceBtn.setDisable(running);
        structureButtons.values().forEach(buttons -> buttons.forEach(button -> button.setDisable(running)));
        algorithmButtons.values().forEach(buttons -> buttons.values().forEach(button -> button.setDisable(running)));
        if (structureControlsHost != null) {
            structureControlsHost.setDisable(running || structureSnapshotPreviewActive);
        }
        refreshSnapshotPreviewPresentation();
    }


    private void refreshSnapshotPreviewPresentation() {
        boolean visible = isStructurePageVisible() && structureSnapshotPreviewActive;
        StructureSnapshot<?> snapshot = null;
        if (visible) {
            snapshot = selectedSavedSnapshot(false);
            if (snapshot == null) {
                visible = false;
                structureSnapshotPreviewActive = false;
            }
        }

        if (structureSnapshotPreviewBadge != null) {
            structureSnapshotPreviewBadge.setManaged(visible);
            structureSnapshotPreviewBadge.setVisible(visible);
        }
        if (snapshotPreviewNotice != null) {
            snapshotPreviewNotice.setManaged(visible);
            snapshotPreviewNotice.setVisible(visible);
        }
        if (snapshotPreviewRestoreBtn != null) {
            snapshotPreviewRestoreBtn.setDisable(!visible);
        }
        if (structureLiveLabel != null) {
            if (visible) {
                structureLiveLabel.setText(I18N.text("label.workspace.structure.preview"));
            } else {
                structureLiveLabel.setText(I18N.text("label.workspace.structure.live"));
            }
        }

        String snapshotId = "";
        if (snapshot != null) {
            snapshotId = I18N.text("label.workspace.snapshot.preview_id", shortSnapshotId(snapshot));
        }
        if (structureSnapshotPreviewIdLabel != null) {
            structureSnapshotPreviewIdLabel.setText(snapshotId);
        }
        if (snapshotPreviewNoticeIdLabel != null) {
            snapshotPreviewNoticeIdLabel.setText(snapshotId);
        }
    }

    @FXML
    private void restoreSelectedPreviewSnapshot() {
        if (!structureSnapshotPreviewActive) {
            return;
        }
        StructureSnapshot<?> snapshot = selectedSavedSnapshot(false);
        if (snapshot == null) {
            return;
        }
        restoreSnapshot(snapshot);
        updateWorkspaceInteractionState();
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

    private void syncAlgorithmSelectionFromController() {
        String algorithmId = null;
        if (currentSubController instanceof AlgorithmSelectionSupport support) {
            algorithmId = support.selectedAlgorithmId();
        }
        handleAlgorithmSelectionChanged(algorithmId);
    }

    private void handleAlgorithmSelectionChanged(String algorithmId) {
        selectedAlgorithmId = algorithmId;
        rebuildAlgorithmMenu();
        clearAlgorithmSelection();
        if (activeDefinition != null && algorithmId != null) {
            selectAlgorithmButton(activeDefinition.id(), algorithmId);
        }
        if (activeDefinition != null) {
            updateAlgorithmWorkspaceAvailability(activeDefinition.id());
        }
        refreshTopContext();
    }

    private void configureRuntimeValueType(String moduleId, BaseController<?> controller) {
        if (!(controller instanceof RuntimeValueTypeSupport support)) {
            return;
        }
        String selected = selectedValueType(moduleId);
        if (selected == null) {
            return;
        }
        Class<?> valueType = ValueAdapters.requireType(selected);
        if (!support.supportedValueTypes().contains(valueType)) {
            throw new IllegalArgumentException("Controller for " + moduleId
                    + " does not support runtime value type " + valueType.getName());
        }
        support.setRuntimeValueType(valueType);
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
        wireAlgorithmSelection();
        wireStructureSelection();
        structureRevisionListener = (observable, oldValue, newValue) -> {
            if (isStructurePageVisible()) {
                structureSnapshotPreviewActive = false;
                if (activeDefinition != null) {
                    selectedSnapshotIds.remove(activeDefinition.id());
                }
            }
            refreshSnapshotCards();
            refreshStructureSummary();
            updateWorkspaceInteractionState();
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
        updateResponsiveLayout(rootPane.getWidth(), rootPane.getHeight());
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

    private boolean isPracticePageVisible() {
        return practiceWorkspacePane != null && practiceWorkspacePane.isManaged();
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
            VBox target;
            if (isAlgorithmSection(section)) {
                target = algorithmControlsHost;
            } else {
                target = structureControlsHost;
            }
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
        String moduleName = activeDefinition.name();
        structureWorkspaceSubtitleLabel.setText(moduleName);
        algorithmWorkspaceSubtitleLabel.setText(moduleName);
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
        String moduleName = activeDefinition.name();
        StructureSnapshotSupport<?> support = currentSnapshotSupport();
        if (support == null) {
            snapshotCards.getChildren().clear();
            if (inspectorSnapshotCards != null) inspectorSnapshotCards.getChildren().clear();
            selectedSnapshotIds.remove(activeDefinition.id());
            snapshotCountLabel.setText(I18N.text(
                    "label.workspace.snapshot.count", 0,
                    structureSnapshotStore.maxSnapshotsPerModule()));
            updateSnapshotActionState();
            refreshStructureHistory();
            return;
        }

        snapshotCards.getChildren().clear();
        if (inspectorSnapshotCards != null) inspectorSnapshotCards.getChildren().clear();
        List<StructureSnapshot<?>> saved = structureSnapshotStore.snapshots(activeDefinition.id());
        String selectedSnapshotId = validSelectedSnapshotId(saved);

        StructureSnapshot<?> current = support.captureStructureSnapshot();
        snapshotCards.getChildren().add(createSnapshotCard(
                moduleName, I18N.text("label.workspace.snapshot.current"), current, support, true,
                selectedSnapshotId == null));

        if (inspectorSnapshotCards != null) {
            inspectorSnapshotCards.getChildren().add(createInspectorCurrentSnapshotCard(
                    current, support, selectedSnapshotId == null));
        }
        for (StructureSnapshot<?> snapshot : saved) {
            boolean selected = snapshot.id().equals(selectedSnapshotId);
            snapshotCards.getChildren().add(createSnapshotCard(
                    moduleName, I18N.text("label.workspace.snapshot.saved"), snapshot,
                    support, false, selected));
            if (inspectorSnapshotCards != null) {
                inspectorSnapshotCards.getChildren().add(createInspectorSnapshotCard(
                        snapshot, support, selected));
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

    private String validSelectedSnapshotId(List<StructureSnapshot<?>> snapshots) {
        if (activeDefinition == null) {
            return null;
        }
        String moduleId = activeDefinition.id();
        String selectedId = selectedSnapshotIds.get(moduleId);
        if (selectedId == null) {
            return null;
        }
        for (StructureSnapshot<?> snapshot : snapshots) {
            if (snapshot.id().equals(selectedId)) {
                return selectedId;
            }
        }
        selectedSnapshotIds.remove(moduleId);
        return null;
    }

    private StructureSnapshot<?> selectedSavedSnapshot(boolean fallbackToNewest) {
        if (activeDefinition == null) {
            return null;
        }
        List<StructureSnapshot<?>> snapshots = structureSnapshotStore.snapshots(activeDefinition.id());
        String selectedId = validSelectedSnapshotId(snapshots);
        if (selectedId != null) {
            for (StructureSnapshot<?> snapshot : snapshots) {
                if (snapshot.id().equals(selectedId)) {
                    return snapshot;
                }
            }
        }
        if (!fallbackToNewest || snapshots.isEmpty()) {
            return null;
        }
        StructureSnapshot<?> newest = snapshots.getFirst();
        selectedSnapshotIds.put(activeDefinition.id(), newest.id());
        return newest;
    }

    private void selectCurrentSnapshotCard() {
        if (activeDefinition == null || currentSubController == null || currentSubController.isRunning()) {
            return;
        }
        selectedSnapshotIds.remove(activeDefinition.id());
        if (isStructurePageVisible()) {
            structureSnapshotPreviewActive = false;
            clearStructureSelection();
            currentSubController.showStructureState();
            refreshSnapshotCards();
            refreshStructureSummary();
            updateSnapshotActionState();
            updateWorkspaceInteractionState();
            refreshTopContext();
            return;
        }
        applyCurrentStructureAlgorithmInput(false);
    }

    private void selectSavedSnapshotCard(StructureSnapshot<?> snapshot) {
        if (snapshot == null || activeDefinition == null || currentSubController == null
                || currentSubController.isRunning() || !activeDefinition.id().equals(snapshot.moduleId())) {
            return;
        }
        selectedSnapshotIds.put(snapshot.moduleId(), snapshot.id());
        if (isStructurePageVisible()) {
            previewSavedStructureSnapshot(snapshot);
            return;
        }
        SnapshotAlgorithmInputSupport<?> inputSupport = currentAlgorithmInputSupport();
        if (inputSupport != null) {
            applySavedSnapshotAlgorithmInput(snapshot, false);
            return;
        }
        refreshSnapshotCards();
        refreshAlgorithmInputSource();
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

    private Node createInspectorCurrentSnapshotCard(
            StructureSnapshot<?> snapshot,
            StructureSnapshotSupport<?> support,
            boolean selected) {
        VBox card = new VBox(5);
        card.setMaxWidth(Double.MAX_VALUE);
        card.getStyleClass().add("snapshot-card");
        card.getStyleClass().add("snapshot-card-current");
        if (selected) {
            card.getStyleClass().add("snapshot-card-selected");
        }
        card.setOnMouseClicked(event -> selectCurrentSnapshotCard());
        HBox header = new HBox(8);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label title = new Label(I18N.text("label.workspace.snapshot.current"));
        title.getStyleClass().add("snapshot-card-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label state = new Label(I18N.text("label.workspace.snapshot.current_state"));
        state.getStyleClass().add("snapshot-card-state");
        header.getChildren().addAll(title, spacer, state);
        Label detail = new Label(describeSnapshot(support, snapshot));
        detail.setWrapText(true);
        detail.getStyleClass().add("snapshot-card-detail");
        card.getChildren().addAll(header, detail);
        return card;
    }

    private Node createInspectorSnapshotCard(
            StructureSnapshot<?> snapshot,
            StructureSnapshotSupport<?> support,
            boolean selected) {
        VBox card = new VBox(5);
        card.setMaxWidth(Double.MAX_VALUE);
        card.getStyleClass().add("snapshot-card");
        card.getStyleClass().add("snapshot-card-saved");
        if (selected) card.getStyleClass().add("snapshot-card-selected");
        card.setOnMouseClicked(event -> selectSavedSnapshotCard(snapshot));
        HBox header = new HBox(8);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label title = new Label(I18N.text("label.workspace.snapshot.card", shortSnapshotId(snapshot)));
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
            boolean current,
            boolean selected) {
        VBox card = new VBox(6);
        card.setMaxWidth(Double.MAX_VALUE);
        card.getStyleClass().add("snapshot-card");
        if (current) {
            card.getStyleClass().add("snapshot-card-current");
            card.setOnMouseClicked(event -> selectCurrentSnapshotCard());
        } else {
            card.getStyleClass().add("snapshot-card-saved");
            card.setOnMouseClicked(event -> selectSavedSnapshotCard(snapshot));
        }
        if (selected) {
            card.getStyleClass().add("snapshot-card-selected");
        }

        Label title;
        if (current) {
            title = new Label(moduleName);
        } else {
            title = new Label(moduleName + " · " + shortSnapshotId(snapshot));
        }
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
            String inputSnapshotId = algorithmInputSupport.algorithmInputSnapshotId();
            boolean algorithmInput;
            if (current) {
                algorithmInput = inputSnapshotId == null;
            } else {
                algorithmInput = snapshot.id().equals(inputSnapshotId);
            }
            if (algorithmInput) {
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
            Button useInput;
            if (current) {
                useInput = new Button(I18N.text("action.workspace.use_current_input"));
            } else {
                useInput = new Button(I18N.text("action.workspace.use_snapshot_input"));
            }
            useInput.getStyleClass().add("snapshot-card-action");
            WorkbenchTheme.applyControl(useInput);
            useInput.setOnAction(event -> {
                if (current) {
                    applyCurrentStructureAlgorithmInput(true);
                } else {
                    selectedSnapshotIds.put(snapshot.moduleId(), snapshot.id());
                    applySavedSnapshotAlgorithmInput(snapshot, true);
                }
            });
            actions.getChildren().add(useInput);
        }
        if (!actions.getChildren().isEmpty()) {
            actions.setOnMouseClicked(event -> event.consume());
            card.getChildren().add(actions);
        }
        return card;
    }

    private void previewSavedStructureSnapshot(StructureSnapshot<?> snapshot) {
        StructureSnapshotSupport<?> support = currentSnapshotSupport();
        if (support == null || snapshot == null || currentSubController == null || currentSubController.isRunning()) {
            return;
        }
        try {
            previewSnapshotUnchecked(support, snapshot);
        } catch (RuntimeException exception) {
            appendSystemLog(I18N.text("message.snapshot.preview_failed"));
            return;
        }
        structureSnapshotPreviewActive = true;
        clearStructureSelection();
        refreshSnapshotCards();
        refreshStructureSummary();
        updateSnapshotActionState();
        updateWorkspaceInteractionState();
        refreshTopContext();
    }

    private void applyCurrentStructureAlgorithmInput(boolean logSelection) {
        SnapshotAlgorithmInputSupport<?> support = currentAlgorithmInputSupport();
        if (support == null || currentSubController == null || currentSubController.isRunning()) {
            return;
        }
        support.useCurrentStructureAsAlgorithmInput();
        if (activeDefinition != null) {
            selectedSnapshotIds.remove(activeDefinition.id());
        }
        if (!isStructurePageVisible()) {
            currentSubController.showAlgorithmState();
        }
        refreshAlgorithmInputSource();
        refreshSnapshotCards();
        refreshTopContext();
        refreshExecutionPresentation();
        if (logSelection) {
            appendSystemLog(I18N.text("message.snapshot.input_current", ""));
        }
    }

    private void applySavedSnapshotAlgorithmInput(StructureSnapshot<?> snapshot, boolean logSelection) {
        SnapshotAlgorithmInputSupport<?> support = currentAlgorithmInputSupport();
        if (support == null || snapshot == null || currentSubController == null || currentSubController.isRunning()) {
            return;
        }
        selectedSnapshotIds.put(snapshot.moduleId(), snapshot.id());
        try {
            useSnapshotAsAlgorithmInputUnchecked(support, snapshot);
        } catch (RuntimeException exception) {
            appendSystemLog(I18N.text("message.snapshot.input_failed"));
            return;
        }
        if (!isStructurePageVisible()) {
            currentSubController.showAlgorithmState();
        }
        refreshAlgorithmInputSource();
        refreshSnapshotCards();
        refreshTopContext();
        refreshExecutionPresentation();
        if (logSelection) {
            appendSystemLog(I18N.text("message.snapshot.input_saved", shortSnapshotId(snapshot)));
        }
    }

    @FXML
    private void useCurrentStructureInput() {
        applyCurrentStructureAlgorithmInput(true);
    }

    @FXML
    private void useLatestSnapshotInput() {
        if (activeDefinition == null || currentSubController == null || currentSubController.isRunning()) {
            return;
        }
        StructureSnapshot<?> snapshot = selectedSavedSnapshot(true);
        if (snapshot == null) {
            refreshAlgorithmInputSource();
            return;
        }
        applySavedSnapshotAlgorithmInput(snapshot, true);
    }

    private void syncSnapshotSelectionFromAlgorithmInput() {
        if (activeDefinition == null) {
            return;
        }
        SnapshotAlgorithmInputSupport<?> support = currentAlgorithmInputSupport();
        if (support == null) {
            selectedSnapshotIds.remove(activeDefinition.id());
            return;
        }
        String snapshotId = support.algorithmInputSnapshotId();
        if (snapshotId == null) {
            selectedSnapshotIds.remove(activeDefinition.id());
        } else {
            selectedSnapshotIds.put(activeDefinition.id(), snapshotId);
        }
    }

    private void refreshAlgorithmInputSource() {
        if (algorithmInputSourceLabel == null) {
            return;
        }
        SnapshotAlgorithmInputSupport<?> support = currentAlgorithmInputSupport();
        boolean hasSaved = activeDefinition != null && !structureSnapshotStore.snapshots(activeDefinition.id()).isEmpty();
        if (savedInputBtn != null) savedInputBtn.setDisable(!hasSaved || support == null);
        if (currentInputBtn != null) currentInputBtn.setDisable(support == null);
        if (support == null) {
            algorithmInputSourceLabel.setText(I18N.text("label.workspace.algorithm.input.parameters"));
            if (currentInputBtn != null) { currentInputBtn.pseudoClassStateChanged(SELECTED, false); currentInputBtn.setText(inputSourceButtonText("label.workspace.algorithm.input.current_button", false)); }
            if (savedInputBtn != null) { savedInputBtn.pseudoClassStateChanged(SELECTED, false); savedInputBtn.setText(inputSourceButtonText("label.workspace.algorithm.input.saved_button", false)); }
            return;
        }
        String snapshotId = support.algorithmInputSnapshotId();
        boolean current = snapshotId == null;
        if (currentInputBtn != null) {
            currentInputBtn.pseudoClassStateChanged(SELECTED, current);
            if (current) {
                currentInputBtn.setText(inputSourceButtonText("label.workspace.algorithm.input.current_button", true));
            } else {
                currentInputBtn.setText(inputSourceButtonText("label.workspace.algorithm.input.current_button", false));
            }
        }
        if (savedInputBtn != null) {
            savedInputBtn.pseudoClassStateChanged(SELECTED, !current);
            if (current) {
                savedInputBtn.setText(inputSourceButtonText("label.workspace.algorithm.input.saved_button", false));
            } else {
                savedInputBtn.setText(inputSourceButtonText("label.workspace.algorithm.input.saved_button", true));
            }
        }
        if (current) {
            algorithmInputSourceLabel.setText(I18N.text("label.workspace.algorithm.input.current_snapshot"));
            return;
        }
        StructureSnapshot<?> selected = structureSnapshotStore.snapshots(activeDefinition.id()).stream()
                .filter(snapshot -> snapshot.id().equals(snapshotId)).findFirst().orElse(null);
        String detail;
        if (selected == null) {
            detail = I18N.text("label.workspace.algorithm.input.saved_snapshot") + " / " + shortSnapshotId(snapshotId);
        } else {
            detail = I18N.text("label.workspace.algorithm.input.saved_snapshot") + " / " + shortSnapshotId(snapshotId) + "\n" + formatSnapshotTime(selected);
        }
        algorithmInputSourceLabel.setText(detail);
    }

    private String inputSourceButtonText(String key, boolean selected) {
        String prefix = "○ ";
        if (selected) {
            prefix = "● ";
        }
        return prefix + I18N.text(key).toUpperCase(Locale.ROOT);
    }

    private String shortSnapshotId(String snapshotId) {
        if (snapshotId == null || snapshotId.isBlank()) {
            return "-";
        }
        if (snapshotId.length() <= 8) {
            return snapshotId;
        } else {
            return snapshotId.substring(0, 8);
        }
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

    @SuppressWarnings("unchecked")
    private void previewSnapshotUnchecked(
            StructureSnapshotSupport<?> support,
            StructureSnapshot<?> snapshot) {
        StructureSnapshotSupport<Object> typedSupport =
                (StructureSnapshotSupport<Object>) support;
        StructureSnapshot<Object> typedSnapshot =
                (StructureSnapshot<Object>) snapshot;
        typedSupport.previewStructureSnapshot(typedSnapshot);
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
        selectedSnapshotIds.put(snapshot.moduleId(), snapshot.id());
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
        if (!confirmSnapshotRestore(snapshot)) {
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
        if (isStructurePageVisible()) {
            structureSnapshotPreviewActive = false;
            selectedSnapshotIds.remove(activeDefinition.id());
        }
        refreshSnapshotCards();
        refreshAlgorithmInputSource();
        refreshStructureSummary();
        clearStructureSelection();
        updateSnapshotActionState();
        updateWorkspaceInteractionState();
        refreshTopContext();
        appendSystemLog(I18N.text("message.snapshot.restored", shortSnapshotId(snapshot)));
    }

    private boolean confirmSnapshotRestore(StructureSnapshot<?> snapshot) {
        ButtonType cancel = new ButtonType(I18N.text("action.common.cancel"));
        ButtonType restore = new ButtonType(I18N.text("action.workspace.restore_snapshot"));
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "", cancel, restore);
        alert.setTitle(I18N.text("confirm.snapshot.restore.title"));
        alert.setHeaderText(I18N.text("confirm.snapshot.restore.header", shortSnapshotId(snapshot)));
        alert.setContentText(I18N.text("confirm.snapshot.restore.content"));
        OperationDialogTheme.apply(alert, 460.0d);
        return alert.showAndWait().filter(restore::equals).isPresent();
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
                        || currentSnapshotSupport() == null
                        || structureSnapshotPreviewActive);
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
        if (isPracticePageVisible()) {
            topContextLabel.setText(
                    I18N.text("label.workspace.practice").toUpperCase(Locale.ROOT) + " / "
                            + I18N.text("label.workspace.practice.catalog").toUpperCase(Locale.ROOT));
            if (runStateLabel != null) {
                runStateLabel.setText(workspaceStatusText("READY"));
                runStateLabel.getStyleClass().removeAll(
                        "state-running", "state-paused", "state-completed", "state-failed");
            }
            if (runIdLabel != null) {
                runIdLabel.setText("");
            }
            return;
        }
        String family;
        if (activeDefinition == null) {
            family = I18N.text("label.menu.title").toUpperCase(Locale.ROOT);
        } else {
            family = activeDefinition.name().toUpperCase(Locale.ROOT);
        }
        boolean structureMode = isStructurePageVisible();
        String suffix;
        if (structureMode) {
            suffix = I18N.text("label.workspace.structure.editor").toUpperCase(Locale.ROOT);
        } else {
            suffix = algorithmContextName();
        }
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
            String state;
            if (structureMode && structureSnapshotPreviewActive) {
                state = "PREVIEW";
            } else if (structureMode) {
                state = "EDITING";
            } else {
                state = "IDLE";
            }
            if (!structureMode) {
                if (currentSubController != null && currentSubController.isRunning()) {
                    if (currentSubController.isPaused()) {
                        state = "PAUSED";
                    } else {
                        state = "RUNNING";
                    }
                } else if (currentSubController != null && currentSubController.hasExecutionRecord()) {
                    state = currentSubController.latestExecutionStatus();
                } else {
                    state = "IDLE";
                }
            }
            runStateLabel.setText(workspaceStatusText(state));
            runStateLabel.getStyleClass().removeAll("state-running", "state-paused", "state-completed", "state-failed");
            if ("RUNNING".equals(state)) runStateLabel.getStyleClass().add("state-running");
            if ("PAUSED".equals(state)) runStateLabel.getStyleClass().add("state-paused");
            if ("COMPLETED".equals(state)) runStateLabel.getStyleClass().add("state-completed");
            if ("FAILED".equals(state) || "CANCELLED".equals(state)) runStateLabel.getStyleClass().add("state-failed");
        }
        if (runIdLabel != null) {
            String id;
            if (currentSubController == null) {
                id = null;
            } else {
                id = currentSubController.latestRunId();
            }
            if (id == null) {
                runIdLabel.setText("");
            } else {
                runIdLabel.setText(I18N.text("label.workspace.run.id", shortRunId(id)));
            }
        }
    }

    private String algorithmContextName() {
        if (selectedAlgorithmId == null || selectedAlgorithmId.isBlank()) {
            return I18N.text("label.workspace.algorithm").toUpperCase(Locale.ROOT);
        }
        return AlgorithmCatalog.name(selectedAlgorithmId).toUpperCase(Locale.ROOT);
    }

    private String workspaceStatusText(String state) {
        return I18N.text("status.workspace." + state.toLowerCase(Locale.ROOT));
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
        StructureSnapshot<?> previewSnapshot = null;
        StructureSnapshotSupport<?> snapshotSupport = null;
        if (structureSnapshotPreviewActive && activeDefinition != null) {
            previewSnapshot = selectedSavedSnapshot(false);
            snapshotSupport = currentSnapshotSupport();
            if (previewSnapshot == null || snapshotSupport == null) {
                structureSnapshotPreviewActive = false;
            }
        }
        if (structureSnapshotPreviewActive && previewSnapshot != null && snapshotSupport != null) {
            String primary = snapshotPrimaryCount(snapshotSupport, previewSnapshot);
            String secondary = snapshotSecondaryCount(snapshotSupport, previewSnapshot);
            if (structureOverviewLabel != null) {
                structureOverviewLabel.setText(describeSnapshot(snapshotSupport, previewSnapshot));
            }
            if (structureNodeCountLabel != null) {
                structureNodeCountLabel.setText(primary);
            }
            if (structureHeightLabel != null) {
                structureHeightLabel.setText(secondary);
            }
            if (structureStateLabel != null) {
                structureStateLabel.setText(workspaceStatusText("PREVIEW"));
            }
            if (overviewPrimaryValue != null) {
                overviewPrimaryValue.setText(primary);
            }
            if (overviewSecondaryValue != null) {
                overviewSecondaryValue.setText(secondary);
            }
            if (overviewEventsValue != null) {
                overviewEventsValue.setText("—");
            }
            if (overviewStateValue != null) {
                overviewStateValue.setText(workspaceStatusText("PREVIEW"));
            }
            return;
        }
        String summary = currentSubController.structureSummaryText();
        if (structureOverviewLabel != null) {
            if (summary == null || summary.isBlank()) {
                structureOverviewLabel.setText(workspaceStatusText("READY"));
            } else {
                structureOverviewLabel.setText(summary);
            }
        }
        if (structureNodeCountLabel != null) {
            structureNodeCountLabel.setText(currentSubController.structurePrimaryCount());
        }
        if (structureHeightLabel != null) {
            structureHeightLabel.setText(currentSubController.structureSecondaryCount());
        }
        if (structureStateLabel != null) {
            structureStateLabel.setText(workspaceStatusText("READY"));
        }
        if (overviewPrimaryValue != null) {
            overviewPrimaryValue.setText(currentSubController.structurePrimaryCount());
        }
        if (overviewSecondaryValue != null) {
            overviewSecondaryValue.setText(currentSubController.structureSecondaryCount());
        }
        if (overviewEventsValue != null) {
            overviewEventsValue.setText(Integer.toString(currentSubController.structureEvents().size()));
        }
        if (overviewStateValue != null) {
            overviewStateValue.setText(workspaceStatusText("READY"));
        }
    }

    @SuppressWarnings("unchecked")
    private String snapshotPrimaryCount(
            StructureSnapshotSupport<?> support,
            StructureSnapshot<?> snapshot) {
        StructureSnapshotSupport<Object> typedSupport =
                (StructureSnapshotSupport<Object>) support;
        StructureSnapshot<Object> typedSnapshot =
                (StructureSnapshot<Object>) snapshot;
        return typedSupport.snapshotPrimaryCount(typedSnapshot.state());
    }

    @SuppressWarnings("unchecked")
    private String snapshotSecondaryCount(
            StructureSnapshotSupport<?> support,
            StructureSnapshot<?> snapshot) {
        StructureSnapshotSupport<Object> typedSupport =
                (StructureSnapshotSupport<Object>) support;
        StructureSnapshot<Object> typedSnapshot =
                (StructureSnapshot<Object>) snapshot;
        return typedSupport.snapshotSecondaryCount(typedSnapshot.state());
    }

    private void wireAlgorithmSelection() {
        if (currentSubController instanceof AlgorithmSelectionSupport support) {
            support.setAlgorithmSelectionListener(this::handleAlgorithmSelectionChanged);
        }
    }

    private void wireStructureSelection() {
        if (currentSubController instanceof TreeController treeController) {
            treeController.setSelectionListener(this::showTreeSelection);
            treeController.setStructureSelectionEnabled(isStructurePageVisible());
        }
        if (currentSubController instanceof ArrayController arrayController) {
            arrayController.setSelectionListener(this::showArraySelection);
            arrayController.setStructureSelectionEnabled(isStructurePageVisible());
        }
        if (currentSubController instanceof StringController stringController) {
            stringController.setSelectionListener(this::showStringSelection);
            stringController.setStructureSelectionEnabled(isStructurePageVisible());
        }
        if (currentSubController instanceof GraphController graphController) {
            graphController.setSelectionListener(this::showGraphSelection);
            graphController.setStructureSelectionEnabled(isStructurePageVisible());
        }
        if (currentSubController instanceof MazeController mazeController) {
            mazeController.setSelectionListener(this::showMazeSelection);
            mazeController.setStructureSelectionEnabled(isStructurePageVisible());
        }
        if (currentSubController instanceof LinkedListController linkedController) {
            linkedController.setSelectionListener(this::showLinkedSelection);
            linkedController.setStructureSelectionEnabled(isStructurePageVisible());
        }
        if (currentSubController instanceof LinearStructureController linearController) {
            linearController.setSelectionListener(this::showLinearSelection);
            linearController.setStructureSelectionEnabled(isStructurePageVisible());
        }
    }

    private void showTreeSelection(TreeController.NodeSelection selection) {
        if (selection == null) {
            clearStructureSelection();
            return;
        }
        showStructureSelectionOverlay(
                I18N.text("label.workspace.selection.node"),
                "#" + selection.id(),
                selection.value().text(),
                I18N.text("label.workspace.selection.node.hint"));
        if (structureInspectorBody != null) {
            String parent;
            if (selection.parentId() == null) {
                parent = I18N.text("label.workspace.selection.none");
            } else {
                parent = "#" + selection.parentId();
            }
            structureInspectorBody.setText(I18N.text(
                    "label.workspace.selection.tree.detail",
                    selection.id(),
                    selection.value().text(),
                    parent,
                    selection.childCount(),
                    selection.depth()));
        }
    }

    private void showArraySelection(ArrayController.IndexSelection selection) {
        if (selection == null) {
            clearStructureSelection();
            return;
        }
        showStructureSelectionOverlay(
                I18N.text("label.workspace.selection.cell"),
                "[" + selection.index() + "]",
                selection.value().text(),
                I18N.text("label.workspace.selection.array.hint"));
        if (structureInspectorBody != null) {
            structureInspectorBody.setText(I18N.text(
                    "label.workspace.selection.array.detail",
                    selection.index(),
                    selection.value().text(),
                    selection.size()));
        }
    }

    private void showStringSelection(StringController.IndexSelection selection) {
        if (selection == null) {
            clearStructureSelection();
            return;
        }
        showStructureSelectionOverlay(
                I18N.text("label.workspace.selection.character"),
                "[" + selection.index() + "]",
                Character.toString(selection.value()),
                I18N.text("label.workspace.selection.string.hint"));
        if (structureInspectorBody != null) {
            structureInspectorBody.setText(I18N.text(
                    "label.workspace.selection.string.detail",
                    selection.index(),
                    Character.toString(selection.value()),
                    selection.length()));
        }
    }

    private void showGraphSelection(GraphController.Selection selection) {
        if (selection == null) {
            clearStructureSelection();
            return;
        }
        if (selection instanceof GraphController.NodeSelection node) {
            showStructureSelectionOverlay(
                    I18N.text("label.workspace.selection.node"),
                    "#" + node.id(),
                    node.value().text(),
                    I18N.text("label.workspace.selection.graph.node.hint"));
            if (structureInspectorBody != null) {
                structureInspectorBody.setText(I18N.text(
                        "label.workspace.selection.graph.node.detail",
                        node.id(),
                        node.value().text(),
                        node.degree()));
            }
            return;
        }
        GraphController.EdgeSelection edge = (GraphController.EdgeSelection) selection;
        String relation;
        if (edge.directed()) {
            relation = " → ";
        } else {
            relation = " — ";
        }
        showStructureSelectionOverlay(
                I18N.text("label.workspace.selection.edge"),
                "E#" + edge.id(),
                edge.fromValue().text() + relation + edge.toValue().text(),
                I18N.text("label.workspace.selection.graph.edge.hint"));
        if (structureInspectorBody != null) {
            String directed;
            if (edge.directed()) {
                directed = I18N.text("label.workspace.selection.yes");
            } else {
                directed = I18N.text("label.workspace.selection.no");
            }
            structureInspectorBody.setText(I18N.text(
                    "label.workspace.selection.graph.edge.detail",
                    edge.id(),
                    edge.fromValue().text(),
                    edge.toValue().text(),
                    directed));
        }
    }

    private void showLinkedSelection(LinkedListController.NodeSelection selection) {
        if (selection == null) {
            clearStructureSelection();
            return;
        }
        showStructureSelectionOverlay(
                I18N.text("label.workspace.selection.node"),
                "#" + selection.id(),
                selection.value().text(),
                I18N.text("label.workspace.selection.linked.hint"));
        if (structureInspectorBody != null) {
            String previousText;
            if (selection.previousId() == null) {
                previousText = I18N.text("label.workspace.selection.none");
            } else {
                previousText = "#" + selection.previousId();
            }
            String nextText;
            if (selection.nextId() == null) {
                nextText = I18N.text("label.workspace.selection.none");
            } else {
                nextText = "#" + selection.nextId();
            }
            structureInspectorBody.setText(I18N.text(
                    "label.workspace.selection.linked.detail",
                    selection.id(),
                    selection.value().text(),
                    selection.index(),
                    previousText,
                    nextText,
                    selection.size()));
        }
    }

    private void showLinearSelection(LinearStructureController.ItemSelection selection) {
        if (selection == null) {
            clearStructureSelection();
            return;
        }
        String role = linearRoleText(selection.role());
        showStructureSelectionOverlay(
                I18N.text("label.workspace.selection.item"),
                "[" + selection.index() + "]",
                selection.value().text(),
                role);
        if (structureInspectorBody != null) {
            structureInspectorBody.setText(I18N.text(
                    "label.workspace.selection.linear.detail",
                    selection.index(),
                    selection.value().text(),
                    role,
                    selection.size()));
        }
    }

    private void showMazeSelection(MazeController.CellSelection selection) {
        if (selection == null) {
            clearStructureSelection();
            return;
        }
        String state = mazeCellStateText(selection.state());
        showStructureSelectionOverlay(
                I18N.text("label.workspace.selection.cell"),
                "[" + selection.row() + "," + selection.column() + "]",
                state,
                I18N.text("label.workspace.selection.maze.hint"));
        if (structureInspectorBody != null) {
            structureInspectorBody.setText(I18N.text(
                    "label.workspace.selection.maze.detail",
                    selection.row(),
                    selection.column(),
                    state));
        }
    }

    private String linearRoleText(String role) {
        if ("TOP".equals(role)) {
            return I18N.text("label.workspace.selection.role.top");
        }
        if ("FRONT / REAR".equals(role)) {
            return I18N.text("label.workspace.selection.role.front_rear");
        }
        if ("FRONT".equals(role)) {
            return I18N.text("label.workspace.selection.role.front");
        }
        if ("REAR".equals(role)) {
            return I18N.text("label.workspace.selection.role.rear");
        }
        return I18N.text("label.workspace.selection.role.item");
    }

    private String mazeCellStateText(String state) {
        if ("ENTRANCE".equals(state)) {
            return I18N.text("label.workspace.selection.maze.entrance");
        }
        if ("EXIT".equals(state)) {
            return I18N.text("label.workspace.selection.maze.exit");
        }
        if ("OPEN".equals(state)) {
            return I18N.text("label.workspace.selection.maze.open");
        }
        if ("WALL".equals(state)) {
            return I18N.text("label.workspace.selection.maze.wall");
        }
        return state;
    }

    private void showStructureSelectionOverlay(String title, String id, String value, String hint) {
        if (!isStructurePageVisible()) {
            showAlgorithmSelectionOverlay(title, id, value);
            return;
        }
        if (structureSelectionOverlay != null) {
            structureSelectionOverlay.setManaged(true);
            structureSelectionOverlay.setVisible(true);
        }
        if (algorithmSelectionOverlay != null) {
            algorithmSelectionOverlay.setManaged(false);
            algorithmSelectionOverlay.setVisible(false);
        }
        if (selectedEntityTitleLabel != null) selectedEntityTitleLabel.setText(title);
        if (selectedEntityHintLabel != null) selectedEntityHintLabel.setText(hint);
        if (selectedNodeIdLabel != null) selectedNodeIdLabel.setText(id);
        if (selectedNodeValueLabel != null) selectedNodeValueLabel.setText(value);
        updateVisualizationObstruction(currentStepOverlay != null && currentStepOverlay.isVisible());
    }

    private void showAlgorithmSelectionOverlay(String title, String id, String value) {
        if (algorithmSelectionOverlay != null) {
            algorithmSelectionOverlay.setManaged(true);
            algorithmSelectionOverlay.setVisible(true);
        }
        if (structureSelectionOverlay != null) {
            structureSelectionOverlay.setManaged(false);
            structureSelectionOverlay.setVisible(false);
        }
        if (algorithmSelectedEntityTitleLabel != null) algorithmSelectedEntityTitleLabel.setText(title);
        if (algorithmSelectedEntityHintLabel != null) {
            algorithmSelectedEntityHintLabel.setText(I18N.text("label.workspace.selection.algorithm.hint"));
        }
        if (algorithmSelectedNodeIdLabel != null) algorithmSelectedNodeIdLabel.setText(id);
        if (algorithmSelectedNodeValueLabel != null) algorithmSelectedNodeValueLabel.setText(value);
        updateVisualizationObstruction(currentStepOverlay != null && currentStepOverlay.isVisible());
    }

    private void clearStructureSelection() {
        if (structureSelectionOverlay != null) {
            structureSelectionOverlay.setManaged(false);
            structureSelectionOverlay.setVisible(false);
        }
        if (algorithmSelectionOverlay != null) {
            algorithmSelectionOverlay.setManaged(false);
            algorithmSelectionOverlay.setVisible(false);
        }
        if (structureInspectorBody != null) {
            structureInspectorBody.setText(I18N.text("label.workspace.selection.prompt"));
        }
        updateVisualizationObstruction(currentStepOverlay != null && currentStepOverlay.isVisible());
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
            updateVisualizationObstruction(false);
            if (eventKindLabel != null) eventKindLabel.setText(I18N.text("label.workspace.event.none"));
            if (eventDetailsLabel != null) eventDetailsLabel.setText(I18N.text("label.workspace.event.prompt"));
            if (eventKindDot != null) setEventDotClass("event-dot-idle");
            if (timelineCursorLabel != null) { timelineCursorLabel.setManaged(false); timelineCursorLabel.setVisible(false); }
        } else {
            if (currentStepOverlay != null) {
                currentStepOverlay.setManaged(true);
                currentStepOverlay.setVisible(true);
            }
            updateVisualizationObstruction(true);
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
            if (count == 0) {
                timelinePositionLabel.setText("#0000");
            } else {
                timelinePositionLabel.setText(String.format(Locale.ROOT, "#%04d / %04d", Math.max(0, index + 1), count));
            }
        }
        refreshRunSummary();
        rebuildTimelineMarkers();
        refreshStructureSummary();
    }

    private void updateVisualizationObstruction(boolean currentStepVisible) {
        if (currentSubController == null || currentSubController.getVisualizer() == null) {
            return;
        }
        double left;
        if (currentStepVisible) {
            left = 254.0d;
        } else {
            left = 0.0d;
        }
        double right;
        if (algorithmSelectionOverlay != null && algorithmSelectionOverlay.isVisible()) {
            right = 254.0d;
        } else {
            right = 0.0d;
        }
        currentSubController.getVisualizer().setViewportObstructionInsets(
                new javafx.geometry.Insets(0.0d, right, 0.0d, left));
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
        if (event instanceof ObservationEvent.PathTraced pathTraced) {
            return "TARGET  " + formatReference(pathTraced.ref());
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
        String left;
        if (previous == null) {
            left = "none";
        } else {
            left = "#" + previous;
        }
        String right;
        if (next == null) {
            right = "none";
        } else {
            right = "#" + next;
        }
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
        addMetricIfPresent(metrics, values, "nodesVisited", I18N.text("label.workspace.metric.nodes_visited"));
        addMetricIfPresent(metrics, values, "edgesExamined", I18N.text("label.workspace.metric.edges_examined"));
        addMetricIfPresent(metrics, values, "comparisons", I18N.text("label.workspace.metric.comparisons"));
        addMetricIfPresent(metrics, values, "writes", I18N.text("label.workspace.metric.writes"));
        addMetricIfPresent(metrics, values, "swaps", I18N.text("label.workspace.metric.swaps"));
        addMetricIfPresent(metrics, values, "matches", I18N.text("label.workspace.metric.matches"));
        addMetricIfPresent(metrics, values, "fallbacks", I18N.text("label.workspace.metric.fallbacks"));
        addMetricIfPresent(metrics, values, "backtracks", I18N.text("label.workspace.metric.backtracks"));
        if (metrics.size() < 3) {
            metrics.add(new MetricDisplay(
                    I18N.text("label.workspace.metric.domain_events"),
                    Long.toString(statistics.domainEventCount())));
        }
        if (metrics.size() < 3) {
            metrics.add(new MetricDisplay(
                    I18N.text("label.workspace.metric.total_events"),
                    Long.toString(statistics.totalEventCount())));
        }
        while (metrics.size() < 3) {
            metrics.add(new MetricDisplay(I18N.text("label.workspace.metric.events"), "0"));
        }
        List<MetricDisplay> result = new ArrayList<>(metrics.subList(0, 3));
        result.add(new MetricDisplay(
                I18N.text("label.workspace.metric.duration"),
                formatDuration(statistics.duration())));
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
        timelineCursorLabel.setText(String.format(Locale.ROOT, "#%04d  %s", current.sequence(), eventDisplayName(current)));
        timelineCursorLabel.setManaged(true);
        timelineCursorLabel.setVisible(true);
    }

    private record MetricDisplay(String title, String value) {}

    private void rebuildTimelineMarkers() {
        if (timelineMarkers == null || currentSubController == null) {
            return;
        }
        timelineMarkers.getChildren().clear();
        List<EventEnvelope> events = currentSubController.executionEvents();
        if (events.isEmpty()) {
            return;
        }

        double paneWidth;
        if (timelineMarkers.getWidth() > 0.0d) {
            paneWidth = timelineMarkers.getWidth();
        } else {
            paneWidth = 700.0d;
        }
        double horizontalInset = 8.0d;
        double usableWidth = Math.max(1.0d, paneWidth - horizontalInset * 2.0d);

        int currentIndex = currentSubController.presentationEventIndex();
        if (currentIndex >= 0 && currentIndex < events.size()) {
            Region cursor = new Region();
            cursor.getStyleClass().add("timeline-marker-cursor");
            cursor.setManaged(false);
            cursor.setMouseTransparent(true);
            double ratio = eventRatio(currentIndex, events.size());
            cursor.resizeRelocate(horizontalInset + ratio * usableWidth - 1.0d, 3.0d, 2.0d, 26.0d);
            timelineMarkers.getChildren().add(cursor);
        }

        List<TimelineMarkerGroup> groups = timelineMarkerGroups(events, currentIndex, usableWidth);
        for (TimelineMarkerGroup group : groups) {
            int representativeIndex = group.representativeIndex();
            EventEnvelope envelope = events.get(representativeIndex);
            VBox marker = new VBox(1.0d);
            marker.setAlignment(javafx.geometry.Pos.CENTER);
            marker.setPrefWidth(24.0d);
            marker.setMinWidth(24.0d);
            marker.setMaxWidth(24.0d);
            marker.setPrefHeight(28.0d);
            marker.setMinHeight(28.0d);
            marker.setManaged(false);
            marker.setFocusTraversable(true);
            marker.getStyleClass().add("timeline-marker-node");
            if (group.eventIndexes().size() > 1) {
                marker.getStyleClass().add("timeline-marker-aggregate");
            }

            Region symbol = new Region();
            symbol.getStyleClass().addAll("timeline-marker-symbol", eventMarkerClass(envelope));
            if (group.eventIndexes().contains(currentIndex)) {
                symbol.getStyleClass().add("timeline-marker-current");
            }
            marker.getChildren().setAll(symbol);
            if (group.eventIndexes().size() > 1) {
                Label aggregate = new Label("×" + group.eventIndexes().size());
                aggregate.getStyleClass().add("timeline-marker-sequence");
                if (group.eventIndexes().contains(currentIndex)) {
                    aggregate.getStyleClass().add("timeline-marker-sequence-current");
                }
                marker.getChildren().add(aggregate);
            }

            double ratio = eventRatio(representativeIndex, events.size());
            marker.relocate(horizontalInset + ratio * usableWidth - 12.0d, 2.0d);
            String tooltipText = timelineMarkerTooltip(events, group);
            Tooltip.install(marker, new Tooltip(tooltipText));
            marker.setAccessibleText(tooltipText);
            marker.setOnMouseClicked(event -> {
                jumpToTimelineMarkerGroup(group);
                event.consume();
            });
            marker.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
                    jumpToTimelineMarkerGroup(group);
                    event.consume();
                }
            });
            timelineMarkers.getChildren().add(marker);
        }
    }

    private void jumpToTimelineMarkerGroup(TimelineMarkerGroup group) {
        if (currentSubController == null || currentSubController.isRunning()) {
            return;
        }
        int currentIndex = currentSubController.presentationEventIndex();
        int targetIndex = group.eventIndexes().getFirst();
        if (group.eventIndexes().size() > 1) {
            for (int eventIndex : group.eventIndexes()) {
                if (eventIndex > currentIndex) {
                    targetIndex = eventIndex;
                    break;
                }
            }
        }
        currentSubController.seekEventIndex(targetIndex);
        refreshExecutionPresentation();
    }

    private List<TimelineMarkerGroup> timelineMarkerGroups(
            List<EventEnvelope> events,
            int currentIndex,
            double usableWidth) {
        List<Integer> candidates = new ArrayList<>();
        for (int index = 0; index < events.size(); index++) {
            if (isTimelineMarkerVisible(events.get(index))) {
                candidates.add(index);
            }
        }
        if (currentIndex >= 0 && currentIndex < events.size() && !candidates.contains(currentIndex)) {
            candidates.add(currentIndex);
            candidates.sort(Integer::compareTo);
        }
        if (candidates.isEmpty()) {
            return List.of();
        }

        int markerBudget = (int) Math.floor(usableWidth / 30.0d);
        markerBudget = Math.max(13, Math.min(48, markerBudget));
        if (candidates.size() <= markerBudget) {
            List<TimelineMarkerGroup> groups = new ArrayList<>(candidates.size());
            for (int index : candidates) {
                groups.add(new TimelineMarkerGroup(index, List.of(index)));
            }
            return List.copyOf(groups);
        }

        Map<Integer, List<Integer>> buckets = new LinkedHashMap<>();
        for (int eventIndex : candidates) {
            double ratio = eventRatio(eventIndex, events.size());
            int bucket = (int) Math.floor(ratio * (markerBudget - 1));
            buckets.computeIfAbsent(bucket, ignored -> new ArrayList<>()).add(eventIndex);
        }
        List<TimelineMarkerGroup> groups = new ArrayList<>(buckets.size());
        for (List<Integer> eventIndexes : buckets.values()) {
            int representative = representativeTimelineEvent(events, eventIndexes, currentIndex);
            groups.add(new TimelineMarkerGroup(representative, List.copyOf(eventIndexes)));
        }
        groups.sort((left, right) -> Integer.compare(left.representativeIndex(), right.representativeIndex()));
        return List.copyOf(groups);
    }

    private int representativeTimelineEvent(
            List<EventEnvelope> events,
            List<Integer> eventIndexes,
            int currentIndex) {
        if (eventIndexes.contains(currentIndex)) {
            return currentIndex;
        }
        for (int eventIndex : eventIndexes) {
            if (events.get(eventIndex).event() instanceof ExecutionLifecycleEvent) {
                return eventIndex;
            }
        }
        for (int eventIndex : eventIndexes) {
            if (events.get(eventIndex).event() instanceof com.majortom.algorithms.core.event.structure.StructureEvent) {
                return eventIndex;
            }
        }
        return eventIndexes.get(eventIndexes.size() / 2);
    }

    private boolean isTimelineMarkerVisible(EventEnvelope envelope) {
        if (envelope.event() instanceof ExecutionLifecycleEvent) {
            return timelineRuntimeVisible;
        }
        if (envelope.event() instanceof com.majortom.algorithms.core.event.structure.StructureEvent) {
            return timelineStructureVisible;
        }
        if (envelope.event() instanceof ObservationEvent) {
            return timelineObservationVisible;
        }
        return false;
    }

    private double eventRatio(int eventIndex, int eventCount) {
        if (eventCount <= 1) {
            return 0.0d;
        }
        return eventIndex / (double) (eventCount - 1);
    }

    private String timelineMarkerTooltip(List<EventEnvelope> events, TimelineMarkerGroup group) {
        EventEnvelope envelope = events.get(group.representativeIndex());
        String base = String.format(Locale.ROOT, "#%04d  %s", envelope.sequence(), eventDisplayName(envelope));
        if (group.eventIndexes().size() <= 1) {
            return base;
        }
        return base + "  ·  ×" + group.eventIndexes().size();
    }

    private record TimelineMarkerGroup(int representativeIndex, List<Integer> eventIndexes) {
    }

    private enum TimelineMarkerCategory {
        RUNTIME,
        STRUCTURE,
        OBSERVATION
    }

    private String eventMarkerClass(EventEnvelope envelope) {
        if (envelope.event() instanceof ExecutionLifecycleEvent) return "timeline-runtime";
        if (envelope.event() instanceof com.majortom.algorithms.core.event.structure.StructureEvent) return "timeline-structure";
        if (envelope.event() instanceof ObservationEvent) return "timeline-observation";
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
