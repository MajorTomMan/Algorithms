package com.majortom.algorithms.visualization.layout;

import atlantafx.base.theme.Styles;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Central geometry and text policy for the Workbench shell.
 *
 * <p>The visualization hosts are deliberately excluded: their coordinate
 * system is controlled by FIT/CENTER/zoom. Everything around the canvas uses
 * this framework for font-aware sizing, responsive shell geometry and control
 * density.</p>
 */
public final class WorkbenchUiFramework {

    private static final PseudoClass COMPACT_LAYOUT = PseudoClass.getPseudoClass("compact-layout");
    private static final PseudoClass NARROW_LAYOUT = PseudoClass.getPseudoClass("narrow-layout");
    private static final double BASE_MODE_FONT = 16.0d;
    private static final double MIN_CANVAS_WIDTH = 360.0d;
    private static final double BASE_FAMILY_WIDTH = 156.0d;
    private static final double MIN_FAMILY_WIDTH = 148.0d;
    private static final double MAX_FAMILY_WIDTH = 260.0d;
    private static final double NORMAL_FAMILY_ROW_HEIGHT = 76.0d;
    private static final double COMPACT_FAMILY_ROW_HEIGHT = 60.0d;
    private static final double NARROW_FAMILY_ROW_HEIGHT = 52.0d;
    private static final double MIN_CONTROL_WIDTH = 260.0d;
    private static final double MIN_INSPECTOR_WIDTH = 300.0d;
    private static final double CONTROL_VERTICAL_PADDING = 12.0d;

    private final BorderPane root;
    private final Region topBar;
    private final Region brandZone;
    private final Node brandSubtitle;
    private final Region workspaceModeBox;
    private final Region topContextZone;
    private final Node topContextLabel;
    private final Node runIdLabel;
    private final Node fontSettingsButton;
    private final Region structureFamilyRail;
    private final Region algorithmFamilyRail;
    private final Region structureControlRail;
    private final Region algorithmControlRail;
    private final Region practiceControlRail;
    private final Region structureInspector;
    private final Region algorithmInspector;
    private final Region structureOverlay;
    private final Region currentStepOverlay;
    private final Region algorithmOverlay;
    private final TabPane structureTabs;
    private final TabPane algorithmTabs;
    private final PlaybackToolbar playbackToolbar;
    private final Region structureHistoryDock;
    private final Node structureHistoryDetails;

    private final Set<Node> ownedSmall = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<Node> ownedDense = Collections.newSetFromMap(new IdentityHashMap<>());
    private LayoutState state = new LayoutState(false, false);
    private boolean structureHistoryExpanded;
    private boolean refreshScheduled;

    public WorkbenchUiFramework(
            BorderPane root,
            Region topBar,
            Region brandZone,
            Node brandSubtitle,
            Region workspaceModeBox,
            Region topContextZone,
            Node topContextLabel,
            Node runIdLabel,
            Node fontSettingsButton,
            Region structureFamilyRail,
            Region algorithmFamilyRail,
            Region structureControlRail,
            Region algorithmControlRail,
            Region practiceControlRail,
            Region structureInspector,
            Region algorithmInspector,
            Region structureOverlay,
            Region currentStepOverlay,
            Region algorithmOverlay,
            TabPane structureTabs,
            TabPane algorithmTabs,
            PlaybackToolbar playbackToolbar,
            Region structureHistoryDock,
            Node structureHistoryDetails) {
        this.root = root;
        this.topBar = topBar;
        this.brandZone = brandZone;
        this.brandSubtitle = brandSubtitle;
        this.workspaceModeBox = workspaceModeBox;
        this.topContextZone = topContextZone;
        this.topContextLabel = topContextLabel;
        this.runIdLabel = runIdLabel;
        this.fontSettingsButton = fontSettingsButton;
        this.structureFamilyRail = structureFamilyRail;
        this.algorithmFamilyRail = algorithmFamilyRail;
        this.structureControlRail = structureControlRail;
        this.algorithmControlRail = algorithmControlRail;
        this.practiceControlRail = practiceControlRail;
        this.structureInspector = structureInspector;
        this.algorithmInspector = algorithmInspector;
        this.structureOverlay = structureOverlay;
        this.currentStepOverlay = currentStepOverlay;
        this.algorithmOverlay = algorithmOverlay;
        this.structureTabs = structureTabs;
        this.algorithmTabs = algorithmTabs;
        this.playbackToolbar = playbackToolbar;
        this.structureHistoryDock = structureHistoryDock;
        this.structureHistoryDetails = structureHistoryDetails;
    }

