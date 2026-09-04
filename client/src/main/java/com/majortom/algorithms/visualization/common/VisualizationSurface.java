package com.majortom.algorithms.visualization.common;

import com.majortom.algorithms.visualization.international.I18N;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.geometry.Bounds;
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
import javafx.scene.layout.StackPane;
import net.kurobako.gesturefx.GesturePane;

import java.util.Locale;

/**
 * Shared viewport infrastructure for the Workbench visualization area.
 *
 * <p>The surface owns only presentation-space concerns: three ordered JavaFX
 * layers plus GestureFX zoom/pan/fit/center/reset behavior. It deliberately has
 * no knowledge of structure families, events, algorithms or view-state
 * semantics.</p>
 */
public final class VisualizationSurface extends StackPane {
    private static final double MIN_ZOOM = 0.10d;
    private static final double MAX_ZOOM = 8.00d;
    private static final double DEFAULT_ZOOM = 1.00d;
    private static final double TOOLBAR_ZOOM_FACTOR = 1.15d;
    private static final double FIT_PADDING = 36.0d;

    private final Group edgeLayer = layer("visualization-edge-layer");
    private final Group nodeLayer = layer("visualization-node-layer");
    private final Group decorationLayer = layer("visualization-decoration-layer");
    private final Group worldPane = new Group(edgeLayer, nodeLayer, decorationLayer);
    private final GesturePane gesturePane = new GesturePane(worldPane);
    private final HBox viewportToolbar = new HBox(5.0d);
    private final Label zoomLabel = new Label();
    private final ReadOnlyDoubleWrapper zoom = new ReadOnlyDoubleWrapper(DEFAULT_ZOOM);

    private boolean userViewportChanged;
    private boolean programmaticViewportChange;
    private boolean fitQueued;

    public VisualizationSurface() {
        getStyleClass().add("visualization-surface");
        worldPane.getStyleClass().add("visualization-world");
        configureGesturePane();
        configureToolbar();
        installInteractionTracking();
        installShortcuts();
        getChildren().setAll(gesturePane, viewportToolbar);
        StackPane.setAlignment(viewportToolbar, Pos.TOP_RIGHT);
    }

    /** Layer rendered behind nodes. Intended for EdgeView / Path instances. */
    public Group edgeLayer() {
        return edgeLayer;
    }

    /** Main visual-element layer. */
    public Group nodeLayer() {
        return nodeLayer;
    }

    /** Foreground annotations, transient markers and presentation decorations. */
    public Group decorationLayer() {
        return decorationLayer;
    }

    /** Current GestureFX scale, where {@code 1.0 == 100% design scale}. */
    public double zoom() {
        return zoom.get();
    }

    public ReadOnlyDoubleProperty zoomProperty() {
        return zoom.getReadOnlyProperty();
    }

    /** Current viewport bounds in surface coordinates. */
    public Bounds viewportBounds() {
        return gesturePane.getViewportBound();
    }

    /** Bounds of the complete project-owned visualization world. */
    public Bounds worldBounds() {
        return worldPane.getLayoutBounds();
    }

    /** Zooms one toolbar step around the current viewport centre. */
    public void zoomIn() {
        userViewportChanged = true;
        setZoomAroundViewportCentre(zoom() * TOOLBAR_ZOOM_FACTOR);
    }

    /** Zooms one toolbar step around the current viewport centre. */
    public void zoomOut() {
        userViewportChanged = true;
        setZoomAroundViewportCentre(zoom() / TOOLBAR_ZOOM_FACTOR);
    }

    /** Keeps the current scale and centres the world in the viewport. */
    public void center() {
        Bounds bounds = worldBounds();
        if (hasWorld(bounds)) {
            runProgrammatic(() -> gesturePane.centreOn(worldCenter(bounds)));
        }
        userViewportChanged = true;
    }

    /** Fits the complete world into the current viewport while preserving aspect ratio. */
    public void fit() {
        fit(false);
    }

    /**
     * Initial-load fit. After the user has manipulated the viewport this method
     * becomes a no-op so ordinary data updates cannot unexpectedly move the view.
     */
    public void fitIfPristine() {
        if (!userViewportChanged) {
            fit(true);
        }
    }

    /** Restores 100% design scale and centres the world. */
    public void reset() {
        Bounds bounds = worldBounds();
        Point2D pivot = hasWorld(bounds) ? worldCenter(bounds) : new Point2D(0.0d, 0.0d);
        runProgrammatic(() -> {
            gesturePane.zoomTo(DEFAULT_ZOOM, pivot);
            if (hasWorld(bounds)) {
                gesturePane.centreOn(pivot);
            }
        });
        userViewportChanged = true;
    }

    /** True once wheel/drag/pinch or a toolbar viewport action has been used. */
    public boolean isUserViewportChanged() {
        return userViewportChanged;
    }

    /**
     * Marks the viewport as pristine for a newly mounted visualization world.
     * This does not itself move or scale the viewport.
     */
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
        gesturePane.setScrollMode(GesturePane.ScrollMode.PAN);
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
        zoomLabel.setMinWidth(54.0d);
        zoomLabel.setAlignment(Pos.CENTER);
        Button zoomIn = button("+", "action.viewport.zoom_in", this::zoomIn);
        Button center = button("C", "action.viewport.center", this::center);
        Button fit = button("Fit", "action.viewport.fit", this::fit);
        Button reset = button("1:1", "action.viewport.reset", this::reset);
        viewportToolbar.getChildren().setAll(zoomOut, zoomLabel, zoomIn, center, fit, reset);
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

    private void fit(boolean initialFit) {
        if (fitNow(initialFit)) {
            return;
        }
        if (getScene() != null && !fitQueued) {
            fitQueued = true;
            Platform.runLater(() -> {
                fitQueued = false;
                if (!initialFit || !userViewportChanged) {
                    fitNow(initialFit);
                }
            });
        }
    }

    private boolean fitNow(boolean initialFit) {
        Bounds bounds = worldBounds();
        double viewportWidth = gesturePane.getViewportWidth();
        double viewportHeight = gesturePane.getViewportHeight();
        if (!hasWorld(bounds) || viewportWidth <= 0.0d || viewportHeight <= 0.0d) {
            return false;
        }
        double availableWidth = Math.max(1.0d, viewportWidth - FIT_PADDING * 2.0d);
        double availableHeight = Math.max(1.0d, viewportHeight - FIT_PADDING * 2.0d);
        double scaleX = bounds.getWidth() <= 0.0d ? MAX_ZOOM : availableWidth / bounds.getWidth();
        double scaleY = bounds.getHeight() <= 0.0d ? MAX_ZOOM : availableHeight / bounds.getHeight();
        double targetScale = clamp(Math.min(scaleX, scaleY));
        Point2D center = worldCenter(bounds);
        runProgrammatic(() -> {
            gesturePane.zoomTo(targetScale, center);
            gesturePane.centreOn(center);
        });
        if (!initialFit) {
            userViewportChanged = true;
        }
        return true;
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
