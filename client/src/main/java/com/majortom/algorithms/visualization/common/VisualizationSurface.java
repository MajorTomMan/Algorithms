package com.majortom.algorithms.visualization.common;

import com.majortom.algorithms.visualization.international.I18N;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.geometry.Bounds;
import javafx.geometry.Dimension2D;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import net.kurobako.gesturefx.GesturePane;

import java.util.Locale;
import java.util.Objects;

/**
 * Shared viewport infrastructure for project-owned Structure visualizers.
 *
 * <p>This class owns presentation-space concerns only: layers, GestureFX viewport behavior,
 * safe-area-aware fit/center and the viewport toolbar. It never interprets Structure/Event data.</p>
 */
public final class VisualizationSurface extends StackPane {
    private static final double MIN_ZOOM = 0.10d;
    private static final double MAX_ZOOM = 8.00d;
    private static final double DEFAULT_ZOOM = 1.00d;
    private static final double MAX_AUTO_FIT_SCALE = 1.35d;
    private static final double TOOLBAR_ZOOM_FACTOR = 1.15d;
    private static final Insets DEFAULT_SAFE_INSETS = new Insets(16.0d, 16.0d, 62.0d, 16.0d);

    private final Group edgeLayer = layer("visualization-edge-layer");
    private final Group nodeLayer = layer("visualization-node-layer");
    private final Group decorationLayer = layer("visualization-decoration-layer");
    private final Group worldPane = new Group(edgeLayer, nodeLayer, decorationLayer);
    private final GesturePane gesturePane = new GesturePane(worldPane);
    private final HBox viewportToolbar = new HBox(0.0d);
    private final Label zoomLabel = new Label();
    private final ReadOnlyDoubleWrapper zoom = new ReadOnlyDoubleWrapper(DEFAULT_ZOOM);

    private Insets safeInsets = DEFAULT_SAFE_INSETS;
    private Insets obstructionInsets = Insets.EMPTY;
    private boolean userViewportChanged;
    private double autoFitMinimumScale = MIN_ZOOM;
    private boolean programmaticViewportChange;
    private boolean fitQueued;
    private double queuedMinimumAutoScale = MIN_ZOOM;
    private boolean queuedInitialFit;

    public VisualizationSurface() {
        getStyleClass().add("visualization-surface");
        worldPane.getStyleClass().add("visualization-world");
        configureGesturePane();
        configureToolbar();
        installInteractionTracking();
        installShortcuts();
        getChildren().setAll(gesturePane, viewportToolbar);
        StackPane.setAlignment(viewportToolbar, Pos.BOTTOM_RIGHT);
        viewportToolbar.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    }

    public Group edgeLayer() {
        return edgeLayer;
    }

    public Group nodeLayer() {
        return nodeLayer;
    }

    public Group decorationLayer() {
        return decorationLayer;
    }

    public double zoom() {
        return zoom.get();
    }

    public ReadOnlyDoubleProperty zoomProperty() {
        return zoom.getReadOnlyProperty();
    }

    public Bounds viewportBounds() {
        return gesturePane.getViewportBound();
    }

    public Bounds worldBounds() {
        return worldPane.getLayoutBounds();
    }

    /** Insets reserved for overlays/toolbars. Fit and center use the remaining usable viewport. */
    public void setSafeInsets(Insets safeInsets) {
        this.safeInsets = Objects.requireNonNull(safeInsets, "safeInsets");
        if (!userViewportChanged) {
            fitWithMinimumScale(autoFitMinimumScale);
        }
    }

    public Insets safeInsets() {
        return effectiveSafeInsets();
    }

    /** Additional shell-level obstruction, composed with family safe insets without replacing them. */
    public void setObstructionInsets(Insets obstructionInsets) {
        Insets next = Objects.requireNonNull(obstructionInsets, "obstructionInsets");
        if (next.equals(this.obstructionInsets)) {
            return;
        }
        this.obstructionInsets = next;
        if (!userViewportChanged) {
            fitWithMinimumScale(autoFitMinimumScale);
        }
    }

    public void zoomIn() {
        userViewportChanged = true;
        setZoomAroundViewportCentre(zoom() * TOOLBAR_ZOOM_FACTOR);
    }

    public void zoomOut() {
        userViewportChanged = true;
        setZoomAroundViewportCentre(zoom() / TOOLBAR_ZOOM_FACTOR);
    }

