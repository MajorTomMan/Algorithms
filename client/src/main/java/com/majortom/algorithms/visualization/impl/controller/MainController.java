package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.utils.EffectUtils;
import com.majortom.algorithms.visualization.BaseController;
import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.VisualizationActionType;
import com.majortom.algorithms.visualization.VisualizationEvent;
import com.majortom.algorithms.visualization.WorkbenchControls;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.module.AlgorithmModuleDefinition;
import com.majortom.algorithms.visualization.module.ModuleRegistry;
import com.majortom.algorithms.visualization.structure.InMemoryStructureSnapshotStore;
import com.majortom.algorithms.visualization.structure.StructureSnapshot;
import com.majortom.algorithms.visualization.structure.StructureSnapshotSupport;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * 双工作区 JavaFX 外壳。
 *
 * <p>结构工作区负责输入、编辑和快照，算法工作区负责选择算法、执行以及逐帧
 * 回放。模块控制器仍然拥有具体的输入和算法逻辑，外壳只负责把同一套模块面板
 * 按语义区段装配到两个工作区。</p>
 */
public class MainController implements Initializable {

    private static final PseudoClass COMPACT_LAYOUT = PseudoClass.getPseudoClass("compact-layout");
    private static final PseudoClass NARROW_LAYOUT = PseudoClass.getPseudoClass("narrow-layout");
    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
    private static final PseudoClass WORKSPACE_FOCUS = PseudoClass.getPseudoClass("workspace-focus");
    private static final double COMPACT_LAYOUT_WIDTH = 1180.0d;
    private static final double NARROW_LAYOUT_WIDTH = 860.0d;
    private static final DateTimeFormatter SNAPSHOT_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML
    private BorderPane rootPane;
    @FXML
    private VBox topShell;
    @FXML
    private HBox topBar;
    @FXML
    private HBox moduleMenuBox;
    @FXML
    private HBox workspaceModeBox;
    @FXML
    private Button structureWorkspaceBtn;
    @FXML
    private Button algorithmWorkspaceBtn;
    @FXML
    private Button langBtn;
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
    private TextArea logArea;
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

    private final List<AlgorithmModuleDefinition> moduleDefinitions = ModuleRegistry.defaults();
    private final InMemoryStructureSnapshotStore structureSnapshotStore =
            new InMemoryStructureSnapshotStore();
    private final Map<String, Button> moduleButtons = new LinkedHashMap<>();
    private BaseController<?> currentSubController;
    private AlgorithmModuleDefinition activeDefinition;
    private javafx.beans.value.ChangeListener<Number> structureRevisionListener;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (resources != null) {
            I18N.setLocale(resources.getLocale());
        }

