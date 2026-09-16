package com.majortom.algorithms.visualization.common;

import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.render.runtime.RenderRuntime;
import com.majortom.algorithms.visualization.render.viewport.CameraState;
import com.majortom.algorithms.visualization.render.viewport.ViewportInsets;
import com.majortom.algorithms.visualization.render.viewport.ViewportSnapshot;

import javafx.animation.PauseTransition;
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
import javafx.util.Duration;

import net.kurobako.gesturefx.GesturePane;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Shared viewport infrastructure for project-owned Structure visualizers.
 *
 * <p>This class owns presentation-space concerns only: layers, GestureFX viewport behavior,
 * safe-area-aware fit/center and the viewport toolbar. It never interprets Structure/Event data.
 */
public final class VisualizationSurface extends StackPane {
    private static final double MIN_ZOOM = 0.10d;
    private static final double MAX_ZOOM = 8.00d;
    private static final double DEFAULT_ZOOM = 1.00d;
    private static final double MAX_AUTO_FIT_SCALE = 1.35d;
    private static final double TOOLBAR_ZOOM_FACTOR = 1.15d;
    private static final double AUTO_FIT_SETTLE_MS = 120.0d;
    private static final double AUTO_FIT_RESIZE_EPSILON = 0.75d;
    private static final double INITIAL_FIT_RETRY_MS = 40.0d;
    private static final int INITIAL_FIT_RETRY_LIMIT = 8;
    private static final Insets DEFAULT_SAFE_INSETS = new Insets(16.0d, 16.0d, 62.0d, 16.0d);

    private final Group edgeLayer = layer("visualization-edge-layer");
    private final Group nodeLayer = layer("visualization-node-layer");
    private final Group decorationLayer = layer("visualization-decoration-layer");
    private final Group worldPane = new Group(edgeLayer, nodeLayer, decorationLayer);
    private final GesturePane gesturePane = new GesturePane(worldPane);
    private final HBox viewportToolbar = new HBox(0.0d);
    private final Label zoomLabel = new Label();
    private final ReadOnlyDoubleWrapper zoom = new ReadOnlyDoubleWrapper(DEFAULT_ZOOM);
    private final PauseTransition autoFitSettleTransition =
            new PauseTransition(Duration.millis(AUTO_FIT_SETTLE_MS));
    private final PauseTransition initialFitRetryTransition =
            new PauseTransition(Duration.millis(INITIAL_FIT_RETRY_MS));

    private Insets safeInsets = DEFAULT_SAFE_INSETS;
    private Insets obstructionInsets = Insets.EMPTY;
    private boolean userViewportChanged;
    private double autoFitMinimumScale = MIN_ZOOM;
    private boolean programmaticViewportChange;
    private boolean fitQueued;
    private double queuedMinimumAutoScale = MIN_ZOOM;
    private boolean queuedInitialFit;
    private boolean initialAutoFitPending;

    /** True only after the visualizer has applied its first factual geometry. */
    private boolean initialLayoutReady = true;

    private boolean frameworkManagedCamera;
    private Consumer<ViewportSnapshot> viewportListener = ignored -> {};
    private int initialFitRetryCount;
    private double lastObservedWidth = -1.0d;
    private double lastObservedHeight = -1.0d;

    public VisualizationSurface() {
        getStyleClass().add("visualization-surface");
        worldPane.getStyleClass().add("visualization-world");
        configureGesturePane();
        configureToolbar();
        installInteractionTracking();
        installShortcuts();
        getChildren().setAll(gesturePane, viewportToolbar);
        autoFitSettleTransition.setOnFinished(event -> performSettledAutoFit());
        initialFitRetryTransition.setOnFinished(event -> requestInitialAutoFit());
        StackPane.setAlignment(viewportToolbar, Pos.BOTTOM_RIGHT);
        viewportToolbar.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        widthProperty()
                .addListener(
                        (observable, oldValue, newValue) ->
                                requestAutoFitAfterResize(true, newValue.doubleValue()));
        heightProperty()
                .addListener(
                        (observable, oldValue, newValue) ->
                                requestAutoFitAfterResize(false, newValue.doubleValue()));
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
        if (frameworkManagedCamera) {
            notifyViewportChanged();
        } else if (!userViewportChanged) {
            fitWithMinimumScale(autoFitMinimumScale);
        }
    }

    public Insets safeInsets() {
        return effectiveSafeInsets();
    }

    /**
     * Additional shell-level obstruction, composed with family safe insets without replacing them.
     */
    public void setObstructionInsets(Insets obstructionInsets) {
        Insets next = Objects.requireNonNull(obstructionInsets, "obstructionInsets");
        if (next.equals(this.obstructionInsets)) {
            return;
        }
        // Transient cards must not move the camera. Explicit fit/reset uses the current insets.
        this.obstructionInsets = next;
        if (frameworkManagedCamera) {
            notifyViewportChanged();
        }
    }

