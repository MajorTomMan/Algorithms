package com.majortom.algorithms.visualization.common;

import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.render.api.BoundsSnapshot;
import com.majortom.algorithms.visualization.render.fx.FxSurfaceAdapter;
import com.majortom.algorithms.visualization.render.viewport.CameraPolicy;
import com.majortom.algorithms.visualization.render.viewport.CameraScale;
import com.majortom.algorithms.visualization.render.viewport.CameraState;
import com.majortom.algorithms.visualization.render.viewport.ViewportInsets;
import com.majortom.algorithms.visualization.render.viewport.ViewportSnapshot;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
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
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import net.kurobako.gesturefx.GesturePane;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Shared viewport infrastructure for project-owned Structure visualizers.
 *
 * <p>This class owns presentation-space concerns only: layers, GestureFX viewport behavior,
 * safe-area-aware fit/center and the viewport toolbar. It never interprets Structure/Event data.</p>
 */
public final class VisualizationSurface extends StackPane implements FxSurfaceAdapter {
    private static final Insets DEFAULT_SAFE_INSETS = new Insets(16.0d);

    private final Group edgeLayer = layer("visualization-edge-layer");
    private final Group nodeLayer = layer("visualization-node-layer");
    private final Group decorationLayer = layer("visualization-decoration-layer");
    // A Pane owns an explicit factual extent. Child decorations no longer participate in
    // target layout bounds, so GesturePane cannot renormalize the camera after a CSS pulse.
    // The resizable Pane owns the explicit factual extent, while the outer Group is the
    // non-resizable GestureFX target. GesturePaneSkin lays out resizable content on every pulse;
    // targeting the Pane directly would therefore mutate target geometry after camera commit.
    private final Pane worldPane = new Pane(edgeLayer, nodeLayer, decorationLayer);
    private final Group worldTarget = new Group(worldPane);
    private final GesturePane gesturePane = new GesturePane(worldTarget);
    private final HBox viewportToolbar = new HBox(0.0d);
    private final Label zoomLabel = new Label();
    private final ReadOnlyDoubleWrapper zoom = new ReadOnlyDoubleWrapper(CameraScale.DEFAULT);