        setupI18n();
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
        logArea.promptTextProperty().bind(I18N.createStringBinding("label.panel.log.prompt"));
        stepBackwardBtn.accessibleTextProperty().bind(
                I18N.createStringBinding("action.execution.step.backward"));
        stepForwardBtn.accessibleTextProperty().bind(
                I18N.createStringBinding("action.execution.step.forward"));
        I18N.localeProperty().addListener((observable, oldValue, newValue) -> {
            refreshPauseText();
            refreshWorkspaceContext();
        });
        refreshPauseText();
    }

    private void setupModuleMenu() {
        moduleMenuBox.getChildren().clear();
        moduleButtons.clear();
        for (AlgorithmModuleDefinition definition : moduleDefinitions) {
            Button button = new Button();
            button.setMaxWidth(Double.MAX_VALUE);
            button.getStyleClass().add("menu-button");
            button.getStyleClass().add("module-button");
            button.getStyleClass().add(moduleAccentStyleClass(definition.id()));
            button.textProperty().bind(I18N.createStringBinding(definition.labelKey()));
            button.setOnAction(event -> switchToModule(definition));
            moduleMenuBox.getChildren().add(button);
            moduleButtons.put(definition.id(), button);
        }
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
        if (nextNarrowLayout) {
            snapshotPanel.setManaged(false);
            snapshotPanel.setVisible(false);
        } else {
            snapshotPanel.setManaged(true);
            snapshotPanel.setVisible(true);
        }
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

    private void switchToModule(AlgorithmModuleDefinition definition) {
        activeDefinition = definition;
        loadSubController(definition.controllerFactory().get());
        refreshWorkspaceContext();
        if (currentSubController != null) {
            currentSubController.dispatchVisualizerEvent(mainEvent(moduleSwitchAction(definition.id())));
        }
        moduleButtons.forEach((id, button) ->
                button.pseudoClassStateChanged(SELECTED, id.equals(definition.id())));
    }

    private void loadSubController(BaseController<?> newController) {
        detachCurrentController();
        visualizationContainer.getChildren().clear();
        structureControlsHost.getChildren().clear();
        algorithmControlsHost.getChildren().clear();
        customControlBox.getChildren().clear();

        newController.setUIReferences(new WorkbenchControls(
                statsLabel,
                logArea,
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
        currentSubController.runningProperty().addListener(
                (observable, oldValue, newValue) -> updateSnapshotActionState());
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
    }

    private Node createSnapshotCard(
            String moduleName,
            String status,
            StructureSnapshot<?> snapshot,
            StructureSnapshotSupport<?> support,
            boolean current) {
        VBox content = new VBox(4);
        content.setMaxWidth(Double.MAX_VALUE);
        Label title = new Label(moduleName);
        if (!current) {
            title.setText(moduleName + " · " + shortSnapshotId(snapshot));
        }
        title.getStyleClass().add("snapshot-card-title");
        Label state = new Label(status);
        state.getStyleClass().add("snapshot-card-state");
        Label detail = new Label(describeSnapshot(support, snapshot));
        detail.getStyleClass().add("snapshot-card-detail");
        Label createdAt = new Label(I18N.text(
                "label.workspace.snapshot.time", formatSnapshotTime(snapshot)));
        createdAt.getStyleClass().add("snapshot-card-detail");
        content.getChildren().addAll(title, state, detail);
        if (!current) {
            content.getChildren().add(createdAt);
        }
        if (current) {
            content.getStyleClass().add("snapshot-card");
            content.getStyleClass().add("snapshot-card-current");
            return content;
        }

        Button card = new Button();
        card.setMaxWidth(Double.MAX_VALUE);
        card.setGraphic(content);
        card.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        card.getStyleClass().add("snapshot-card");
        card.getStyleClass().add("snapshot-card-saved");
        card.setOnAction(event -> restoreSnapshot(snapshot));
        return card;
    }

    private StructureSnapshotSupport<?> currentSnapshotSupport() {
        if (currentSubController instanceof StructureSnapshotSupport<?> support) {
            return support;
        }
        return null;
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
        if (currentSubController != null) {
            currentSubController.dispatchVisualizerEvent(mainEvent(VisualizationActionType.LANGUAGE_TOGGLE));
        }
        Locale newLocale = Locale.CHINESE;
        if (I18N.getLocale().getLanguage().equals("zh")) {
            newLocale = Locale.ENGLISH;
        }
        I18N.setLocale(newLocale);
        appendSystemLog(I18N.text(
                "message.system.language_switched", newLocale.getDisplayLanguage(newLocale)));
    }

    private void appendSystemLog(String message) {
        if (logArea == null) {
            return;
        }
        logArea.appendText("System: " + message + "\n");
    }

    private String moduleAccentStyleClass(String moduleId) {
        return switch (moduleId) {
            case "sort" -> "btn-ran-blue";
            case "maze" -> "btn-ran-red";
            case "tree" -> "btn-ran-gold";
            case "graph" -> "btn-ran-white";
            default -> "btn-ran-blue";
        };
    }

    private VisualizationActionType moduleSwitchAction(String moduleId) {
        return switch (moduleId) {
            case "sort" -> VisualizationActionType.MODULE_SORT;
            case "maze" -> VisualizationActionType.MODULE_MAZE;
            case "tree" -> VisualizationActionType.MODULE_TREE;
            case "graph" -> VisualizationActionType.MODULE_GRAPH;
            default -> VisualizationActionType.MODULE_SORT;
        };
    }

    private VisualizationEvent mainEvent(VisualizationActionType actionType) {
        String moduleId = "unknown";
        boolean running = false;
        boolean paused = false;
        if (currentSubController != null) {
            moduleId = currentSubController.getModuleId();
            running = currentSubController.isRunning();
            paused = currentSubController.isPaused();
        }
        return VisualizationEvent.of(
                actionType,
                moduleId,
                getClass().getSimpleName(),
                running,
                paused);
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