    /** Enables RenderFramework ownership of automatic resize/Fit decisions. */
    public void setFrameworkManagedCamera(boolean frameworkManagedCamera) {
        this.frameworkManagedCamera = frameworkManagedCamera;
        if (frameworkManagedCamera) {
            autoFitSettleTransition.stop();
            initialFitRetryTransition.stop();
            notifyViewportChanged();
        }
    }

    public void setViewportListener(Consumer<ViewportSnapshot> listener) {
        viewportListener = listener == null ? ignored -> {} : listener;
        if (frameworkManagedCamera) {
            notifyViewportChanged();
        }
    }

    public ViewportSnapshot viewportSnapshot() {
        Insets insets = effectiveSafeInsets();
        double width =
                gesturePane.getViewportWidth() > 0.0d ? gesturePane.getViewportWidth() : getWidth();
        double height =
                gesturePane.getViewportHeight() > 0.0d
                        ? gesturePane.getViewportHeight()
                        : getHeight();
        return new ViewportSnapshot(
                Math.max(0.0d, width),
                Math.max(0.0d, height),
                new ViewportInsets(
                        insets.getTop(), insets.getRight(), insets.getBottom(), insets.getLeft()));
    }

    public CameraState cameraState() {
        var worldTransform = worldPane.getLocalToParentTransform();
        return new CameraState(zoom(), worldTransform.getTx(), worldTransform.getTy());
    }

    public void applyCameraState(CameraState state) {
        Objects.requireNonNull(state, "state");
        ViewportSnapshot viewport = viewportSnapshot();
        if (!(viewport.width() > 0.0d) || !(viewport.height() > 0.0d)) {
            return;
        }
        // GesturePane normalizes a target whose layout bounds do not start at (0, 0) by
        // relocating that target. CameraState, however, is expressed in the visualizer's
        // factual world coordinates. Compensate for the target relocation before asking
        // GesturePane to centre its affine transform; otherwise non-zero world origins are
        // applied twice and content drifts toward the top/left of the viewport.
        double affineTranslateX = state.translateX() - worldPane.getLayoutX();
        double affineTranslateY = state.translateY() - worldPane.getLayoutY();
        Point2D targetAtViewportCentre =
                new Point2D(
                        (viewport.width() / 2.0d - affineTranslateX) / state.scale(),
                        (viewport.height() / 2.0d - affineTranslateY) / state.scale());
        runProgrammatic(
                () -> {
                    gesturePane.zoomTo(clamp(state.scale()), targetAtViewportCentre);
                    gesturePane.centreOn(targetAtViewportCentre);
                });
    }

