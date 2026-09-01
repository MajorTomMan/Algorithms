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

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * 主界面控制器。
 *
 * <p>
 * 它负责装配全局 UI：顶部模块导航、可视化容器、两侧面板和底部执行按钮。
 * 具体算法逻辑不在这里执行，而是通过 {@link ModuleRegistry} 创建当前模块的
 * {@link BaseController} 子控制器，再把共享控件注入进去。
 * </p>
 */
public class MainController implements Initializable {

    private static final PseudoClass COMPACT_LAYOUT = PseudoClass.getPseudoClass("compact-layout");
    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
    private static final PseudoClass WORKSPACE_FOCUS = PseudoClass.getPseudoClass("workspace-focus");
    private static final double COMPACT_LAYOUT_WIDTH = 1180.0d;
    private static final double NARROW_LAYOUT_WIDTH = 1020.0d;
    private static final String DIAGNOSTIC_SECTION_EXPANDED_PROPERTY =
            MainController.class.getName() + ".diagnosticSectionExpanded";

    @FXML
    private BorderPane rootPane;
    @FXML
    private VBox topShell;
    @FXML
    private HBox topBar;
    @FXML
    private Button structureWorkspaceBtn;
    @FXML
    private Button algorithmWorkspaceBtn;
    @FXML
    private StackPane workspaceLayer;
    @FXML
    private HBox desktopWorkspace;
    @FXML
    private StackPane visualizationViewport;
    @FXML
    private StackPane visualizationContainer;
    @FXML
    private HBox customControlBox;
    @FXML
    private VBox settingsPanel;
    @FXML
    private VBox diagnosticsPanel;
    @FXML
    private Region drawerScrim;
    @FXML
    private HBox moduleMenuBox;
    @FXML
    private Label menuTitleLabel;
    @FXML
    private Label settingsTitleLabel;
    @FXML
    private Label settingsKickerLabel;
    @FXML
    private Label viewportHintLabel;
    @FXML
    private Label liveLabel;
    @FXML
    private Label statsTitleLabel;
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
    private Button settingsToggleBtn;
    @FXML
    private Button diagnosticsToggleBtn;
    @FXML
    private Button langBtn;
    @FXML
    private Button settingsCloseBtn;
    @FXML
    private Button diagnosticsCloseBtn;
    @FXML
    private HBox narrowPanelToggleBar;
    @FXML
    private Button narrowSettingsToggleBtn;
    @FXML
    private Button narrowDiagnosticsToggleBtn;
    @FXML
    private Button narrowLangBtn;
    @FXML
    private Slider delaySlider;
    @FXML
    private Slider timelineSlider;

    private BaseController<?> currentSubController;
    private final List<AlgorithmModuleDefinition> moduleDefinitions = ModuleRegistry.defaults();
    private final Map<String, Button> moduleButtons = new LinkedHashMap<>();
    private boolean narrowLayout;
    private boolean settingsAutoCollapsed;
    private boolean diagnosticsAutoCollapsed;