    /** Keeps current scale and centers the world in the safe viewport rather than under overlays. */
    public void center() {
        Bounds bounds = worldBounds();
        if (hasWorld(bounds)) {
            runProgrammatic(() -> centerOnSafeViewport(worldCenter(bounds)));
        }
        userViewportChanged = true;
    }

    /** Explicit FIT: always fits the complete world, even when that requires a small scale. */
    public void fit() {
        requestFit(false, MIN_ZOOM);
    }

    /**
     * Legacy pristine fit used by families that have not adopted a family-specific auto-fit floor yet.
     */
    public void fitIfPristine() {
        fitWithMinimumScale(MIN_ZOOM);
    }

    /**
     * Initial/automatic fit with a readability floor. This never marks the viewport as user-modified.
     * Explicit {@link #fit()} ignores this floor and can fit the complete world at any supported scale.
     */
    public void fitWithMinimumScale(double minimumAutoScale) {
        autoFitMinimumScale = clamp(minimumAutoScale);
        if (userViewportChanged) {
            return;
        }
        requestFit(true, autoFitMinimumScale);
    }

    public void reset() {
        Bounds bounds = worldBounds();
        Point2D pivot = hasWorld(bounds) ? worldCenter(bounds) : new Point2D(0.0d, 0.0d);
        runProgrammatic(() -> {
            gesturePane.zoomTo(DEFAULT_ZOOM, pivot);
            if (hasWorld(bounds)) {
                centerOnSafeViewport(pivot);
            }
        });
        userViewportChanged = true;
    }

    public boolean isUserViewportChanged() {
        return userViewportChanged;
    }

    public void markViewportPristine() {
        userViewportChanged = false;
    }

    private void configureGesturePane() {
        gesturePane.getStyleClass().add("visualization-gesture-pane");
        gesturePane.setMinScale(MIN_ZOOM);
        gesturePane.setMaxScale(MAX_ZOOM);
        gesturePane.setBindScale(true);
        gesturePane.setFitWidth(false);
        gesturePane.setFitHeight(false);
        gesturePane.setFitMode(GesturePane.FitMode.UNBOUNDED);
        gesturePane.setScrollMode(GesturePane.ScrollMode.ZOOM);
        gesturePane.setScrollBarPolicy(GesturePane.ScrollBarPolicy.NEVER);
        gesturePane.currentScaleProperty().addListener((observable, oldValue, newValue) -> {
            zoom.set(newValue.doubleValue());
            updateZoomLabel();
        });
        updateZoomLabel();
    }

    private void configureToolbar() {
        viewportToolbar.getStyleClass().add("viewport-toolbar");
        viewportToolbar.setAlignment(Pos.CENTER);
        viewportToolbar.setPickOnBounds(false);

        Button zoomOut = button("−", "action.viewport.zoom_out", this::zoomOut);
        zoomLabel.getStyleClass().add("viewport-zoom-label");
        zoomLabel.setMinWidth(46.0d);
        zoomLabel.setAlignment(Pos.CENTER);
        Button zoomIn = button("+", "action.viewport.zoom_in", this::zoomIn);
        Button fit = button("FIT", "action.viewport.fit", this::fit);
        Button center = button("CENTER", "action.viewport.center", this::center);
        Button reset = button("⌂", "action.viewport.reset", this::reset);
        reset.getStyleClass().add("viewport-toolbar-last");
        viewportToolbar.getChildren().setAll(zoomOut, zoomLabel, zoomIn, fit, center, reset);
    }

    private Button button(String text, String tooltipKey, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("viewport-toolbar-button");
        Tooltip tooltip = new Tooltip();
        tooltip.textProperty().bind(I18N.createStringBinding(tooltipKey));
        button.setTooltip(tooltip);
        button.setOnAction(event -> action.run());
        return button;
    }

