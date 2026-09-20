package com.majortom.algorithms.visualization.layout;

import atlantafx.base.theme.Styles;
import com.majortom.algorithms.visualization.navigation.FamilyNavigator;
import com.majortom.algorithms.visualization.render.fx.FxDispatch;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
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
  private static final double MIN_FAMILY_WIDTH = 148.0d;
  private static final double MAX_FAMILY_WIDTH = 260.0d;
  private static final double MIN_CONTROL_WIDTH = 260.0d;
  private static final double MIN_INSPECTOR_WIDTH = 300.0d;
  private static final double CONTROL_VERTICAL_PADDING = 12.0d;
  private static final String INSPECTOR_OVERFLOW_ARROW = "inspector-overflow-arrow";

  private final BorderPane root;
  private final WorkbenchHeader header;
  private final Region familyNavigator;
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
  private Runnable refreshRequester = this::scheduleLegacyRefresh;

  public WorkbenchUiFramework(BorderPane root, WorkbenchHeader header, Region familyNavigator,
      Region structureControlRail, Region algorithmControlRail, Region practiceControlRail,
      Region structureInspector, Region algorithmInspector, Region structureOverlay,
      Region currentStepOverlay, Region algorithmOverlay, TabPane structureTabs,
      TabPane algorithmTabs, PlaybackToolbar playbackToolbar, Region structureHistoryDock,
      Node structureHistoryDetails) {
    this.root = root;
    this.header = header;
    this.familyNavigator = familyNavigator;
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
    // TabPaneSkin can recreate its overflow node on a skin change. Reapply
    // only the inspector's semantic paint class, never alter TabPane behavior.
    if (structureTabs != null) {
      structureTabs.skinProperty().addListener((observable, oldSkin, newSkin)
          -> FxDispatch.defer(() -> styleInspectorOverflowArrow(structureTabs)));
    }
    if (algorithmTabs != null) {
      algorithmTabs.skinProperty().addListener((observable, oldSkin, newSkin)
          -> FxDispatch.defer(() -> styleInspectorOverflowArrow(algorithmTabs)));
    }
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
    double navigationFontSize = shellFont().getSize();
    applyFamilyTypography(familyNavigator, navigationFontSize);

    double familyWidth = familyRailContentWidth();
    double controlWidth = clamp(320.0d * scale, MIN_CONTROL_WIDTH, 460.0d);
    double inspectorWidth = Math.max(MIN_INSPECTOR_WIDTH,
        Math.max(inspectorTextWidth(structureTabs), inspectorTextWidth(algorithmTabs)));
    inspectorWidth = clamp(
        Math.max(inspectorWidth, 360.0d * Math.min(scale, 1.30d)), MIN_INSPECTOR_WIDTH, 500.0d);

    double requiredBody = familyWidth + controlWidth + inspectorWidth + MIN_CANVAS_WIDTH;
    boolean compact =
        width > 0.0d && (width < requiredBody + 80.0d || height < 820.0d * Math.min(scale, 1.18d));
    boolean narrow = width > 0.0d
        && (width < familyWidth + controlWidth + MIN_CANVAS_WIDTH + 80.0d || height < 650.0d);

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

    layoutHeader();
    setFixedWidth(structureControlRail, controlWidth);
    setFixedWidth(algorithmControlRail, controlWidth);
    setFixedWidth(practiceControlRail, practiceControlWidth(scale, compact, narrow));
    setFixedWidth(structureInspector, inspectorWidth);
    setFixedWidth(algorithmInspector, inspectorWidth);
    setOverlayWidth(structureOverlay, overlayWidth(scale, compact, narrow));
    setOverlayWidth(currentStepOverlay, overlayWidth(scale, compact, narrow));
    setOverlayWidth(algorithmOverlay, overlayWidth(scale, compact, narrow));
    setVisibleManaged(algorithmInspector, !narrow);
    configureInspectorTabs(structureTabs);
    configureInspectorTabs(algorithmTabs);
    applyControlDensity(root, compact);
    // Density classes and pseudo-classes must settle before Family Rail geometry is
    // applied. Family navigation is identity/navigation chrome, not a compact form
    // control, so it keeps the legacy roomy row rhythm under the new layout owner.
    if (root.getScene() != null) {
      root.applyCss();
    }
    // Single-line form selectors must not inherit the popup skin's preferred
    // height. Apply this only after theme/typography and density CSS settle.
    WorkbenchFormLayout.refreshMetrics(root);
    WorkbenchFormLayout.synchronizeChoiceHeights(root, shellFont());
    layoutFamilyRail(familyNavigator, familyWidth);
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

  /** The shell reports changes; its owner chooses when to execute the refresh. */
  public void setRefreshRequester(Runnable requester) {
    refreshRequester = java.util.Objects.requireNonNull(requester, "requester");
  }

  public void scheduleRefresh() {
    refreshRequester.run();
  }

  /** Compatibility path for a standalone shell without a render coordinator. */
  private void scheduleLegacyRefresh() {
    if (refreshScheduled) return;
    refreshScheduled = true;
    FxDispatch.defer(() -> {
      refreshScheduled = false;
      refresh();
    });
  }

  private void layoutHeader() {
    if (header == null) {
      return;
    }
    header.syncTypographyFromContent();
    releaseHeight(header);
    header.requestLayout();
  }

  private double familyRailContentWidth() {
    double width = familyRailContentWidth(familyNavigator);
    if (width <= 0.0d) {
      return MIN_FAMILY_WIDTH;
    }
    return Math.ceil(width);
  }

  private double familyRailContentWidth(Region rail) {
    if (rail == null || !rail.isManaged()) {
      return 0.0d;
    }
    if (rail instanceof FamilyNavigator navigator) {
      return navigator.preferredRailWidth();
    }
    return rail == null ? 0.0d : rail.prefWidth(-1.0d);
  }

  private static void applyFamilyTypography(Region rail, double fontSize) {
    if (rail instanceof FamilyNavigator navigator) {
      navigator.applyTypography(fontSize);
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

  private void configureInspectorTabs(TabPane tabs) {
    if (tabs == null || tabs.getTabs().isEmpty()) {
      return;
    }
    // tabMinWidth is the *label* width; JavaFX adds the tab's CSS padding on
    // either side. The old min-width calculation counted that padding twice,
    // forcing overflow even when all titles would fit naturally. Let the Skin
    // measure each localized label; it will expose its native overflow menu
    // only when the labels really cannot fit the available inspector width.
    tabs.setTabMinWidth(0.0d);
    tabs.setTabMaxWidth(132.0d);
    styleInspectorOverflowArrow(tabs);
  }

  private static void styleInspectorOverflowArrow(TabPane tabs) {
    if (tabs == null || tabs.getSkin() == null) {
      return;
    }
    // JavaFX's skin-owned arrow is not reliably matched by ancestor-scoped
    // author CSS under AtlantaFX. Give that single region a semantic class;
    // a direct author rule then wins without recoloring unrelated arrows.
    Node arrow = tabs.lookup(".control-buttons-tab .arrow");
    if (arrow != null && !arrow.getStyleClass().contains(INSPECTOR_OVERFLOW_ARROW)) {
      arrow.getStyleClass().add(INSPECTOR_OVERFLOW_ARROW);
    }
  }

  private double fontScale() {
    Font font = shellFont();
    return clamp(font.getSize() / BASE_MODE_FONT, 0.82d, 1.50d);
  }

  private Font shellFont() {
    return header == null ? Font.getDefault() : header.syncTypographyFromContent();
  }

  private void normalizeShellControls(Node node) {
    if (node == null || isVisualizationSubtree(node)) {
      return;
    }
    // The form owns all three vertical bounds for its single-line controls.
    // Shell chrome retains its independent, font-aware minimum height.
    if (!WorkbenchFormLayout.ownsControl(node)) {
      if (node instanceof Button button) {
        button.setMinHeight(Math.max(32.0d, textControlHeight(button)));
      } else if (node instanceof TextInputControl input) {
        input.setMinHeight(Math.max(32.0d, textControlHeight(input)));
      } else if (node instanceof ComboBoxBase<?> combo) {
        combo.setMinHeight(Math.max(32.0d, textControlHeight(combo)));
        combo.setMaxHeight(Region.USE_PREF_SIZE);
      }
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
    return styles.contains("lab-canvas") || styles.contains("visualization-viewport")
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
    return parent.getChildrenUnmodifiable()
        .stream()
        .filter(Node::isManaged)
        .mapToDouble(node -> node.prefHeight(-1.0d))
        .max()
        .orElse(42.0d);
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
    structureHistoryDock.getStyleClass().add(
        structureHistoryExpanded ? "history-expanded" : "history-collapsed");

    releaseHeight(structureHistoryDock);
    double lineHeight = mixedLineHeight(shellFont());
    double headerHeight =
        Math.max(lineHeight + 20.0d, firstManagedChildPrefHeight(structureHistoryDock));
    if (!structureHistoryExpanded) {
      setFixedHeight(structureHistoryDock, Math.ceil(headerHeight));
      return;
    }

    double rootHeight = root == null ? 0.0d : root.getHeight();
    double minimumDetails = lineHeight * 3.4d + 24.0d;
    double desiredDetails = structureHistoryDetails == null
        ? minimumDetails
        : Math.max(minimumDetails, structureHistoryDetails.prefHeight(-1.0d));
    double viewportBudget =
        rootHeight > 0.0d ? rootHeight * (compact ? 0.22d : 0.28d) : lineHeight * 7.0d + 44.0d;
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
    } else if (node instanceof Button
        || node instanceof ComboBoxBase<?> || node instanceof TextInputControl
        || node instanceof Spinner<?> || node instanceof Slider) {
      setOwnedClass(node, Styles.SMALL, compact, ownedSmall);
    }
    if (node instanceof Parent parent) {
      for (Node child : parent.getChildrenUnmodifiable()) {
        applyControlDensity(child, compact);
      }
    }
  }

  private static void setOwnedClass(
      Node node, String styleClass, boolean enabled, Set<Node> owned) {
    if (enabled) {
      if (!node.getStyleClass().contains(styleClass)) {
        node.getStyleClass().add(styleClass);
        owned.add(node);
      }
    } else if (owned.remove(node)) {
      node.getStyleClass().remove(styleClass);
    }
  }

  private static void layoutFamilyRail(Region rail, double width) {
    if (rail == null) {
      return;
    }
    setFixedWidth(rail, width);
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

  public record LayoutState(boolean compact, boolean narrow) {}
}
