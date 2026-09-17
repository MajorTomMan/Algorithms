package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.visualization.render.fx.FxDispatch;

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
import com.majortom.algorithms.visualization.layout.PlaybackToolbar;
import com.majortom.algorithms.visualization.layout.WorkbenchHeader;
import com.majortom.algorithms.visualization.layout.WorkbenchUiFramework;
import com.majortom.algorithms.visualization.navigation.FamilyEntry;
import com.majortom.algorithms.visualization.navigation.FamilyNavigator;
import com.majortom.algorithms.visualization.structure.InMemoryStructureSnapshotStore;
import com.majortom.algorithms.core.domain.execution.ExecutionLifecycleEvent;
import com.majortom.algorithms.core.logging.LogEvent;
import com.majortom.algorithms.core.metadata.StructureModule;
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
import com.majortom.algorithms.visualization.render.api.ContentStyleSnapshot;
import com.majortom.algorithms.visualization.render.runtime.RenderContext;
import com.majortom.algorithms.visualization.render.runtime.RenderRuntime;
import com.majortom.algorithms.visualization.settings.FontSettings;
import com.majortom.algorithms.visualization.settings.FontSettingsService;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
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
import javafx.scene.layout.FlowPane;
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

    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
    private static final PseudoClass WORKSPACE_FOCUS = PseudoClass.getPseudoClass("workspace-focus");
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
    private WorkbenchHeader topBar;
    @FXML
    private Button structureWorkspaceBtn;
    @FXML
    private Button algorithmWorkspaceBtn;
    @FXML
    private Button practiceWorkspaceBtn;
    @FXML
    private VBox valueTypeBox;
    @FXML
    private VBox structureKindHost;
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
    private FamilyNavigator familyNavigator;
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
    private VBox practiceControlRail;
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
    private PlaybackToolbar playbackToolbar;
    @FXML
    private HBox timelineRow;
    @FXML
    private Label menuTitleLabel;
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
    private TabPane structureInspectorTabs;
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
    private Pane timelineMarkers;
    @FXML
    private FlowPane timelineDetails;
    @FXML
    private VBox timelineDetailPanel;
    @FXML
    private Region timelineStatusDot;
    @FXML
    private Label timelineStatusLabel;
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
    private Button jumpStartBtn;
    @FXML
    private Button endExecutionBtn;
    @FXML
    private Button closeExecutionBtn;
    @FXML
    private Button jumpEndBtn;
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

    private final RenderContext renderContext = RenderRuntime.context();
    private final List<WorkbenchModuleDefinition> moduleDefinitions = WorkbenchModules.available(COMPONENTS, renderContext);
    private final InMemoryStructureSnapshotStore structureSnapshotStore =
            new InMemoryStructureSnapshotStore();
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
    private WorkspaceMode workspaceMode = WorkspaceMode.STRUCTURE;
    private boolean moduleTransitionInProgress;
    private WorkbenchUiFramework uiFramework;
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
        setupPlaybackShellActions();
        setupTimelinePresentation();
        setupGlobalEffects();
        setupLayoutClips();
        setStructureHistoryExpanded(false);
        WorkbenchTheme.apply(rootPane);
        WorkbenchTheme.leftPill(structureWorkspaceBtn);
        WorkbenchTheme.rightPill(practiceWorkspaceBtn);
        setupUiFramework();

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
        stepBackwardBtn.setText("‹");
        stepBackwardBtn.accessibleTextProperty().bind(
                I18N.createStringBinding("action.execution.step.backward"));
        stepForwardBtn.setText("›");
        stepForwardBtn.accessibleTextProperty().bind(
                I18N.createStringBinding("action.execution.step.forward"));
        jumpStartBtn.accessibleTextProperty().bind(
                I18N.createStringBinding("action.execution.jump.start"));
        jumpEndBtn.accessibleTextProperty().bind(
                I18N.createStringBinding("action.execution.jump.end"));
        endExecutionBtn.textProperty().bind(I18N.createStringBinding("action.execution.end"));
        closeExecutionBtn.textProperty().bind(I18N.createStringBinding("action.execution.close"));
        localeListener = (observable, oldValue, newValue) -> {
            if (fontSettingsPopup != null && fontSettingsPopup.isShowing()) {
                fontSettingsPopup.hide();
            }
            refreshPauseText();
            refreshTimelineStatus();
            refreshWorkspaceContext();
            refreshTopContext();
            refreshAlgorithmInputSource();
            refreshSnapshotPreviewPresentation();
            refreshValueTypeSelectors();
            refreshStructureSummary();
            refreshExecutionPresentation();
            if (uiFramework != null) uiFramework.scheduleRefresh();
            boolean selectionVisible = structureSelectionOverlay != null && structureSelectionOverlay.isVisible();
            if (algorithmSelectionOverlay != null && algorithmSelectionOverlay.isVisible()) {
                selectionVisible = true;
            }
            if (selectionVisible && selectionPresentation != null) {
                selectionPresentation.run();
            } else if (!selectionVisible) {
                clearStructureSelection();
            }
        };
        I18N.localeProperty().addListener(new javafx.beans.value.WeakChangeListener<>(localeListener));
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
        publishContentStyle(appliedFontSettings);
    }

    private void publishContentStyle(FontSettings settings) {
        if (settings == null) {
            return;
        }
        RenderRuntime.shared().updateContentStyle(new ContentStyleSnapshot(
                settings.size(), settings.chineseFamily(), settings.englishFamily()));
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

        VBox popupShell = new VBox();
        popupShell.setAlignment(javafx.geometry.Pos.TOP_RIGHT);
        popupShell.getStyleClass().add("font-settings-popup-shell");
        popupShell.getStylesheets().addAll(rootPane.getStylesheets());

        Region arrow = new Region();
        arrow.getStyleClass().add("font-settings-arrow");
        HBox arrowRow = new HBox(arrow);
        arrowRow.getStyleClass().add("font-settings-arrow-row");

        VBox content = new VBox();
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

        String projectDefault = I18N.text("settings.text.fontDefault");
        List<String> families = new ArrayList<>();
        families.add(projectDefault);
        families.addAll(FONT_SETTINGS_SERVICE.availableFamilies());

        Label chineseFamilyLabel = new Label();
        chineseFamilyLabel.textProperty().bind(I18N.createStringBinding("settings.text.fontChinese"));
        chineseFamilyLabel.getStyleClass().add("font-settings-row-label");
        ComboBox<String> chineseFamilySelector = new ComboBox<>();
        chineseFamilySelector.setMaxWidth(Double.MAX_VALUE);
        chineseFamilySelector.getStyleClass().add("font-settings-combo");
        chineseFamilySelector.getItems().setAll(families);
        chineseFamilySelector.getSelectionModel().select(
                initial.chineseFamily().isBlank() ? projectDefault : initial.chineseFamily());
        HBox chineseFamilyRow = fontSettingsRow(chineseFamilyLabel, chineseFamilySelector);

        Label englishFamilyLabel = new Label();
        englishFamilyLabel.textProperty().bind(I18N.createStringBinding("settings.text.fontEnglish"));
        englishFamilyLabel.getStyleClass().add("font-settings-row-label");
        ComboBox<String> englishFamilySelector = new ComboBox<>();
        englishFamilySelector.setMaxWidth(Double.MAX_VALUE);
        englishFamilySelector.getStyleClass().add("font-settings-combo");
        englishFamilySelector.getItems().setAll(families);
        englishFamilySelector.getSelectionModel().select(
                initial.englishFamily().isBlank() ? projectDefault : initial.englishFamily());
        HBox englishFamilyRow = fontSettingsRow(englishFamilyLabel, englishFamilySelector);

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
        HBox sizeControl = new HBox(decreaseSize, sizeValue, increaseSize);
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
        VBox preview = new VBox(previewPrimary, previewArray, previewStep);
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

        chineseFamilySelector.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (updatingControls[0] || newValue == null) {
                return;
            }
            String family = chineseFamilySelector.getSelectionModel().getSelectedIndex() == 0 ? "" : newValue;
            draft[0] = new FontSettings(
                    family, draft[0].englishFamily(), draft[0].size(), draft[0].color());
            refreshPreview.run();
        });
        englishFamilySelector.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (updatingControls[0] || newValue == null) {
                return;
            }
            String family = englishFamilySelector.getSelectionModel().getSelectedIndex() == 0 ? "" : newValue;
            draft[0] = new FontSettings(
                    draft[0].chineseFamily(), family, draft[0].size(), draft[0].color());
            refreshPreview.run();
        });
        decreaseSize.setOnAction(event -> {
            double size = FONT_SETTINGS_SERVICE.clampSize(draft[0].size() - 1.0d);
            draft[0] = new FontSettings(
                    draft[0].chineseFamily(), draft[0].englishFamily(), size, draft[0].color());
            sizeValue.setText(formatFontSize(size));
            refreshPreview.run();
        });
        increaseSize.setOnAction(event -> {
            double size = FONT_SETTINGS_SERVICE.clampSize(draft[0].size() + 1.0d);
            draft[0] = new FontSettings(
                    draft[0].chineseFamily(), draft[0].englishFamily(), size, draft[0].color());
            sizeValue.setText(formatFontSize(size));
            refreshPreview.run();
        });
        colorPicker.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (updatingControls[0] || newValue == null) {
                return;
            }
            draft[0] = new FontSettings(
                    draft[0].chineseFamily(),
                    draft[0].englishFamily(),
                    draft[0].size(),
                    FONT_SETTINGS_SERVICE.toCssColor(newValue));
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
        HBox footer = new HBox(reset, apply);
        footer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        footer.getStyleClass().add("font-settings-footer");

        reset.setOnAction(event -> {
            FontSettings defaults = FONT_SETTINGS_SERVICE.defaults();
            updatingControls[0] = true;
            chineseFamilySelector.getSelectionModel().select(projectDefault);
            englishFamilySelector.getSelectionModel().select(projectDefault);
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
            publishContentStyle(normalized);
            if (languageChanged) {
                I18N.setLocale(selectedLocale);
                appendSystemLog(I18N.text(
                        "message.system.language_switched",
                        selectedLocale.getDisplayLanguage(selectedLocale)));
            }
            rootPane.applyCss();
            rootPane.layout();
            refreshUiFramework();
            boolean timelineExpanded = timelineDetailPanel != null && timelineDetailPanel.isVisible();
            setTimelineExpanded(timelineExpanded);
            rootPane.requestLayout();
            FxDispatch.defer(() -> {
                if (rootPane == null) {
                    return;
                }
                rootPane.applyCss();
                rootPane.layout();
                refreshUiFramework();
            });
            popup.hide();
        });

        content.getChildren().setAll(
                title,
                languageRow,
                chineseFamilyRow,
                englishFamilyRow,
                sizeRow,
                colorRow,
                new Separator(),
                previewTitle,
                preview,
                new Separator(),
                footer);
        popupShell.getChildren().setAll(arrowRow, content);
        FONT_SETTINGS_SERVICE.apply(popupShell, initial);
        FONT_SETTINGS_SERVICE.applyPreview(preview, initial);
        WorkbenchTheme.apply(popupShell);
        popup.getContent().setAll(popupShell);
        return popup;
    }

    private HBox fontSettingsRow(Label label, Node control) {
        label.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        HBox.setHgrow(control, Priority.ALWAYS);
        HBox row = new HBox(label, control);
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

    private Runnable selectionPresentation;
    private javafx.beans.value.ChangeListener<Locale> localeListener;

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
            if (newValue.type().equals(selectedValueType(activeDefinition.id()))) return;
            if (!isStructurePageVisible() || currentSubController.isRunning()
                    || structureSnapshotPreviewActive || !confirmValueTypeChange(newValue.type())) {
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
                    setText(valueTypeDisplayName(item.type()));
                    setDisable(false);
                } else {
                    setText(valueTypeDisplayName(item.type()) + " · " + I18N.text("label.value_type.unavailable"));
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
        boolean maze = StructureIds.MAZE.equals(moduleId);
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

            valueTypeLabel.textProperty().unbind();
            String typeLabelKey = switch (moduleId) {
                case StructureIds.TREE -> "label.value_type.node";
                case StructureIds.GRAPH -> "label.value_type.vertex";
                default -> "label.value_type.element";
            };
            valueTypeLabel.setText(I18N.text(typeLabelKey));
            valueTypeSelector.setDisable(StructureIds.STRING.equals(moduleId) || !isStructurePageVisible()
                    || structureSnapshotPreviewActive
                    || (currentSubController != null && currentSubController.isRunning()));
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

    private String valueTypeDisplayName(String type) {
        return I18N.text("label.value_type." + type);
    }

    private boolean confirmValueTypeChange(String nextType) {
        if (currentSubController instanceof RuntimeValueTypeSupport support && !support.hasValues()) return true;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        if (rootPane.getScene() != null) alert.initOwner(rootPane.getScene().getWindow());
        alert.setTitle(I18N.text("dialog.value_type.title"));
        alert.setHeaderText(I18N.text("dialog.value_type.header", valueTypeDisplayName(nextType)));
        alert.setContentText(I18N.text("dialog.value_type.body"));
        ButtonType change = new ButtonType(I18N.text("dialog.value_type.confirm"),
                javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType(I18N.text("dialog.value_type.cancel"),
                javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(change, cancel);
        return alert.showAndWait().orElse(cancel) == change;
    }

    private void refreshHashTableTypeSelectors() {
        selectedHashKeyType = null;
        selectedHashValueType = null;
        valueTypeSelector.getItems().clear();
        hashValueTypeSelector.getItems().clear();
    }

    private List<String> availableValueTypes(String moduleId) {
        return switch (moduleId) {
            case StructureIds.ARRAY, StructureIds.LINKED_LIST, StructureIds.STACK, StructureIds.QUEUE, StructureIds.TREE, StructureIds.GRAPH -> ValueAdapters.supportedTypeNames();
            case StructureIds.STRING -> List.of(String.class.getSimpleName());
            default -> COMPONENTS.algorithmValueTypes(StructureModule.fromId(moduleId));
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
        refreshAlgorithmInputSource();
        refreshWorkspaceContext();
        refreshStructureSummary();
        rebuildAlgorithmMenu();
        updateAlgorithmWorkspaceAvailability(activeDefinition.id());
        List<AlgorithmNavigationItem> items = algorithmNavigationItems(activeDefinition.id());
        if (!items.isEmpty() && currentSubController instanceof AlgorithmSelectionSupport support) {
            support.selectAlgorithm(items.getFirst().id());
        }
    }

    private void setupModuleMenu() {
        familyNavigator.setEntries(moduleDefinitions.stream()
                .map(definition -> familyEntry(definition, false, () -> selectFamily(definition)))
                .toList());
        syncFamilyNavigatorSelection();
        updateFamilyNavigatorAvailability();
    }

    private void rebuildAlgorithmMenu() {
        updateFamilyNavigatorAvailability();
        syncFamilyNavigatorSelection();
    }

    private void selectFamily(WorkbenchModuleDefinition definition) {
        if (definition == null || workspaceMode == WorkspaceMode.PRACTICE) {
            return;
        }
        if (workspaceMode == WorkspaceMode.STRUCTURE) {
            switchToModule(definition);
            return;
        }
        List<AlgorithmNavigationItem> navigationItems = algorithmNavigationItems(definition.id());
        if (navigationItems.isEmpty()) {
            return;
        }
        String preferred = activeDefinition != null
                && activeDefinition.id().equals(definition.id())
                ? selectedAlgorithmId
                : null;
        String targetAlgorithm = navigationItems.stream()
                .map(AlgorithmNavigationItem::id)
                .filter(id -> java.util.Objects.equals(id, preferred))
                .findFirst()
                .orElse(navigationItems.getFirst().id());
        switchToModule(definition, WorkspaceMode.ALGORITHM, targetAlgorithm);
    }

    private FamilyEntry familyEntry(
            WorkbenchModuleDefinition definition,
            boolean disabled,
            Runnable action) {
        return new FamilyEntry(
                definition.id(),
                definition.navigation().glyph(),
                javafx.beans.binding.Bindings.createStringBinding(
                        () -> familyName(definition.id()),
                        I18N.localeProperty()),
                disabled,
                action);
    }

    private void syncFamilyNavigatorSelection() {
        String familyId = activeDefinition == null ? null : activeDefinition.id();
        familyNavigator.setSelectedFamily(familyId);
    }

    private String familyName(String moduleId) {
        return I18N.text("label.structure." + moduleId).toUpperCase(Locale.ROOT);
    }

    private String familyIndex(String moduleId) {
        for (int index = 0; index < moduleDefinitions.size(); index++) {
            if (moduleDefinitions.get(index).id().equals(moduleId)) {
                return "%02d".formatted(index + 1);
            }
        }
        return "--";
    }

    private void selectAlgorithm(WorkbenchModuleDefinition definition, String algorithmId) {
        if (activeDefinition == null || !activeDefinition.id().equals(definition.id())) {
            switchToModule(definition, WorkspaceMode.ALGORITHM, algorithmId);
            return;
        }
        List<AlgorithmNavigationItem> available = algorithmNavigationItems(definition.id());
        if (available.stream().noneMatch(item -> item.id().equals(algorithmId))) {
            return;
        }
        setWorkspaceMode(WorkspaceMode.ALGORITHM);
        if (currentSubController instanceof AlgorithmSelectionSupport support) {
            support.selectAlgorithm(algorithmId);
        }
    }

    /**
     * Algorithms exposed by a family in the workspace rail.
     *
     * <p>This deliberately describes the whole family, not only the currently active
     * structure variant.  A family such as Tree can start on General Tree while its
     * algorithms live on the AVL variant.  Using the active controller's list here
     * made the rail change availability during a module transition and could bounce
     * the workspace back to Structure before the target variant was selected.</p>
     */
    private List<AlgorithmNavigationItem> algorithmNavigationItems(String moduleId) {
        List<String> algorithmIds = new ArrayList<>();
        if (StructureIds.MAZE.equals(moduleId)) {
            algorithmIds.addAll(AlgorithmCatalog.forWorkbenchModule(moduleId));
        } else {
            String selected = selectedValueType(moduleId);
            if (selected == null) {
                return List.of();
            }
            Class<?> valueType = ValueAdapters.requireType(selected);
            algorithmIds.addAll(AlgorithmCatalog.forWorkbenchModule(moduleId, valueType));
            List<String> registered = COMPONENTS.algorithmIds(StructureModule.fromId(moduleId), valueType.getSimpleName());
            algorithmIds.removeIf(id -> !registered.contains(id));
        }
        return algorithmIds.stream().distinct().map(AlgorithmNavigationItem::new).toList();
    }

    private void addAlgorithmsForAllTypes(List<String> target, String family, String excludedPrefix) {
        for (String valueType : COMPONENTS.algorithmValueTypes(StructureModule.fromId(family))) {
            for (String algorithmId : COMPONENTS.algorithmIds(StructureModule.fromId(family), valueType)) {
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
        if (activeDefinition == null || currentSubController == null) {
            return;
        }
        List<AlgorithmNavigationItem> available = algorithmNavigationItems(activeDefinition.id());
        if (available.isEmpty() || !(currentSubController instanceof AlgorithmSelectionSupport support)) {
            return;
        }
        String targetAlgorithm = available.stream()
                .map(AlgorithmNavigationItem::id)
                .filter(id -> java.util.Objects.equals(id, selectedAlgorithmId))
                .findFirst()
                .orElse(available.getFirst().id());
        if (!support.selectAlgorithm(targetAlgorithm)) {
            return;
        }
        syncAlgorithmSelectionFromController();
        setWorkspaceMode(WorkspaceMode.ALGORITHM);
        refreshUiFramework();
    }

    private void setWorkspaceMode(boolean structure) {
        setWorkspaceMode(structure ? WorkspaceMode.STRUCTURE : WorkspaceMode.ALGORITHM);
    }

    @FXML
    private void selectPracticeWorkspace() {
        setWorkspaceMode(WorkspaceMode.PRACTICE);
    }

    private void setWorkspaceMode(WorkspaceMode mode) {
        workspaceMode = mode;
        boolean structure = mode == WorkspaceMode.STRUCTURE;
        boolean algorithm = mode == WorkspaceMode.ALGORITHM;
        boolean practice = mode == WorkspaceMode.PRACTICE;

        familyNavigator.setManaged(!practice);
        familyNavigator.setVisible(!practice);
        updateFamilyNavigatorAvailability();

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
        boolean expanded = timelineDetailPanel != null && !timelineDetailPanel.isVisible();
        setTimelineExpanded(expanded);
        if (expanded) {
            rebuildTimelineMarkers();
        }
    }

    private void setTimelineExpanded(boolean expanded) {
        if (timelineDetailPanel != null) {
            timelineDetailPanel.setManaged(expanded);
            timelineDetailPanel.setVisible(expanded);
        }
        if (bottomDock != null) {
            bottomDock.getStyleClass().removeAll("timeline-collapsed", "timeline-expanded");
            bottomDock.getStyleClass().add(expanded ? "timeline-expanded" : "timeline-collapsed");
        }
        if (timelineToggleBtn != null) {
            timelineToggleBtn.setText(expanded ? "▲" : "▼");
            timelineToggleBtn.setAccessibleText(I18N.text(expanded
                    ? "action.execution.timeline.collapse"
                    : "action.execution.timeline.expand"));
        }
        if (uiFramework != null) {
            uiFramework.scheduleRefresh();
        }
    }

    @FXML
    private void toggleStructureHistory() {
        setStructureHistoryExpanded(!structureHistoryExpanded);
    }

    private void setStructureHistoryExpanded(boolean expanded) {
        structureHistoryExpanded = expanded;
        if (structureHistoryToggleBtn != null) {
            structureHistoryToggleBtn.setText(expanded ? "▼" : "▲");
        }
        if (uiFramework != null) {
            uiFramework.setStructureHistoryExpanded(expanded);
        } else if (structureHistoryDetails != null) {
            structureHistoryDetails.setManaged(expanded);
            structureHistoryDetails.setVisible(expanded);
        }
    }

    private void setupGlobalEffects() {
        EffectUtils.applyDynamicEffect(
                structureWorkspaceBtn, algorithmWorkspaceBtn, fontSettingsBtn,
                startBtn, pauseBtn, endExecutionBtn, closeExecutionBtn,
                jumpStartBtn, stepBackwardBtn, stepForwardBtn, jumpEndBtn,
                resetBtn, replayBtn, exportBtn, compareBtn, saveSnapshotBtn,
                speed1Btn, speed2Btn, speed4Btn, speed8Btn, speed16Btn);
    }

    private void setupLayoutClips() {
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

    private void setupUiFramework() {
        uiFramework = new WorkbenchUiFramework(
                rootPane,
                topBar,
                familyNavigator,
                structureControlRail,
                algorithmControlRail,
                practiceControlRail,
                snapshotPanel,
                diagnosticsPanel,
                structureSelectionOverlay,
                currentStepOverlay,
                algorithmSelectionOverlay,
                structureInspectorTabs,
                algorithmInspectorTabs,
                playbackToolbar,
                structureHistoryDock,
                structureHistoryDetails);
        uiFramework.setStructureHistoryExpanded(structureHistoryExpanded);
        uiFramework.install();
        refreshUiFramework();
    }

    private void refreshUiFramework() {
        if (rootPane != null && appliedFontSettings != null) {
            FONT_SETTINGS_SERVICE.refreshScriptFonts(rootPane, appliedFontSettings);
        }
        if (uiFramework == null) {
            return;
        }
        WorkbenchUiFramework.LayoutState state = uiFramework.refresh();
        compactLayout = state.compact();
        narrowLayout = state.narrow();
    }

    private void setPageVisibility(VBox page, boolean visible) {
        if (page == null) {
            return;
        }
        page.setManaged(visible);
        page.setVisible(visible);
    }

    private void setControlVisibility(Node control, boolean visible) {
        if (control == null) {
            return;
        }
        control.setManaged(visible);
        control.setVisible(visible);
    }

    private void switchToModule(WorkbenchModuleDefinition definition) {
        switchToModule(definition, WorkspaceMode.STRUCTURE, null);
    }

    private void switchToModule(
            WorkbenchModuleDefinition definition,
            WorkspaceMode requestedMode,
            String preferredAlgorithmId) {
        // Prepare the next controller off-screen. Nothing below is allowed to reveal a visualizer
        // until the target workspace and algorithm selection are final.
        BaseController<?> nextController = null;
        HBox preparedControls = new HBox();
        try {
            nextController = definition.controllerFactory().get();
            configureRuntimeValueType(definition.id(), nextController);
            nextController.setupCustomControls(preparedControls);
        } catch (RuntimeException failure) {
            if (nextController != null) nextController.dispatchVisualizerDetached();
            Throwable cause = failure;
            while (cause.getCause() != null) cause = cause.getCause();
            appendSystemLog(I18N.text("message.error.module_load",
                    familyName(definition.id()), cause.toString()));
            return;
        }

        moduleTransitionInProgress = true;
        try {
            activeDefinition = definition;
            selectedAlgorithmId = null;
            structureSnapshotPreviewActive = false;
            clearStructureSelection();
            refreshValueTypeSelectors();
            loadSubController(nextController, preparedControls);
            rebuildAlgorithmMenu();

            WorkspaceMode finalMode = requestedMode;
            if (requestedMode == WorkspaceMode.ALGORITHM) {
                List<AlgorithmNavigationItem> available = algorithmNavigationItems(definition.id());
                if (available.isEmpty() || !(currentSubController instanceof AlgorithmSelectionSupport support)) {
                    finalMode = WorkspaceMode.STRUCTURE;
                } else {
                    String targetAlgorithm = available.stream()
                            .map(AlgorithmNavigationItem::id)
                            .filter(id -> java.util.Objects.equals(id, preferredAlgorithmId))
                            .findFirst()
                            .orElse(available.getFirst().id());
                    if (!support.selectAlgorithm(targetAlgorithm)) {
                        finalMode = WorkspaceMode.STRUCTURE;
                    }
                }
            }

            syncAlgorithmSelectionFromController();
            updateAlgorithmWorkspaceAvailability(definition.id());
            refreshWorkspaceContext();
            setWorkspaceMode(finalMode);
            currentSubController.dispatchVisualizerAttached();
            refreshPauseText();
            refreshTopContext();
            refreshExecutionPresentation();
            updateWorkspaceInteractionState();
            refreshUiFramework();
            syncFamilyNavigatorSelection();
        } finally {
            moduleTransitionInProgress = false;
        }
    }


    private void updateAlgorithmWorkspaceAvailability(String moduleId) {
        boolean available = !algorithmNavigationItems(moduleId).isEmpty();
        if (!available && !moduleTransitionInProgress && workspaceMode == WorkspaceMode.ALGORITHM) {
            setWorkspaceMode(WorkspaceMode.STRUCTURE);
        }
        refreshExecutionDockVisibility(isStructurePageVisible());
        updateWorkspaceInteractionState();
    }

    private void updateFamilyNavigatorAvailability() {
        if (familyNavigator == null) {
            return;
        }
        boolean running = currentSubController != null && currentSubController.isRunning();
        boolean algorithmMode = workspaceMode == WorkspaceMode.ALGORITHM;
        for (WorkbenchModuleDefinition definition : moduleDefinitions) {
            boolean unavailableInAlgorithm = algorithmMode
                    && algorithmNavigationItems(definition.id()).isEmpty();
            familyNavigator.setFamilyDisabled(definition.id(), running || unavailableInAlgorithm);
        }
    }

    private void updateWorkspaceInteractionState() {
        boolean running = currentSubController != null && currentSubController.isRunning();
        boolean algorithmAvailable = activeDefinition != null
                && !algorithmNavigationItems(activeDefinition.id()).isEmpty();
        structureWorkspaceBtn.setDisable(running);
        algorithmWorkspaceBtn.setDisable(running || !algorithmAvailable);
        practiceWorkspaceBtn.setDisable(running);
        updateFamilyNavigatorAvailability();
        if (structureControlsHost != null) {
            structureControlsHost.setDisable(running || structureSnapshotPreviewActive);
        }
        if (valueTypeBox != null) valueTypeBox.setDisable(running || structureSnapshotPreviewActive);
        if (structureKindHost != null) structureKindHost.setDisable(running || structureSnapshotPreviewActive);
        refreshValueTypeSelectors();
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
        syncFamilyNavigatorSelection();
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

    private void loadSubController(BaseController<?> newController, HBox preparedControls) {
        detachCurrentController();
        visualizationContainer.getChildren().clear();
        structureControlsHost.getChildren().clear();
        structureKindHost.getChildren().clear();
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
        List<Node> preparedNodes = List.copyOf(preparedControls.getChildren());
        preparedControls.getChildren().clear();
        customControlBox.getChildren().setAll(preparedNodes);
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

        // The caller decides the final workspace first, then attaches and reveals the visualizer.
        // This prevents an intermediate parent size/mode from triggering a visible first layout.
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
        // Parent allocates the viewport; content must not feed its previous size back into HBox.
        visualizer.setMinSize(0, 0);
        visualizer.setPrefSize(0, 0);
        visualizer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
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
            } else if (structureKindHost.getChildren().isEmpty()
                    && section.getStyleClass().contains("structure-section")
                    && !section.getStyleClass().contains("operation-section")) {
                target = structureKindHost;
            } else {
                target = structureControlsHost;
            }
            target.getChildren().add(section);
        }
        stretchControls(structureControlsHost);
        stretchControls(algorithmControlsHost);
        stretchControls(structureKindHost);
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
        String moduleName = familyName(activeDefinition.id());
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
        String moduleName = familyName(activeDefinition.id());
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
            algorithmInputSourceLabel.setText(I18N.text("label.workspace.algorithm.input.current_snapshot") + "\n" + inputValueTypeText());
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
        algorithmInputSourceLabel.setText(detail + "\n" + inputValueTypeText());
    }

    private String inputValueTypeText() {
        if (currentSubController instanceof RuntimeValueTypeSupport support) {
            return I18N.text("label.value_type.input", valueTypeDisplayName(support.runtimeValueType().getSimpleName()));
        }
        return "";
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
        OperationDialogTheme.apply(alert);
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
            case StructureIds.ARRAY, StructureIds.STACK -> "btn-ran-blue";
            case StructureIds.LINKED_LIST, StructureIds.TREE -> "btn-ran-gold";
            case StructureIds.QUEUE, StructureIds.GRAPH -> "btn-ran-white";
            case StructureIds.MAZE -> "btn-ran-red";
            case StructureIds.STRING -> "btn-ran-gold";
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

    private void setupPlaybackShellActions() {
        if (jumpStartBtn != null) {
            jumpStartBtn.setOnAction(event -> {
                if (currentSubController != null && currentSubController.jumpToStart()) {
                    refreshExecutionPresentation();
                }
            });
        }
        if (jumpEndBtn != null) {
            jumpEndBtn.setOnAction(event -> {
                if (currentSubController != null && currentSubController.jumpToEnd()) {
                    refreshExecutionPresentation();
                }
            });
        }
        if (endExecutionBtn != null) {
            endExecutionBtn.setOnAction(event -> {
                if (currentSubController != null) {
                    currentSubController.endAlgorithm();
                    refreshExecutionPresentation();
                }
            });
        }
        if (closeExecutionBtn != null) {
            closeExecutionBtn.setOnAction(event -> {
                if (currentSubController != null) {
                    currentSubController.closeExecution();
                    refreshExecutionPresentation();
                    refreshTopContext();
                }
            });
        }
        refreshPlaybackShellControls();
    }

    private void refreshPlaybackShellControls() {
        boolean available = currentSubController != null;
        boolean running = available && currentSubController.isRunning();
        boolean hasTimeline = available && currentSubController.hasExecutionData();
        if (jumpStartBtn != null) jumpStartBtn.setDisable(running || !hasTimeline);
        if (jumpEndBtn != null) jumpEndBtn.setDisable(running || !hasTimeline);
        if (endExecutionBtn != null) endExecutionBtn.setDisable(!running);
        if (closeExecutionBtn != null) closeExecutionBtn.setDisable(!running && !hasTimeline);
        refreshTimelineStatus();
    }

    private void refreshTimelineStatus() {
        if (timelineStatusLabel == null) {
            return;
        }
        boolean available = currentSubController != null;
        boolean running = available && currentSubController.isRunning();
        boolean replaying = available && currentSubController.isPlaybackPlaying();
        boolean paused = available && currentSubController.isPaused();
        boolean hasTimeline = available && currentSubController.hasExecutionData();

        String key;
        String tone;
        if (paused && (running || replaying || hasTimeline)) {
            key = "status.workspace.paused";
            tone = "timeline-status-paused";
        } else if (running) {
            key = "status.workspace.running";
            tone = "timeline-status-running";
        } else if (replaying) {
            key = "status.workspace.playing";
            tone = "timeline-status-playing";
        } else if (hasTimeline) {
            key = "status.workspace.completed";
            tone = "timeline-status-completed";
        } else {
            key = "status.workspace.ready";
            tone = "timeline-status-ready";
        }
        timelineStatusLabel.setText(I18N.text(key));
        if (timelineStatusDot != null) {
            timelineStatusDot.getStyleClass().removeAll(
                    "timeline-status-running",
                    "timeline-status-playing",
                    "timeline-status-paused",
                    "timeline-status-completed",
                    "timeline-status-ready");
            timelineStatusDot.getStyleClass().add(tone);
        }
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
        // Family identity is owned by FamilyNavigator and run state by the workspace.
        // The header deliberately contains no duplicate breadcrumb or execution badge.
    }

    private String workspaceStatusText(String state) {
        return I18N.text("status.workspace." + state.toLowerCase(Locale.ROOT));
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
        if (selection == null) { clearStructureSelection(); return; }
        selectionPresentation = () -> presentSelection(
                I18N.text("label.workspace.selection.node"), "#" + selection.id(),
                selection.value().text(), I18N.text("label.workspace.selection.node.hint"),
                I18N.text("label.workspace.selection.tree.detail", selection.id(), selection.value().text(), nodeIdText(selection.parentId()), selection.childCount(), selection.depth()));
        selectionPresentation.run();
    }

    private void showArraySelection(ArrayController.IndexSelection selection) {
        if (selection == null) { clearStructureSelection(); return; }
        selectionPresentation = () -> presentSelection(
                I18N.text("label.workspace.selection.cell"), "[" + selection.index() + "]",
                selection.value().text(), I18N.text("label.workspace.selection.array.hint"),
                I18N.text("label.workspace.selection.array.detail", selection.index(), selection.value().text(), selection.size()));
        selectionPresentation.run();
    }

    private void showStringSelection(StringController.IndexSelection selection) {
        if (selection == null) { clearStructureSelection(); return; }
        selectionPresentation = () -> presentSelection(
                I18N.text("label.workspace.selection.character"), "[" + selection.index() + "]",
                Character.toString(selection.value()), I18N.text("label.workspace.selection.string.hint"),
                I18N.text("label.workspace.selection.string.detail", selection.index(), Character.toString(selection.value()), selection.length()));
        selectionPresentation.run();
    }

    private void showLinkedSelection(LinkedListController.NodeSelection selection) {
        if (selection == null) { clearStructureSelection(); return; }
        selectionPresentation = () -> presentSelection(
                I18N.text("label.workspace.selection.node"), "#" + selection.id(),
                selection.value().text(), I18N.text("label.workspace.selection.linked.hint"),
                I18N.text("label.workspace.selection.linked.detail", selection.id(), selection.value().text(), selection.index(), nodeIdText(selection.previousId()), nodeIdText(selection.nextId()), selection.size()));
        selectionPresentation.run();
    }

    private void showLinearSelection(LinearStructureController.ItemSelection selection) {
        if (selection == null) { clearStructureSelection(); return; }
        selectionPresentation = () -> presentSelection(
                I18N.text("label.workspace.selection.item"), "[" + selection.index() + "]",
                selection.value().text(), linearRoleText(selection.role()),
                I18N.text("label.workspace.selection.linear.detail", selection.index(), selection.value().text(), linearRoleText(selection.role()), selection.size()));
        selectionPresentation.run();
    }

    private void showMazeSelection(MazeController.CellSelection selection) {
        if (selection == null) { clearStructureSelection(); return; }
        selectionPresentation = () -> presentSelection(
                I18N.text("label.workspace.selection.cell"), "[" + selection.row() + "," + selection.column() + "]",
                mazeCellStateText(selection.state()), I18N.text("label.workspace.selection.maze.hint"),
                I18N.text("label.workspace.selection.maze.detail", selection.row(), selection.column(), mazeCellStateText(selection.state())));
        selectionPresentation.run();
    }

    private void showGraphSelection(GraphController.Selection selection) {
        if (selection == null) { clearStructureSelection(); return; }
        selectionPresentation = () -> {
            if (selection instanceof GraphController.NodeSelection node) {
                presentSelection(I18N.text("label.workspace.selection.node"), "#" + node.id(),
                        node.value().text(), I18N.text("label.workspace.selection.graph.node.hint"),
                        I18N.text("label.workspace.selection.graph.node.detail", node.id(), node.value().text(), node.degree()));
            } else if (selection instanceof GraphController.EdgeSelection edge) {
                presentSelection(I18N.text("label.workspace.selection.edge"), "E#" + edge.id(),
                        edge.fromValue().text() + (edge.directed() ? " → " : " — ") + edge.toValue().text(),
                        I18N.text("label.workspace.selection.graph.edge.hint"),
                        I18N.text("label.workspace.selection.graph.edge.detail", edge.id(),
                                edge.fromValue().text(), edge.toValue().text(),
                                I18N.text(edge.directed() ? "label.workspace.selection.yes" : "label.workspace.selection.no")));
            }
        };
        selectionPresentation.run();
    }

    private String nodeIdText(Long id) {
        return id == null ? I18N.text("label.workspace.selection.none") : "#" + id;
    }

    /** All formatting completes before either view is changed, so a failed format cannot leave half a selection. */
    private void presentSelection(String title, String id, String value, String hint, String detail) {
        showStructureSelectionOverlay(title, id, value, hint);
        if (structureInspectorBody != null) structureInspectorBody.setText(detail);
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
        selectionPresentation = null;
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
            refreshPlaybackShellControls();
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
            if (timelineCursorLabel != null) {
                timelineCursorLabel.setText("");
                timelineCursorLabel.setVisible(false);
            }
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
        refreshRunSummary();
        rebuildTimelineMarkers();
        refreshStructureSummary();
        refreshPlaybackShellControls();
    }

    private void updateVisualizationObstruction(boolean currentStepVisible) {
        if (currentSubController == null || currentSubController.getVisualizer() == null) {
            return;
        }
        double left;
        if (currentStepVisible) {
            left = currentStepOverlay.getWidth() + 24.0d;
        } else {
            left = 0.0d;
        }
        double right;
        if (algorithmSelectionOverlay != null && algorithmSelectionOverlay.isVisible()) {
            right = algorithmSelectionOverlay.getWidth() + 24.0d;
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
        long millis = Math.max(0L, duration.toMillis());
        long hours = millis / 3_600_000L;
        long minutes = (millis % 3_600_000L) / 60_000L;
        long seconds = (millis % 60_000L) / 1_000L;
        long milliseconds = millis % 1_000L;
        return String.format(Locale.ROOT, "%02d:%02d:%02d.%03d", hours, minutes, seconds, milliseconds);
    }

    private void updateTimelineCursorCallout(EventEnvelope current) {
        if (timelineCursorLabel == null || current == null) {
            return;
        }
        timelineCursorLabel.setText(I18N.text("label.workspace.event.current") + ": " + eventDisplayName(current));
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

        double paneWidth = timelineMarkers.getWidth();
        if (!(paneWidth > 0.0d)) {
            paneWidth = timelineMarkers.prefWidth(-1.0d);
        }
        if (!(paneWidth > 0.0d) || !Double.isFinite(paneWidth)) {
            paneWidth = 320.0d;
        }
        double paneHeight = timelineMarkers.getHeight();
        if (!(paneHeight > 0.0d)) {
            paneHeight = timelineMarkers.prefHeight(paneWidth);
        }
        if (!(paneHeight > 0.0d) || !Double.isFinite(paneHeight)) {
            paneHeight = 36.0d;
        }

        double fontSize = timelineBaseFontSize();
        double symbolSize = Math.max(7.0d, Math.min(14.0d, fontSize * 0.52d));
        double horizontalInset = Math.max(symbolSize, fontSize * 0.62d);
        double usableWidth = Math.max(1.0d, paneWidth - horizontalInset * 2.0d);
        double trackCenterY = paneHeight / 2.0d;

        int currentIndex = currentSubController.presentationEventIndex();
        if (currentIndex >= 0 && currentIndex < events.size()) {
            Region cursor = new Region();
            cursor.getStyleClass().add("timeline-marker-cursor");
            cursor.setManaged(false);
            cursor.setMouseTransparent(true);
            double ratio = eventRatio(currentIndex, events.size());
            double cursorWidth = Math.max(2.0d, fontSize * 0.10d);
            double cursorHeight = Math.max(symbolSize * 2.4d, paneHeight * 0.72d);
            double cursorX = horizontalInset + ratio * usableWidth - cursorWidth / 2.0d;
            double cursorY = trackCenterY - cursorHeight / 2.0d;
            cursor.resizeRelocate(cursorX, cursorY, cursorWidth, cursorHeight);
            timelineMarkers.getChildren().add(cursor);
        }

        List<TimelineMarkerGroup> groups = timelineMarkerGroups(events, currentIndex, usableWidth);
        for (TimelineMarkerGroup group : groups) {
            int representativeIndex = group.representativeIndex();
            EventEnvelope envelope = events.get(representativeIndex);
            VBox marker = new VBox(Math.max(1.0d, fontSize * 0.06d));
            marker.setAlignment(javafx.geometry.Pos.TOP_CENTER);
            marker.setManaged(false);
            marker.setFocusTraversable(true);
            marker.getStyleClass().add("timeline-marker-node");
            if (group.eventIndexes().size() > 1) {
                marker.getStyleClass().add("timeline-marker-aggregate");
            }

            Region symbol = new Region();
            symbol.setMinSize(symbolSize, symbolSize);
            symbol.setPrefSize(symbolSize, symbolSize);
            symbol.setMaxSize(symbolSize, symbolSize);
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

            timelineMarkers.getChildren().add(marker);
            marker.applyCss();
            marker.autosize();
            double markerWidth = Math.max(symbolSize, marker.prefWidth(-1.0d));
            double resolvedMarkerHeight = Math.max(symbolSize, marker.prefHeight(markerWidth));
            marker.resize(markerWidth, resolvedMarkerHeight);

            double ratio = eventRatio(representativeIndex, events.size());
            double centerX = horizontalInset + ratio * usableWidth;
            double markerX = Math.max(0.0d, Math.min(paneWidth - markerWidth, centerX - markerWidth / 2.0d));
            double markerY = Math.max(0.0d, trackCenterY - symbolSize / 2.0d);
            marker.relocate(markerX, markerY);

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
        }
    }

    private double timelineBaseFontSize() {
        if (timelineLabel != null && timelineLabel.getFont() != null) {
            return Math.max(8.0d, timelineLabel.getFont().getSize());
        }
        return 16.0d;
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

        double markerFootprint = Math.max(18.0d, timelineBaseFontSize() * 1.75d);
        int markerBudget = (int) Math.floor(usableWidth / markerFootprint);
        markerBudget = Math.max(3, Math.min(48, markerBudget));
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
        String base = I18N.text("label.workspace.event") + " " + envelope.sequence()
                + " · " + eventDisplayName(envelope);
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
        String key = paused ? "action.execution.resume" : "action.execution.pause";
        pauseBtn.setText(paused ? "▶" : "❚❚");
        pauseBtn.setAccessibleText(I18N.text(key));
    }
}