    public void setWorldVisible(boolean visible) {
        worldPane.setOpacity(visible ? 1.0d : 0.0d);
        worldPane.setMouseTransparent(!visible);
        if (visible) {
            initialAutoFitPending = false;
            initialLayoutReady = true;
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

    /**
     * Keeps current scale and centers the world in the safe viewport rather than under overlays.
     */
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
     * Initial/automatic fit with a legacy preferred-scale hint. Complete-world visibility always
     * wins, so the hint is never allowed to enlarge content past the scale that fits the factual
     * world. This never marks the viewport as user-modified.
     */
    public void fitWithMinimumScale(double minimumAutoScale) {
        autoFitMinimumScale = clamp(minimumAutoScale);
        if (userViewportChanged) {
            return;
        }
        if (initialAutoFitPending) {
            if (initialLayoutReady) {
                initialFitRetryCount = 0;
                requestInitialAutoFit();
            }
            return;
        }
        requestSettledAutoFit();
    }

    /**
     * Signals that the visualizer has applied its first complete factual layout. Until this point
     * resize/CSS pulses are not allowed to fit or reveal the world.
     */
    public boolean markInitialLayoutReady(double minimumAutoScale) {
        autoFitMinimumScale = clamp(minimumAutoScale);
        if (!initialAutoFitPending) {
            return false;
        }
        initialLayoutReady = true;
        initialFitRetryCount = 0;
        requestInitialAutoFit();
        return true;
    }

    public void reset() {
        Bounds bounds = worldBounds();
        Point2D pivot;
        if (hasWorld(bounds)) {
            pivot = worldCenter(bounds);
        } else {
            pivot = new Point2D(0.0d, 0.0d);
        }
        runProgrammatic(
                () -> {
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

    /**
     * True while the world is intentionally hidden waiting for its first factual geometry and fit.
     */
    public boolean isAwaitingInitialLayout() {
        return initialAutoFitPending;
    }

    public void markViewportPristine() {
        userViewportChanged = false;
        autoFitSettleTransition.stop();
        initialFitRetryTransition.stop();
        initialFitRetryCount = 0;
        initialAutoFitPending = true;
        initialLayoutReady = false;
        worldPane.setOpacity(0.0d);
        worldPane.setMouseTransparent(true);
    }

    private void requestAutoFitAfterResize(boolean widthChanged, double value) {
        double previous;
        if (widthChanged) {
            previous = lastObservedWidth;
            lastObservedWidth = value;
        } else {
            previous = lastObservedHeight;
            lastObservedHeight = value;
        }
        if (previous >= 0.0d && Math.abs(value - previous) < AUTO_FIT_RESIZE_EPSILON) {
            return;
        }
        if (frameworkManagedCamera) {
            notifyViewportChanged();
            return;
        }
        if (initialAutoFitPending) {
            if (initialLayoutReady) {
                requestInitialAutoFit();
            }
            return;
        }
        requestSettledAutoFit();
    }

    private void requestInitialAutoFit() {
        if (userViewportChanged || !initialAutoFitPending || !initialLayoutReady) {
            return;
        }
        if (fitNow(true, autoFitMinimumScale)) {
            initialFitRetryTransition.stop();
            initialFitRetryCount = 0;
            return;
        }
        if (getScene() == null || initialFitRetryCount >= INITIAL_FIT_RETRY_LIMIT) {
            return;
        }
        initialFitRetryCount++;
        initialFitRetryTransition.playFromStart();
    }

    private void requestSettledAutoFit() {
        if (userViewportChanged) {
            return;
        }
        autoFitSettleTransition.playFromStart();
    }

    private void performSettledAutoFit() {
        if (userViewportChanged) {
            return;
        }
        if (!fitNow(true, autoFitMinimumScale) && initialAutoFitPending) {
            requestInitialAutoFit();
        }
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
        gesturePane
                .currentScaleProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
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
        zoomLabel.setAlignment(Pos.CENTER);
        Button zoomIn = button("+", "action.viewport.zoom_in", this::zoomIn);
        Button fit = localizedButton("action.viewport.fit", this::fit);
        Button center = localizedButton("action.viewport.center", this::center);
        Button reset = button("⌂", "action.viewport.reset", this::reset);
        reset.getStyleClass().add("viewport-toolbar-last");
        viewportToolbar.getChildren().setAll(zoomOut, zoomLabel, zoomIn, fit, center, reset);
    }

    private Button localizedButton(String textKey, Runnable action) {
        Button button = button("", textKey, action);
        button.textProperty()
                .bind(
                        javafx.beans.binding.Bindings.createStringBinding(
                                () -> I18N.text(textKey).toUpperCase(Locale.ROOT),
                                I18N.localeProperty()));
        return button;
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
        addEventFilter(
                KeyEvent.KEY_PRESSED,
                event -> {
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
            RenderRuntime.shared()
                    .fxExecutor()
                    .defer(
                            () -> {
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
        double availableWidth =
                Math.max(1.0d, viewportWidth - insets.getLeft() - insets.getRight());
        double availableHeight =
                Math.max(1.0d, viewportHeight - insets.getTop() - insets.getBottom());
        double scaleX;
        if (bounds.getWidth() <= 0.0d) {
            scaleX = MAX_ZOOM;
        } else {
            scaleX = availableWidth / bounds.getWidth();
        }
        double scaleY;
        if (bounds.getHeight() <= 0.0d) {
            scaleY = MAX_ZOOM;
        } else {
            scaleY = availableHeight / bounds.getHeight();
        }
        double fitScale = clamp(Math.min(scaleX, scaleY));
        double targetScale;
        if (initialFit) {
            // A readability preference must never make the factual world clip. If the whole
            // world only fits below the preferred scale, the complete-world fit wins.
            targetScale = Math.min(MAX_AUTO_FIT_SCALE, fitScale);
        } else {
            targetScale = fitScale;
        }
        Point2D center = worldCenter(bounds);
        Point2D targetCenter =
                new Point2D(
                        center.getX() + worldPane.getLayoutX(),
                        center.getY() + worldPane.getLayoutY());
        runProgrammatic(
                () -> {
                    gesturePane.zoomTo(targetScale, targetCenter);
                    centerOnSafeViewport(center);
                });
        if (!initialFit) {
            userViewportChanged = true;
        }
        revealWorldAfterInitialFit();
        return true;
    }

    private void revealWorldAfterInitialFit() {
        if (!initialAutoFitPending || !initialLayoutReady) {
            return;
        }
        initialAutoFitPending = false;
        worldPane.setOpacity(1.0d);
        worldPane.setMouseTransparent(false);
    }

    private void centerOnSafeViewport(Point2D worldCenter) {
        Point2D targetCenter =
                new Point2D(
                        worldCenter.getX() + worldPane.getLayoutX(),
                        worldCenter.getY() + worldPane.getLayoutY());
        gesturePane.centreOn(targetCenter);
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
            if (frameworkManagedCamera) {
                // The VIEWPORT intent keeps geometry untouched but lets the Session
                // remember the user's camera for later RESTORE.
                notifyViewportChanged();
            }
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

    private void notifyViewportChanged() {
        if (!frameworkManagedCamera) {
            return;
        }
        ViewportSnapshot snapshot = viewportSnapshot();
        if (snapshot.width() > 0.0d && snapshot.height() > 0.0d) {
            viewportListener.accept(snapshot);
        }
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
        return bounds != null
                && !bounds.isEmpty()
                && (bounds.getWidth() > 0.0d || bounds.getHeight() > 0.0d);
    }

    private static double clamp(double value) {
        return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, value));
    }
}