    public void install() {
        if (root == null) {
            return;
        }
        root.widthProperty().addListener((observable, oldValue, newValue) -> scheduleRefresh());
        root.heightProperty().addListener((observable, oldValue, newValue) -> scheduleRefresh());
        root.sceneProperty().addListener((observable, oldValue, newValue) -> scheduleRefresh());
        scheduleRefresh();
    }

    public LayoutState refresh() {
        if (root == null) {
            return state;
        }
        if (root.getScene() != null) {
            root.applyCss();
        }
        normalizeShellControls(root);

        double width = root.getWidth() > 0.0d ? root.getWidth() : root.prefWidth(-1.0d);
        double height = root.getHeight() > 0.0d ? root.getHeight() : root.prefHeight(width);
        double scale = fontScale();

        double familyContentWidth = familyRailContentWidth();
        double familyWidth = clamp(Math.max(familyContentWidth, BASE_FAMILY_WIDTH * scale),
                MIN_FAMILY_WIDTH, MAX_FAMILY_WIDTH);
        double controlWidth = clamp(320.0d * scale, MIN_CONTROL_WIDTH, 460.0d);
        double inspectorWidth = Math.max(MIN_INSPECTOR_WIDTH,
                Math.max(inspectorTextWidth(structureTabs), inspectorTextWidth(algorithmTabs)));
        inspectorWidth = clamp(Math.max(inspectorWidth, 360.0d * Math.min(scale, 1.30d)), MIN_INSPECTOR_WIDTH, 500.0d);

        double requiredBody = familyWidth + controlWidth + inspectorWidth + MIN_CANVAS_WIDTH;
        boolean compact = width > 0.0d && (width < requiredBody + 80.0d || height < 820.0d * Math.min(scale, 1.18d));
        boolean narrow = width > 0.0d && (width < familyWidth + controlWidth + MIN_CANVAS_WIDTH + 80.0d
                || height < 650.0d);

        if (compact) {
            controlWidth = Math.max(MIN_CONTROL_WIDTH, controlWidth * 0.86d);
            inspectorWidth = Math.max(MIN_INSPECTOR_WIDTH, inspectorWidth * 0.88d);
        }
        if (narrow) {
            // Family navigation keeps a stable, comfortable width. Narrow layouts
            // compress the control rail and inspector first; identity navigation
            // must not collapse to a text-tight strip.
            controlWidth = Math.max(220.0d, controlWidth * 0.90d);
        }

        state = new LayoutState(compact, narrow);
        root.pseudoClassStateChanged(COMPACT_LAYOUT, compact);
        root.pseudoClassStateChanged(NARROW_LAYOUT, narrow);

        layoutHeader(width, scale, compact, narrow);
        setFixedWidth(structureControlRail, controlWidth);
        setFixedWidth(algorithmControlRail, controlWidth);
        setFixedWidth(practiceControlRail, practiceControlWidth(scale, compact, narrow));
        setFixedWidth(structureInspector, inspectorWidth);
        setFixedWidth(algorithmInspector, inspectorWidth);
        setOverlayWidth(structureOverlay, overlayWidth(scale, compact, narrow));
        setOverlayWidth(currentStepOverlay, overlayWidth(scale, compact, narrow));
        setOverlayWidth(algorithmOverlay, overlayWidth(scale, compact, narrow));
        setVisibleManaged(algorithmInspector, !narrow);
        configureInspectorTabs(structureTabs, inspectorWidth);
        configureInspectorTabs(algorithmTabs, inspectorWidth);
        applyControlDensity(root, compact);
        // Density classes and pseudo-classes must settle before Family Rail geometry is
        // applied. Family navigation is identity/navigation chrome, not a compact form
        // control, so it keeps the legacy roomy row rhythm under the new layout owner.
        if (root.getScene() != null) {
            root.applyCss();
        }
        double familyRowHeight = familyRowHeight(scale, compact, narrow);
        layoutFamilyRail(structureFamilyRail, familyWidth, familyRowHeight);
        layoutFamilyRail(algorithmFamilyRail, familyWidth, familyRowHeight);
        layoutStructureHistory(compact, narrow);

        if (playbackToolbar != null) {
            playbackToolbar.requestLayout();
        }
        root.requestLayout();
        return state;
    }