    private void installInteractionTracking() {
        gesturePane.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> markUserGesture());
        gesturePane.addEventFilter(ScrollEvent.SCROLL, event -> markUserGesture());
        gesturePane.addEventFilter(ZoomEvent.ZOOM, event -> markUserGesture());
    }

    private void installShortcuts() {
        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (!event.isShortcutDown()) {
                return;
            }
            if (event.getCode() == KeyCode.DIGIT0 || event.getCode() == KeyCode.NUMPAD0) {
                fit();
                event.consume();
            }
        });
    }

    private void requestFit(boolean initialFit, double minimumAutoScale) {
        if (fitNow(initialFit, minimumAutoScale)) {
            return;
        }
        if (getScene() != null && !fitQueued) {
            fitQueued = true;
            queuedInitialFit = initialFit;
            queuedMinimumAutoScale = minimumAutoScale;
            Platform.runLater(() -> {
                fitQueued = false;
                if (!queuedInitialFit || !userViewportChanged) {
                    fitNow(queuedInitialFit, queuedMinimumAutoScale);
                }
            });
        }
    }

    private boolean fitNow(boolean initialFit, double minimumAutoScale) {
        Bounds bounds = worldBounds();
        double viewportWidth = gesturePane.getViewportWidth();
        double viewportHeight = gesturePane.getViewportHeight();
        if (!hasWorld(bounds) || viewportWidth <= 0.0d || viewportHeight <= 0.0d) {
            return false;
        }

        Insets insets = effectiveSafeInsets();
        double availableWidth = Math.max(1.0d, viewportWidth - insets.getLeft() - insets.getRight());
        double availableHeight = Math.max(1.0d, viewportHeight - insets.getTop() - insets.getBottom());
        double scaleX = bounds.getWidth() <= 0.0d ? MAX_ZOOM : availableWidth / bounds.getWidth();
        double scaleY = bounds.getHeight() <= 0.0d ? MAX_ZOOM : availableHeight / bounds.getHeight();
        double fitScale = clamp(Math.min(scaleX, scaleY));
        double targetScale = initialFit
                ? Math.min(MAX_AUTO_FIT_SCALE, Math.max(fitScale, minimumAutoScale))
                : fitScale;
        Point2D center = worldCenter(bounds);
        runProgrammatic(() -> {
            gesturePane.zoomTo(targetScale, center);
            centerOnSafeViewport(center);
        });
        if (!initialFit) {
            userViewportChanged = true;
        }
        return true;
    }

    private void centerOnSafeViewport(Point2D worldCenter) {
        gesturePane.centreOn(worldCenter);
        Insets insets = effectiveSafeInsets();
        double xOffset = (insets.getRight() - insets.getLeft()) / 2.0d;
        double yOffset = (insets.getBottom() - insets.getTop()) / 2.0d;
        if (Math.abs(xOffset) > 0.01d || Math.abs(yOffset) > 0.01d) {
            gesturePane.translateBy(new Dimension2D(xOffset, yOffset));
        }
    }

    private void setZoomAroundViewportCentre(double requestedScale) {
        double targetScale = clamp(requestedScale);
        Point2D pivot = gesturePane.targetPointAtViewportCentre();
        runProgrammatic(() -> gesturePane.zoomTo(targetScale, pivot));
    }

    private void markUserGesture() {
        if (!programmaticViewportChange) {
            userViewportChanged = true;
        }
    }

    private void runProgrammatic(Runnable action) {
        programmaticViewportChange = true;
        try {
            action.run();
        } finally {
            programmaticViewportChange = false;
        }
    }

    private void updateZoomLabel() {
        zoomLabel.setText(String.format(Locale.ROOT, "%.0f%%", zoom() * 100.0d));
    }


    private Insets effectiveSafeInsets() {
        return new Insets(
                Math.max(safeInsets.getTop(), obstructionInsets.getTop()),
                Math.max(safeInsets.getRight(), obstructionInsets.getRight()),
                Math.max(safeInsets.getBottom(), obstructionInsets.getBottom()),
                Math.max(safeInsets.getLeft(), obstructionInsets.getLeft()));
    }

    private static Group layer(String styleClass) {
        Group layer = new Group();
        layer.getStyleClass().add("visualization-layer");
        layer.getStyleClass().add(styleClass);
        return layer;
    }

    private static Point2D worldCenter(Bounds bounds) {
        return new Point2D(
                bounds.getMinX() + bounds.getWidth() / 2.0d,
                bounds.getMinY() + bounds.getHeight() / 2.0d);
    }

    private static boolean hasWorld(Bounds bounds) {
        return bounds != null && !bounds.isEmpty()
                && (bounds.getWidth() > 0.0d || bounds.getHeight() > 0.0d);
    }

    private static double clamp(double value) {
        return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, value));
    }
}