    private Bounds factualWorldBounds;
    private Insets safeInsets = DEFAULT_SAFE_INSETS;
    private Insets obstructionInsets = Insets.EMPTY;
    private boolean userViewportChanged;
    private boolean programmaticViewportChange;
    private boolean frameworkManagedCamera;
    private Consumer<ViewportSnapshot> viewportListener = ignored -> { };
    private Consumer<CameraPolicy> cameraCommandListener = ignored -> { };

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
        viewportToolbar.heightProperty().addListener((observable, oldValue, newValue) -> notifyViewportChanged());
        widthProperty().addListener((observable, oldValue, newValue) -> notifyViewportChanged());
        heightProperty().addListener((observable, oldValue, newValue) -> notifyViewportChanged());
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
        return factualWorldBounds;
    }

    /**
     * Freezes the factual world extent produced by LayoutEngine. Decorations and labels may draw
     * outside it, but they cannot alter camera geometry or GesturePane target normalization.
     */
    public void setPrimaryContentBounds(BoundsSnapshot bounds) {
        Objects.requireNonNull(bounds, "bounds");
        double width = Math.max(1.0d, Math.max(bounds.width(), bounds.maxX()));
        double height = Math.max(1.0d, Math.max(bounds.height(), bounds.maxY()));
        factualWorldBounds = bounds.isEmpty()
                ? null
                : new BoundingBox(bounds.minX(), bounds.minY(), bounds.width(), bounds.height());
        worldPane.setMinSize(width, height);
        worldPane.setPrefSize(width, height);
        worldPane.setMaxSize(width, height);
        worldPane.resize(width, height);
    }

    /** Insets reserved for overlays/toolbars. Fit and center use the remaining usable viewport. */
    public void setSafeInsets(Insets safeInsets) {
        this.safeInsets = Objects.requireNonNull(safeInsets, "safeInsets");
        if (frameworkManagedCamera) notifyViewportChanged();
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
        // Transient cards must not move the camera. Explicit fit/reset uses the current insets.
        this.obstructionInsets = next;
        if (frameworkManagedCamera) {
            notifyViewportChanged();
        }
    }


    /** Enables RenderFramework ownership of automatic resize/Fit decisions. */
    public void setFrameworkManagedCamera(boolean frameworkManagedCamera) {
        this.frameworkManagedCamera = frameworkManagedCamera;
        if (frameworkManagedCamera) notifyViewportChanged();
    }

    @Override
    public void setViewportListener(Consumer<ViewportSnapshot> listener) {
        viewportListener = listener == null ? ignored -> { } : listener;
        if (frameworkManagedCamera) {
            notifyViewportChanged();
        }
    }

    @Override
    public void setCameraCommandListener(Consumer<CameraPolicy> listener) {
        cameraCommandListener = listener == null ? ignored -> { } : listener;
    }

    @Override
    public ViewportSnapshot viewportSnapshot() {
        Insets insets = effectiveSafeInsets();
        double width = gesturePane.getViewportWidth() > 0.0d ? gesturePane.getViewportWidth() : getWidth();
        double height = gesturePane.getViewportHeight() > 0.0d ? gesturePane.getViewportHeight() : getHeight();
        return new ViewportSnapshot(
                Math.max(0.0d, width),
                Math.max(0.0d, height),
                new ViewportInsets(insets.getTop(), insets.getRight(), insets.getBottom(), insets.getLeft()));
    }

    @Override
    public CameraState cameraState() {
        var affine = gesturePane.getAffine();
        return new CameraState(zoom(), affine.getTx(), affine.getTy());
    }

    @Override
    public void applyCameraState(CameraState state) {
        Objects.requireNonNull(state, "state");
        ViewportSnapshot viewport = viewportSnapshot();
        if (!(viewport.width() > 0.0d) || !(viewport.height() > 0.0d)) {
            return;
        }
        // CameraState is defined in factual world coordinates and GestureFX's affine transforms
        // exactly those target coordinates. Keep the target bounds stable (worldTarget is a
        // non-resizable Group) and derive the target point that must sit at viewport centre.
        Point2D targetAtViewportCentre = new Point2D(
                (viewport.width() / 2.0d - state.translateX()) / state.scale(),
                (viewport.height() / 2.0d - state.translateY()) / state.scale());
        runProgrammatic(() -> {
            gesturePane.zoomTo(clamp(state.scale()), targetAtViewportCentre);
            gesturePane.centreOn(targetAtViewportCentre);
        });
    }

    @Override
    public void applyPrimaryContentBounds(BoundsSnapshot bounds) {
        setPrimaryContentBounds(bounds);
    }

    @Override
    public boolean userControlledCamera() {
        return userViewportChanged;
    }

    @Override
    public void prepareInitialFrame() {
        markViewportPristine();
    }

    @Override
    public void revealFrame() {
        setWorldVisible(true);
    }

    public void setWorldVisible(boolean visible) {
        worldPane.setOpacity(visible ? 1.0d : 0.0d);
        worldPane.setMouseTransparent(!visible);
    }

    public void zoomIn() {
        userViewportChanged = true;
        setZoomAroundViewportCentre(zoom() * CameraScale.TOOLBAR_FACTOR);
        notifyViewportChanged();
    }

    public void zoomOut() {
        userViewportChanged = true;
        setZoomAroundViewportCentre(zoom() / CameraScale.TOOLBAR_FACTOR);
        notifyViewportChanged();
    }

    /** Explicit camera commands are resolved by RenderFramework -> CameraManager. */
    public void center() {
        requestCamera(CameraPolicy.CENTER);
    }

    public void fit() {
        requestCamera(CameraPolicy.FIT_CONTENT);
    }

    public void reset() {
        requestCamera(CameraPolicy.RESET);
    }

    private void requestCamera(CameraPolicy policy) {
        userViewportChanged = true;
        cameraCommandListener.accept(policy);
    }

    public boolean isUserViewportChanged() {
        return userViewportChanged;
    }

    public void markViewportPristine() {
        userViewportChanged = false;
        worldPane.setOpacity(0.0d);
        worldPane.setMouseTransparent(true);
    }

    private void configureGesturePane() {
        gesturePane.getStyleClass().add("visualization-gesture-pane");
        gesturePane.setMinScale(CameraScale.MIN);
        gesturePane.setMaxScale(CameraScale.MAX_MANUAL);
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
        button.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(
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
        // An overlay toolbar does not occupy parent layout space. Reserve its measured
        // height plus the normal margin, retaining larger per-structure bottom insets.
        double toolbarHeight = viewportToolbar.getHeight() > 0.0d
                ? viewportToolbar.getHeight() : viewportToolbar.prefHeight(-1.0d);
        double bottom = Math.max(safeInsets.getBottom(),
                Math.max(0.0d, toolbarHeight) + DEFAULT_SAFE_INSETS.getBottom());
        return new Insets(
                Math.max(safeInsets.getTop(), obstructionInsets.getTop()),
                Math.max(safeInsets.getRight(), obstructionInsets.getRight()),
                Math.max(bottom, obstructionInsets.getBottom()),
                Math.max(safeInsets.getLeft(), obstructionInsets.getLeft()));
    }

    private static Group layer(String styleClass) {
        Group layer = new Group();
        layer.getStyleClass().add("visualization-layer");
        layer.getStyleClass().add(styleClass);
        return layer;
    }

    private static double clamp(double value) {
        return Math.max(CameraScale.MIN, Math.min(CameraScale.MAX_MANUAL, value));
    }
}