    /**
     * JavaFX 初始化入口。
     *
     * @param location  FXML 地址
     * @param resources 国际化资源
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (resources != null) {
            I18N.setLocale(resources.getLocale());
        }

        setupI18n();
        setupWorkspaceMode();
        setupModuleMenu();
        setupGlobalEffects();
        setupLayoutClips();
        setupResponsiveLayout();

        if (!moduleDefinitions.isEmpty()) {
            switchToModule(moduleDefinitions.getFirst());
        }

        appendSystemLog(I18N.text("message.system.initialized"));
    }

    /**
     * 绑定主界面固定文案和暂停按钮动态文案。
     */
    private void setupI18n() {
        menuTitleLabel.textProperty().bind(I18N.createStringBinding("label.menu.title"));
        structureWorkspaceBtn.textProperty().bind(I18N.createStringBinding("label.workspace.structure"));
        algorithmWorkspaceBtn.textProperty().bind(I18N.createStringBinding("label.workspace.algorithm"));
        settingsTitleLabel.textProperty().bind(I18N.createStringBinding("label.panel.settings"));
        settingsKickerLabel.textProperty().bind(I18N.createStringBinding("label.panel.parameters"));
        viewportHintLabel.textProperty().bind(I18N.createStringBinding("label.panel.viewport.hint"));
        settingsToggleBtn.textProperty().bind(I18N.createStringBinding("label.panel.settings.short"));
        diagnosticsToggleBtn.textProperty().bind(I18N.createStringBinding("label.panel.stats.short"));
        narrowSettingsToggleBtn.textProperty().bind(I18N.createStringBinding("label.panel.settings.short"));
        narrowDiagnosticsToggleBtn.textProperty().bind(I18N.createStringBinding("label.panel.stats.short"));
        liveLabel.textProperty().bind(I18N.createStringBinding("label.panel.live"));
        statsTitleLabel.textProperty().bind(I18N.createStringBinding("label.panel.stats"));
        logTitleLabel.textProperty().bind(I18N.createStringBinding("label.panel.log"));
        startBtn.textProperty().bind(I18N.createStringBinding("action.execution.start"));
        resetBtn.textProperty().bind(I18N.createStringBinding("action.execution.reset"));
        replayBtn.textProperty().bind(I18N.createStringBinding("action.execution.replay"));
        exportBtn.textProperty().bind(I18N.createStringBinding("action.execution.export"));
        compareBtn.textProperty().bind(I18N.createStringBinding("action.execution.compare"));
        logArea.promptTextProperty().bind(I18N.createStringBinding("label.panel.log.prompt"));
        stepBackwardBtn.accessibleTextProperty().bind(
                I18N.createStringBinding("action.execution.step.backward"));
        stepForwardBtn.accessibleTextProperty().bind(
                I18N.createStringBinding("action.execution.step.forward"));
        settingsCloseBtn.accessibleTextProperty().bind(
                I18N.createStringBinding("label.panel.drawer.close"));
        diagnosticsCloseBtn.accessibleTextProperty().bind(
                I18N.createStringBinding("label.panel.drawer.close"));
        delayLabel.textProperty().bind(I18N.createStringBinding("label.execution.delay"));
        timelineLabel.textProperty().bind(I18N.createStringBinding("label.execution.timeline"));

        I18N.localeProperty().addListener((observable, oldValue, newValue) -> refreshPauseText());
        refreshPauseText();
    }