    public LayoutState state() {
        return state;
    }

    /** The history panel follows the same font/viewport policy as the rest of the shell. */
    public void setStructureHistoryExpanded(boolean expanded) {
        structureHistoryExpanded = expanded;
        scheduleRefresh();
    }

    /** Recompute after locale/font changes or after a module control panel is replaced. */
    public void scheduleRefresh() {
        if (refreshScheduled) {
            return;
        }
        refreshScheduled = true;
        Platform.runLater(() -> {
            refreshScheduled = false;
            refresh();
        });
    }

    private void layoutHeader(double width, double scale, boolean compact, boolean narrow) {
        if (topBar == null) {
            return;
        }
        setVisibleManaged(fontSettingsButton, true);
        setVisibleManaged(brandSubtitle, !narrow);
        setVisibleManaged(runIdLabel, !compact);
        setVisibleManaged(topContextLabel, !narrow);

        double brand = naturalWidth(brandZone, 250.0d * scale, 470.0d);
        double modes = modeSwitchWidth(scale);
        double context = naturalWidth(topContextZone, 190.0d * scale, 520.0d);
        double gapsAndPadding = 96.0d;
        if (width > 0.0d && brand + modes + context + gapsAndPadding > width) {
            setVisibleManaged(brandSubtitle, false);
            setVisibleManaged(runIdLabel, false);
            brand = naturalWidth(brandZone, 210.0d, 390.0d);
            context = naturalWidth(topContextZone, 150.0d, 420.0d);
        }
        if (width > 0.0d && brand + modes + context + gapsAndPadding > width) {
            setVisibleManaged(topContextLabel, false);
            context = naturalWidth(topContextZone, 120.0d, 280.0d);
        }

        setFixedWidth(brandZone, brand);
        setFixedWidth(workspaceModeBox, modes);
        setFixedWidth(topContextZone, context);
        double topHeight = Math.max(56.0d, maxChildPrefHeight(topBar) + 14.0d);
        setFixedHeight(topBar, topHeight);
    }

