package com.majortom.algorithms.visualization;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.Glow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * 视觉呈现组件基类
 * 承载《乱》高饱和色彩体系与核心渲染调度
 */
public abstract class BaseVisualizer<S> extends StackPane {

    /* Canonical workbench palette: black, white, red, blue, yellow and gray. */
    public static final Color RAN_BLACK = Color.web("#08090A");
    public static final Color RAN_WHITE = Color.WHITE;
    public static final Color RAN_RED = Color.web("#F51B23");
    public static final Color RAN_BLUE = Color.web("#1769D3");
    public static final Color RAN_YELLOW = Color.web("#F5B400");
    public static final Color RAN_GRAY = Color.web("#444444");

    protected final Canvas canvas;
    protected final GraphicsContext gc;

    private S lastData;
    private boolean renderQueued;
    private boolean resizeInProgress;
    private final PauseTransition resizeSettleTransition;
    private final ChangeListener<Number> sizeListener =
            (observable, oldValue, newValue) -> handleSizeInvalidated();
    private boolean moduleAttached;
    private boolean disposed;

    // 默认高亮效果
    protected final Glow highIntensityGlow = new Glow(0.8);
    protected static final double RESIZE_SETTLE_MS = 140.0;

    public BaseVisualizer() {
        this.canvas = new Canvas();
        this.gc = canvas.getGraphicsContext2D();
        this.getChildren().add(canvas);

        this.resizeSettleTransition = new PauseTransition(Duration.millis(RESIZE_SETTLE_MS));
        this.resizeSettleTransition.setOnFinished(event -> handleResizeSettled());
        canvas.widthProperty().bind(this.widthProperty());
        canvas.heightProperty().bind(this.heightProperty());

        this.widthProperty().addListener(sizeListener);
        this.heightProperty().addListener(sizeListener);
    }

    /**
     * 渲染调度：确保 UI 更新在正确线程
     */
    public final void render(S data) {
        this.lastData = data;
        requestRender();
    }

    protected void drawCurrent() {
        if (lastData == null) {
            clear();
            return;
        }
        draw(lastData);
    }

    protected final void requestRender() {
        if (renderQueued || disposed) {
            return;
        }
        renderQueued = true;

        Runnable renderTask = () -> {
            renderQueued = false;
            if (disposed) {
                return;
            }
            drawCurrent();
        };

        Platform.runLater(renderTask);
    }

    private void handleSizeInvalidated() {
        if (disposed) {
            return;
        }
        if (!resizeInProgress) {
            resizeInProgress = true;
            onResizeStateChanged(true);
        }
        resizeSettleTransition.playFromStart();
        requestRender();
    }

    private void handleResizeSettled() {
        resizeInProgress = false;
        onResizeStateChanged(false);
        requestRender();
    }

    /**
     * 清空画布，重置为极夜黑
     */
    public void clear() {
        gc.setEffect(null);
        gc.setFill(RAN_BLACK);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    /**
     * 辅助方法：获取针对高饱和色彩的家纹/线条颜色
     * 逻辑：根据背景饱和度自动计算对比色
     */
    protected Color getContrastStrokeColor(Color background) {
        if (background.equals(RAN_WHITE))
            return RAN_BLACK;
        if (background.equals(RAN_BLUE))
            return RAN_WHITE.deriveColor(0, 0.5, 1, 0.8);
        // 对于红、蓝、黄，返回极深色以模拟“刻痕”感
        return Color.rgb(10, 0, 0, 0.85);
    }

    /**
     * 辅助方法：应用《乱》的视觉特效
     */
    protected void applyFocusEffect() {
        gc.save();
        gc.setEffect(highIntensityGlow);
    }

    protected void releaseEffect() {
        gc.restore();
    }

    protected abstract void draw(S data);

    /** Returns the last state supplied to this visualizer for animation internals. */
    protected final S currentState() {
        return lastData;
    }

    /** Updates presentation-only animation speed. Runtime execution speed remains independent. */
    public void setPlaybackSpeed(double speed) {
    }

    /** Disables presentation animation while a timeline scrub directly seeks factual state. */
    public void setScrubbing(boolean scrubbing) {
    }

    /** Presentation-only viewport obstruction contributed by shell overlays such as Current Step. */
    public void setViewportObstructionInsets(javafx.geometry.Insets insets) {
    }

    /**
     * 重置后的可视化清理钩子。
     * 默认只清空画布，子类可在此停止动画、清空缓存、重置局部状态。
     */
    public void onVisualizationReset() {
        clear();
    }

    /**
     * 模块被挂载到主界面时触发。
     * 默认留空，子类可在此恢复动画、重建监听器或刷新局部缓存。
     */
    public void onModuleAttached(String moduleId) {
        moduleAttached = true;
        requestRender();
    }

    /**
     * 模块从主界面卸载时触发。
     * 默认留空，子类可在此停止动画、释放资源并断开监听器。
     */
    public void onModuleDetached(String moduleId) {
        moduleAttached = false;
    }

    /** Definitively releases listeners, animation and canvas bindings. */
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        moduleAttached = false;
        resizeSettleTransition.stop();
        resizeSettleTransition.setOnFinished(null);
        widthProperty().removeListener(sizeListener);
        heightProperty().removeListener(sizeListener);
        canvas.widthProperty().unbind();
        canvas.heightProperty().unbind();
        renderQueued = false;
    }

    /**
     * 尺寸连续变化时的状态通知。
     * 默认留空，存在环境动画的可视化可在此临时降载。
     */
    protected void onResizeStateChanged(boolean resizing) {
    }

    protected final boolean isResizeInProgress() {
        return resizeInProgress;
    }

    protected final boolean isModuleAttached() {
        return moduleAttached;
    }

    protected final boolean isDisposed() {
        return disposed;
    }

    /**
     * 核心符号学逻辑：统一家纹绘制
     */
    protected void drawClanMon(double mx, double my, double size, Color clanColor, Color strokeColor) {
        gc.setStroke(strokeColor);
        gc.setLineWidth(Math.max(1.2, size * 0.15));

        if (clanColor.equals(RAN_RED)) {
            // 大郎：圆
            gc.strokeOval(mx - size / 2, my - size / 2, size, size);
        } else if (clanColor.equals(RAN_BLUE)) {
            // 二郎：一文字横线
            gc.strokeLine(mx - size * 0.45, my, mx + size * 0.45, my);
        } else if (clanColor.equals(RAN_YELLOW)) {
            // 三郎：三角
            double h = size * 0.866;
            gc.strokePolygon(
                    new double[] { mx, mx - size / 2, mx + size / 2 },
                    new double[] { my - h / 2, my + h / 2, my + h / 2 }, 3);
        } else {
            // 其他状态默认圆环
            gc.strokeOval(mx - size / 2, my - size / 2, size, size);
        }
    }
}