    /** Installs the two top-level workspace entry points. */
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
        if (structureWorkspaceBtn != null) {
            structureWorkspaceBtn.pseudoClassStateChanged(SELECTED, structure);
        }
        if (algorithmWorkspaceBtn != null) {
            algorithmWorkspaceBtn.pseudoClassStateChanged(SELECTED, !structure);
        }
        updateWorkspaceFocus(structure);
    }

    private void updateWorkspaceFocus(boolean structure) {
        if (settingsPanel == null || diagnosticsPanel == null) {
            return;
        }
        settingsPanel.pseudoClassStateChanged(WORKSPACE_FOCUS, structure);
        diagnosticsPanel.pseudoClassStateChanged(WORKSPACE_FOCUS, !structure);
        if (!narrowLayout) {
            return;
        }
        settingsAutoCollapsed = false;
        diagnosticsAutoCollapsed = false;
        applyPanelVisibility(settingsPanel, settingsToggleBtn, structure);
        applyPanelVisibility(diagnosticsPanel, diagnosticsToggleBtn, !structure);
    }

    /**
     * 根据模块注册表创建顶部模块导航。
     */
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

    /**
     * 给主界面按钮安装统一动效。
     */
    private void setupGlobalEffects() {
        EffectUtils.applyDynamicEffect(
                startBtn, pauseBtn, resetBtn, replayBtn, stepBackwardBtn, stepForwardBtn,
                exportBtn, compareBtn, settingsToggleBtn, diagnosticsToggleBtn, langBtn,
                settingsCloseBtn, diagnosticsCloseBtn, structureWorkspaceBtn,
                algorithmWorkspaceBtn, narrowSettingsToggleBtn, narrowDiagnosticsToggleBtn,
                narrowLangBtn);
    }

    private void setupLayoutClips() {
        bindClip(topShell);
        bindClip(topBar);
        bindClip(workspaceLayer);
        bindClip(desktopWorkspace);
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
        boolean compact = width > 0.0d && width < COMPACT_LAYOUT_WIDTH;
        rootPane.pseudoClassStateChanged(COMPACT_LAYOUT, compact);
        if (settingsPanel != null) {
            settingsPanel.setPrefWidth(compact ? 220.0d : 250.0d);
        }
        if (diagnosticsPanel != null) {
            diagnosticsPanel.setPrefWidth(compact ? 250.0d : 300.0d);
        }
        boolean narrow = width > 0.0d && width < NARROW_LAYOUT_WIDTH;
        rootPane.pseudoClassStateChanged(
                PseudoClass.getPseudoClass("narrow-layout"), narrow);
        if (narrowPanelToggleBar != null) {
            narrowPanelToggleBar.setManaged(narrow);
            narrowPanelToggleBar.setVisible(narrow);
        }
        setDesktopToolbarVisibility(!narrow);
        if (narrow == narrowLayout) {
            updateDrawerMask();
            syncPanelToggleStates();
            return;
        }
        narrowLayout = narrow;
        if (narrow) {
            settingsAutoCollapsed = settingsPanel.isManaged();
            diagnosticsAutoCollapsed = diagnosticsPanel.isManaged();
            movePanelsToDrawerLayer();
            applyPanelVisibility(settingsPanel, settingsToggleBtn, false);
            applyPanelVisibility(diagnosticsPanel, diagnosticsToggleBtn, false);
            updateDrawerMask();
            syncPanelToggleStates();
            return;
        }
        movePanelsToDesktopLayout();
        if (settingsAutoCollapsed) {
            applyPanelVisibility(settingsPanel, settingsToggleBtn, true);
        }
        if (diagnosticsAutoCollapsed) {
            applyPanelVisibility(diagnosticsPanel, diagnosticsToggleBtn, true);
        }
        settingsAutoCollapsed = false;
        diagnosticsAutoCollapsed = false;
        updateDrawerMask();
        syncPanelToggleStates();
    }

    private void setDesktopToolbarVisibility(boolean visible) {
        setControlVisibility(settingsToggleBtn, visible);
        setControlVisibility(diagnosticsToggleBtn, visible);
        setControlVisibility(langBtn, visible);
    }

    private void setControlVisibility(Node control, boolean visible) {
        if (control == null) {
            return;
        }
        control.setManaged(visible);
        control.setVisible(visible);
    }

    @FXML
    private void toggleSettingsPanel() {
        if (narrowLayout) {
            settingsAutoCollapsed = false;
        }
        applyPanelVisibility(settingsPanel, settingsToggleBtn, !settingsPanel.isManaged());
    }

    @FXML
    private void toggleDiagnosticsPanel() {
        if (narrowLayout) {
            diagnosticsAutoCollapsed = false;
        }
        applyPanelVisibility(diagnosticsPanel, diagnosticsToggleBtn, !diagnosticsPanel.isManaged());
    }

    @FXML
    private void closeDrawers() {
        applyPanelVisibility(settingsPanel, settingsToggleBtn, false);
        applyPanelVisibility(diagnosticsPanel, diagnosticsToggleBtn, false);
    }

    private void movePanelsToDrawerLayer() {
        if (desktopWorkspace == null || workspaceLayer == null) {
            return;
        }
        desktopWorkspace.getChildren().remove(settingsPanel);
        desktopWorkspace.getChildren().remove(diagnosticsPanel);
        if (!workspaceLayer.getChildren().contains(settingsPanel)) {
            workspaceLayer.getChildren().add(settingsPanel);
        }
        if (!workspaceLayer.getChildren().contains(diagnosticsPanel)) {
            workspaceLayer.getChildren().add(diagnosticsPanel);
        }
        StackPane.setAlignment(settingsPanel, Pos.CENTER_LEFT);
        StackPane.setAlignment(diagnosticsPanel, Pos.CENTER_RIGHT);
    }

    private void movePanelsToDesktopLayout() {
        if (desktopWorkspace == null || workspaceLayer == null) {
            return;
        }
        workspaceLayer.getChildren().remove(settingsPanel);
        workspaceLayer.getChildren().remove(diagnosticsPanel);
        if (!desktopWorkspace.getChildren().contains(settingsPanel)) {
            desktopWorkspace.getChildren().add(0, settingsPanel);
        }
        if (!desktopWorkspace.getChildren().contains(diagnosticsPanel)) {
            desktopWorkspace.getChildren().add(diagnosticsPanel);
        }
        HBox.setHgrow(visualizationViewport, Priority.ALWAYS);
    }

    /** Toggles the statistics or log content below the clicked diagnostics header. */
    @FXML
    private void toggleDiagnosticsSection(MouseEvent event) {
        toggleDiagnosticsSection(event.getSource());
        event.consume();
    }

    /** Allows keyboard users to expand or collapse the focused diagnostics header. */
    @FXML
    private void handleDiagnosticsSectionKey(KeyEvent event) {
        if (event.getCode() != KeyCode.ENTER && event.getCode() != KeyCode.SPACE) {
            return;
        }
        toggleDiagnosticsSection(event.getSource());
        event.consume();
    }

    private void toggleDiagnosticsSection(Object source) {
        if (!(source instanceof Node header)
                || header.getParent() != diagnosticsPanel) {
            return;
        }

        int headerIndex = diagnosticsPanel.getChildren().indexOf(header);
        if (headerIndex < 0 || headerIndex + 1 >= diagnosticsPanel.getChildren().size()) {
            return;
        }

        Node content = diagnosticsPanel.getChildren().get(headerIndex + 1);
        boolean expanded = !Boolean.FALSE.equals(
                header.getProperties().get(DIAGNOSTIC_SECTION_EXPANDED_PROPERTY));
        setDiagnosticsSectionExpanded(header, content, !expanded);
    }

    private void setDiagnosticsSectionExpanded(Node header, Node content, boolean expanded) {
        header.getProperties().put(DIAGNOSTIC_SECTION_EXPANDED_PROPERTY, expanded);
        content.setManaged(expanded);
        content.setVisible(expanded);
        updateDiagnosticsChevron(header, expanded);
    }

    private void updateDiagnosticsChevron(Node header, boolean expanded) {
        if (!(header instanceof Pane pane)) {
            return;
        }
        for (Node child : pane.getChildren()) {
            if (child instanceof Label label && label.getStyleClass().contains("section-chevron")) {
                label.setText(expanded ? "⌃" : "⌄");
                return;
            }
        }
    }

    private void applyPanelVisibility(VBox panel, Button toggleButton, boolean visible) {
        if (panel == null) {
            return;
        }
        panel.setManaged(visible);
        panel.setVisible(visible);
        if (toggleButton != null) {
            toggleButton.pseudoClassStateChanged(
                    PseudoClass.getPseudoClass("selected"), visible);
        }
        if (panel == settingsPanel && narrowSettingsToggleBtn != null) {
            narrowSettingsToggleBtn.pseudoClassStateChanged(
                    PseudoClass.getPseudoClass("selected"), visible);
        }
        if (panel == diagnosticsPanel && narrowDiagnosticsToggleBtn != null) {
            narrowDiagnosticsToggleBtn.pseudoClassStateChanged(
                    PseudoClass.getPseudoClass("selected"), visible);
        }
        updateDrawerMask();
    }

    private void updateDrawerMask() {
        if (drawerScrim == null) {
            return;
        }
        boolean drawerVisible = narrowLayout
                && (isPanelVisible(settingsPanel) || isPanelVisible(diagnosticsPanel));
        drawerScrim.setManaged(drawerVisible);
        drawerScrim.setVisible(drawerVisible);
        drawerScrim.setMouseTransparent(!drawerVisible);
    }

    private boolean isPanelVisible(VBox panel) {
        return panel != null && panel.isManaged() && panel.isVisible();
    }

    private void syncPanelToggleStates() {
        syncPanelToggleState(settingsPanel, settingsToggleBtn, narrowSettingsToggleBtn);
        syncPanelToggleState(diagnosticsPanel, diagnosticsToggleBtn, narrowDiagnosticsToggleBtn);
    }

    private void syncPanelToggleState(VBox panel, Button topToggleButton, Button narrowToggleButton) {
        if (panel == null) {
            return;
        }
        boolean visible = panel.isManaged() && panel.isVisible();
        if (topToggleButton != null) {
            topToggleButton.pseudoClassStateChanged(SELECTED, visible);
        }
        if (narrowToggleButton != null) {
            narrowToggleButton.pseudoClassStateChanged(SELECTED, visible);
        }
    }

    /**
     * 切换到指定模块。
     *
     * @param definition 模块定义
     */
    private void switchToModule(AlgorithmModuleDefinition definition) {
        loadSubController(definition.controllerFactory().get());
        if (currentSubController != null) {
            currentSubController.dispatchVisualizerEvent(mainEvent(moduleSwitchAction(definition.id())));
        }
        moduleButtons.forEach((id, button) -> button.pseudoClassStateChanged(
                javafx.css.PseudoClass.getPseudoClass("selected"), id.equals(definition.id())));
    }

    /**
     * 加载子控制器并接入可视化区域。
     *
     * @param newController 新模块控制器
     */
    private void loadSubController(BaseController<?> newController) {
        if (currentSubController != null) {
            BaseVisualizer<?> previousVisualizer = currentSubController.getVisualizer();
            if (previousVisualizer != null) {
                previousVisualizer.prefWidthProperty().unbind();
                previousVisualizer.prefHeightProperty().unbind();
            }
            currentSubController.dispatchVisualizerDetached();
        }
        visualizationContainer.getChildren().clear();
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

        this.currentSubController = newController;
        this.currentSubController.pausedProperty().addListener(
                (observable, oldValue, newValue) -> refreshPauseText());
        this.currentSubController.dispatchVisualizerAttached();

        BaseVisualizer<?> visualizer = newController.getVisualizer();
        if (visualizer != null) {
            visualizer.prefWidthProperty().bind(visualizationContainer.widthProperty());
            visualizer.prefHeightProperty().bind(visualizationContainer.heightProperty());
            visualizationContainer.getChildren().add(visualizer);
        }

        newController.setupCustomControls(customControlBox);
        stretchModuleControls();
    }

    private void stretchModuleControls() {
        if (customControlBox == null || customControlBox.getChildren().isEmpty()) {
            return;
        }
        javafx.scene.Node controlPanel = customControlBox.getChildren().getFirst();
        HBox.setHgrow(controlPanel, Priority.ALWAYS);
        if (controlPanel instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
    }

    /**
     * 切换界面语言。
     */
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
        appendSystemLog(I18N.text("message.system.language_switched", newLocale.getDisplayLanguage(newLocale)));
    }

    /**
     * 向系统日志追加文本。
     *
     * @param msg 日志文本
     */
    private void appendSystemLog(String msg) {
        if (logArea != null) {
            logArea.appendText("System: " + msg + "\n");
        }
    }

    /**
     * 根据模块 ID 选择菜单按钮强调色。
     *
     * @param moduleId 模块 ID
     * @return CSS class 名称
     */
    private String moduleAccentStyleClass(String moduleId) {
        return switch (moduleId) {
            case "sort" -> "btn-ran-blue";
            case "maze" -> "btn-ran-red";
            case "tree" -> "btn-ran-gold";
            case "graph" -> "btn-ran-gold";
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
        boolean paused = currentSubController != null && currentSubController.isPaused();
        String key = "action.execution.pause";
        if (paused) {
            key = "action.execution.resume";
        }
        pauseBtn.setText(I18N.text(key));
    }
}