    private double modeSwitchWidth(double scale) {
        if (!(workspaceModeBox instanceof Parent parent)) {
            return clamp(420.0d * Math.min(scale, 1.25d), 300.0d, 620.0d);
        }
        double max = 0.0d;
        int count = 0;
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (child instanceof Labeled labeled && child.isManaged()) {
                max = Math.max(max, measuredTextWidth(labeled) + labeled.getInsets().getLeft()
                        + labeled.getInsets().getRight() + 28.0d);
                count++;
            }
        }
        if (count == 0) {
            return 420.0d;
        }
        return clamp(max * count + 2.0d, 300.0d, 640.0d);
    }


    private double familyRailContentWidth() {
        double widest = Math.max(familyRailContentWidth(structureFamilyRail),
                familyRailContentWidth(algorithmFamilyRail));
        if (widest <= 0.0d) {
            return MIN_FAMILY_WIDTH;
        }
        return Math.ceil(widest);
    }

    private double familyRailContentWidth(Region rail) {
        if (!(rail instanceof Parent parent)) {
            return 0.0d;
        }
        double widest = 0.0d;
        for (Node node : descendants(parent)) {
            if (!(node instanceof Labeled labeled)
                    || !node.isManaged()
                    || !node.getStyleClass().contains("family-rail-button")) {
                continue;
            }
            double horizontalInsets = node instanceof Region region
                    ? region.getInsets().getLeft() + region.getInsets().getRight()
                    : 0.0d;
            widest = Math.max(widest, measuredTextWidth(labeled) + horizontalInsets + 4.0d);
        }
        return widest + rail.getInsets().getLeft() + rail.getInsets().getRight();
    }

    private static List<Node> descendants(Parent root) {
        java.util.ArrayList<Node> nodes = new java.util.ArrayList<>();
        collectDescendants(root, nodes);
        return List.copyOf(nodes);
    }

    private static void collectDescendants(Parent parent, List<Node> target) {
        for (Node child : parent.getChildrenUnmodifiable()) {
            target.add(child);
            if (child instanceof Parent nested) {
                collectDescendants(nested, target);
            }
        }
    }

    private double inspectorTextWidth(TabPane tabs) {
        if (tabs == null || tabs.getTabs().isEmpty()) {
            return MIN_INSPECTOR_WIDTH;
        }
        Font font = shellFont();
        double total = 18.0d;
        for (var tab : tabs.getTabs()) {
            total += measuredTextWidth(tab.getText(), font) + 26.0d;
        }
        return Math.max(MIN_INSPECTOR_WIDTH, total);
    }

    private void configureInspectorTabs(TabPane tabs, double panelWidth) {
        if (tabs == null || tabs.getTabs().isEmpty()) {
            return;
        }
        double available = Math.max(56.0d, (panelWidth - 8.0d) / tabs.getTabs().size());
        tabs.setTabMinWidth(Math.min(available, 132.0d));
        tabs.setTabMaxWidth(Math.max(available, 132.0d));
    }

    private double fontScale() {
        Font font = shellFont();
        return clamp(font.getSize() / BASE_MODE_FONT, 0.82d, 1.50d);
    }

    private Font shellFont() {
        if (workspaceModeBox instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                if (child instanceof Labeled labeled && labeled.getFont() != null) {
                    return labeled.getFont();
                }
            }
        }
        return Font.getDefault();
    }

    private void normalizeShellControls(Node node) {
        if (node == null || isVisualizationSubtree(node)) {
            return;
        }
        if (node instanceof Button button) {
            button.setMinHeight(Math.max(32.0d, textControlHeight(button)));
            if (button.getStyleClass().contains("family-rail-button")) {
                button.setMinWidth(0.0d);
                button.setPrefWidth(Region.USE_COMPUTED_SIZE);
                button.setMaxWidth(Double.MAX_VALUE);
            }
        } else if (node instanceof TextInputControl input) {
            input.setMinHeight(Math.max(32.0d, textControlHeight(input)));
        } else if (node instanceof ComboBoxBase<?> combo) {
            combo.setMinHeight(Math.max(32.0d, textControlHeight(combo)));
        }
        if (node instanceof Region region
                && (node.getStyleClass().contains("run-summary-grid")
                || node.getStyleClass().contains("structure-overview-grid"))) {
            releaseHeight(region);
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                normalizeShellControls(child);
            }
        }
    }

    private double textControlHeight(Node node) {
        Font font = null;
        if (node instanceof Labeled labeled) {
            font = labeled.getFont();
        } else if (node instanceof TextInputControl input) {
            font = input.getFont();
        }
        if (font == null) {
            font = shellFont();
        }
        Text probe = new Text("国Ag");
        probe.setFont(font);
        double insets = node instanceof Region region
                ? region.getInsets().getTop() + region.getInsets().getBottom()
                : 0.0d;
        return Math.ceil(probe.getLayoutBounds().getHeight() + insets + CONTROL_VERTICAL_PADDING);
    }

    private boolean isVisualizationSubtree(Node node) {
        List<String> styles = node.getStyleClass();
        return styles.contains("lab-canvas")
                || styles.contains("visualization-viewport")
                || styles.contains("visualization-container");
    }

    private double naturalWidth(Region region, double minimum, double maximum) {
        if (region == null) {
            return minimum;
        }
        releaseWidth(region);
        double preferred = region.prefWidth(-1.0d);
        return clamp(preferred, minimum, maximum);
    }

    private double maxChildPrefHeight(Region region) {
        if (!(region instanceof Parent parent)) {
            return region.prefHeight(-1.0d);
        }
        return parent.getChildrenUnmodifiable().stream()
                .filter(Node::isManaged)
                .mapToDouble(node -> node.prefHeight(-1.0d))
                .max().orElse(42.0d);
    }

    private double practiceControlWidth(double scale, boolean compact, boolean narrow) {
        double width = 420.0d * Math.min(scale, 1.25d);
        if (compact) {
            width *= 0.90d;
        }
        if (narrow) {
            width *= 0.90d;
        }
        return clamp(width, 320.0d, 520.0d);
    }

    private double overlayWidth(double scale, boolean compact, boolean narrow) {
        double width = 220.0d * Math.min(scale, 1.35d);
        if (compact) {
            width *= 0.88d;
        }
        if (narrow) {
            width *= 0.88d;
        }
        return clamp(width, 180.0d, 320.0d);
    }

    private void layoutStructureHistory(boolean compact, boolean narrow) {
        if (structureHistoryDock == null) {
            return;
        }
        setVisibleManaged(structureHistoryDock, !narrow);
        if (narrow) {
            return;
        }

        if (structureHistoryDetails != null) {
            setVisibleManaged(structureHistoryDetails, structureHistoryExpanded);
        }
        structureHistoryDock.getStyleClass().removeAll("history-collapsed", "history-expanded");
        structureHistoryDock.getStyleClass().add(structureHistoryExpanded
                ? "history-expanded"
                : "history-collapsed");

        releaseHeight(structureHistoryDock);
        double lineHeight = mixedLineHeight(shellFont());
        double headerHeight = Math.max(lineHeight + 20.0d, firstManagedChildPrefHeight(structureHistoryDock));
        if (!structureHistoryExpanded) {
            setFixedHeight(structureHistoryDock, Math.ceil(headerHeight));
            return;
        }

        double rootHeight = root == null ? 0.0d : root.getHeight();
        double minimumDetails = lineHeight * 3.4d + 24.0d;
        double desiredDetails = structureHistoryDetails == null
                ? minimumDetails
                : Math.max(minimumDetails, structureHistoryDetails.prefHeight(-1.0d));
        double viewportBudget = rootHeight > 0.0d
                ? rootHeight * (compact ? 0.22d : 0.28d)
                : lineHeight * 7.0d + 44.0d;
        double maximumDetails = Math.max(minimumDetails, viewportBudget);
        double detailsHeight = Math.min(desiredDetails, maximumDetails);
        setFixedHeight(structureHistoryDock, Math.ceil(headerHeight + detailsHeight));
    }

    private double mixedLineHeight(Font font) {
        Text probe = new Text("国Ag");
        probe.setFont(font == null ? Font.getDefault() : font);
        return Math.ceil(probe.getLayoutBounds().getHeight());
    }

    private double firstManagedChildPrefHeight(Region region) {
        if (!(region instanceof Parent parent)) {
            return Math.max(0.0d, region.prefHeight(-1.0d));
        }
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (child.isManaged()) {
                return Math.max(0.0d, child.prefHeight(-1.0d));
            }
        }
        return 0.0d;
    }

    private void applyControlDensity(Node node, boolean compact) {
        if (node == null || isVisualizationSubtree(node)) {
            return;
        }
        if (node instanceof TabPane) {
            setOwnedClass(node, Styles.DENSE, compact, ownedDense);
        } else if ((node instanceof Button
                || node instanceof ComboBoxBase<?>
                || node instanceof TextInputControl
                || node instanceof Spinner<?>
                || node instanceof Slider)
                && !node.getStyleClass().contains("family-rail-button")) {
            setOwnedClass(node, Styles.SMALL, compact, ownedSmall);
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                applyControlDensity(child, compact);
            }
        }
    }

    private static void setOwnedClass(Node node, String styleClass, boolean enabled, Set<Node> owned) {
        if (enabled) {
            if (!node.getStyleClass().contains(styleClass)) {
                node.getStyleClass().add(styleClass);
                owned.add(node);
            }
        } else if (owned.remove(node)) {
            node.getStyleClass().remove(styleClass);
        }
    }

    private double familyRowHeight(double scale, boolean compact, boolean narrow) {
        double baseHeight;
        if (narrow) {
            baseHeight = NARROW_FAMILY_ROW_HEIGHT;
        } else if (compact) {
            baseHeight = COMPACT_FAMILY_ROW_HEIGHT;
        } else {
            baseHeight = NORMAL_FAMILY_ROW_HEIGHT;
        }
        // Keep the navigation rhythm stable once large-font mode is reached. Text
        // keeps scaling, while rows retain enough breathing room without consuming
        // the entire viewport at 21-24px.
        double rowScale = clamp(scale, 0.95d, 1.10d);
        return Math.ceil(baseHeight * rowScale);
    }

    private static void layoutFamilyRail(Region rail, double width, double rowHeight) {
        if (rail == null) {
            return;
        }
        setFixedWidth(rail, width);
        if (!(rail instanceof Parent parent)) {
            return;
        }
        for (Node node : descendants(parent)) {
            if (!(node instanceof Button button)
                    || !button.getStyleClass().contains("family-rail-button")) {
                continue;
            }
            button.setMinWidth(0.0d);
            button.setPrefWidth(Region.USE_COMPUTED_SIZE);
            button.setMaxWidth(Double.MAX_VALUE);
            button.setMinHeight(rowHeight);
            button.setPrefHeight(rowHeight);
            button.setMaxHeight(rowHeight);
        }
    }

    private double measuredTextWidth(Labeled labeled) {
        return measuredTextWidth(labeled.getText(), labeled.getFont());
    }

    private double measuredTextWidth(String value, Font font) {
        if (value == null || value.isBlank()) {
            return 0.0d;
        }
        Text text = new Text(value);
        text.setFont(font == null ? Font.getDefault() : font);
        return Math.ceil(text.getLayoutBounds().getWidth());
    }

    private static void setOverlayWidth(Region region, double width) {
        if (region == null) {
            return;
        }
        region.setMinWidth(width);
        region.setPrefWidth(width);
        region.setMaxWidth(width);
        region.setMinHeight(Region.USE_COMPUTED_SIZE);
        region.setPrefHeight(Region.USE_COMPUTED_SIZE);
        region.setMaxHeight(Region.USE_PREF_SIZE);
    }

    private static void setFixedWidth(Region region, double width) {
        if (region == null) {
            return;
        }
        region.setMinWidth(width);
        region.setPrefWidth(width);
        region.setMaxWidth(width);
    }

    private static void releaseWidth(Region region) {
        region.setMinWidth(Region.USE_COMPUTED_SIZE);
        region.setPrefWidth(Region.USE_COMPUTED_SIZE);
        region.setMaxWidth(Double.MAX_VALUE);
    }

    private static void releaseHeight(Region region) {
        if (region == null) {
            return;
        }
        region.setMinHeight(Region.USE_COMPUTED_SIZE);
        region.setPrefHeight(Region.USE_COMPUTED_SIZE);
        region.setMaxHeight(Double.MAX_VALUE);
    }

    private static void setFixedHeight(Region region, double height) {
        if (region == null) {
            return;
        }
        region.setMinHeight(height);
        region.setPrefHeight(height);
        region.setMaxHeight(height);
    }

    private static void setVisibleManaged(Node node, boolean visible) {
        if (node == null) {
            return;
        }
        node.setManaged(visible);
        node.setVisible(visible);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public record LayoutState(boolean compact, boolean narrow) {
    }
}
