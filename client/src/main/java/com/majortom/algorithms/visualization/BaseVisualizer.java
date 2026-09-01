package com.majortom.algorithms.visualization;

import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.Glow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * 视觉呈现组件基类
 * 承载《乱》高饱和色彩体系与核心渲染调度
 */
public abstract class BaseVisualizer<S> extends StackPane {

    /* Canonical workbench palette: black, white, red, blue, yellow and gray. */
    public static final Color RAN_BLACK = Color.rgb(5, 5, 8); // 极夜黑
    public static final Color RAN_WHITE = Color.rgb(240, 240, 230); // 骨白
    public static final Color RAN_RED = Color.rgb(255, 51, 51); // 太郎红
    public static final Color RAN_BLUE = Color.rgb(0, 162, 255); // 次郎蓝
    public static final Color RAN_YELLOW = Color.rgb(255, 215, 0); // 三郎黄
    public static final Color RAN_GRAY = Color.rgb(112, 117, 122); // 工作台灰

    /* Legacy semantic names remain as palette aliases for existing visualizers. */
    public static final Color RAN_VIOLET =
            RAN_BLUE.deriveColor(0.0d, 1.0d, 0.62d, 1.0d); // 终焉紫 -> 深蓝（交换/出口）

    public static final Color RAN_STEEL = RAN_GRAY; // 钢印 -> 灰
    public static final Color RAN_IRON = Color.rgb(60, 60, 70); // 生铁 -> 深灰
    public static final Color RAN_ASH = Color.rgb(42, 42, 48); // 灰烬 -> 深灰
    public static final Color RAN_SILVER = RAN_WHITE; // 冷银 -> 白
    public static final Color RAN_BRONZE = RAN_YELLOW.deriveColor(0.0d, 0.55d, 0.82d, 1.0d); // 古铜 -> 黄
    public static final Color RAN_SLATE = RAN_GRAY; // 岩板 -> 灰

    public static final Color RAN_DARK_RED = RAN_RED.deriveColor(0.0d, 1.0d, 0.45d, 1.0d); // 枯红
    public static final Color RAN_DARK_BLUE = RAN_BLUE.deriveColor(0.0d, 1.0d, 0.45d, 1.0d); // 墨蓝
    public static final Color RAN_DARK_GOLD = RAN_YELLOW.deriveColor(0.0d, 1.0d, 0.55d, 1.0d); // 暗金
    public static final Color RAN_BURNED = RAN_BLACK; // 焦灼 -> 黑
    public static final Color RAN_DEEP_VINE = RAN_BLACK; // 暗紫 -> 黑
    public static final Color RAN_VOID = RAN_BLACK; // 虚无 (绝对禁区)

    public static final Color RAN_GOLD = RAN_YELLOW.deriveColor(0.0d, 0.42d, 1.0d, 1.0d); // 描金 -> 黄
    public static final Color RAN_CYAN = RAN_BLUE; // 荧蓝 -> 蓝
    public static final Color RAN_BLOOD_VIVID = RAN_RED; // 鲜红 -> 红
    public static final Color RAN_EMERALD = RAN_WHITE; // 翠绿 -> 白（排序完成）
    public static final Color RAN_AMBER = RAN_YELLOW; // 琥珀 -> 黄
    public static final Color RAN_GHOST_WHITE = RAN_WHITE.deriveColor(0.0d, 1.0d, 1.0d, 0.4d); // 幽灵白

    public static final Color RAN_ENEMY_GREEN = RAN_GRAY; // 诡绿 -> 灰
    public static final Color RAN_ENEMY_RUST = RAN_RED; // 铁锈红 -> 红
    public static final Color RAN_ENEMY_SHADOW = RAN_BLACK; // 极暗紫 -> 黑
    public static final Color RAN_LIME_VIVID = RAN_YELLOW; // 毒弩绿 -> 黄
    public static final Color RAN_WALL_STONE = RAN_IRON; // 坚石 -> 深灰
    public static final Color RAN_WALL_MOSS = RAN_ASH; // 苔藓 -> 深灰
    public static final Color RAN_WALL_OBSIDIAN = RAN_BLACK; // 黑曜石
    public static final Color RAN_WALL_CRACKED = RAN_GRAY; // 皲裂 -> 灰
    protected final Canvas canvas;
    protected final GraphicsContext gc;

    private S lastData;
    private String transientFeedbackLabel;
    private long transientFeedbackUntilMillis;
    private boolean renderQueued;
    private boolean resizeInProgress;
    private final PauseTransition resizeSettleTransition;
    private final ChangeListener<Number> sizeListener =
            (observable, oldValue, newValue) -> handleSizeInvalidated();
    private final AnimationTimer feedbackTimer;
    private boolean feedbackTimerRunning;
    private boolean moduleAttached;
    private boolean disposed;

    // 默认高亮效果
    protected final Glow highIntensityGlow = new Glow(0.8);
    protected static final long FEEDBACK_DURATION_MS = 1200L;
    protected static final double RESIZE_SETTLE_MS = 140.0;

    public BaseVisualizer() {
        this.canvas = new Canvas();
        this.gc = canvas.getGraphicsContext2D();
        this.getChildren().add(canvas);

        this.resizeSettleTransition = new PauseTransition(Duration.millis(RESIZE_SETTLE_MS));
        this.resizeSettleTransition.setOnFinished(event -> handleResizeSettled());
        this.feedbackTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!hasTransientFeedback() || !moduleAttached || disposed) {
                    stopFeedbackTimer();
                    requestRender();
                    return;
                }
                requestRender();
            }
        };

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
        if (background.equals(RAN_VIOLET))
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

    /**
     * 控制器按钮联动入口。
     * 默认留空，由具体可视化按需覆写。
     */
    public void onControlAction(VisualizationEvent event) {
        showTransientFeedback(event);
    }

    /**
     * 重置后的可视化清理钩子。
     * 默认只清空画布，子类可在此停止动画、清空缓存、重置局部状态。
     */
    public void onVisualizationReset() {
        clearTransientFeedback();
        clear();
    }

    /**
     * 模块被挂载到主界面时触发。
     * 默认留空，子类可在此恢复动画、重建监听器或刷新局部缓存。
     */
    public void onModuleAttached(String moduleId) {
        moduleAttached = true;
        if (hasTransientFeedback()) {
            startFeedbackTimer();
        }
        requestRender();
    }

    /**
     * 模块从主界面卸载时触发。
     * 默认留空，子类可在此停止动画、释放资源并断开监听器。
     */
    public void onModuleDetached(String moduleId) {
        moduleAttached = false;
        stopFeedbackTimer();
        clearTransientFeedback();
    }

    /** Definitively releases listeners, animation and canvas bindings. */
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        moduleAttached = false;
        stopFeedbackTimer();
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

    protected final void showTransientFeedback(VisualizationEvent event) {
        this.transientFeedbackLabel = describeEvent(event);
        this.transientFeedbackUntilMillis = System.currentTimeMillis() + FEEDBACK_DURATION_MS;
        requestRender();
        if (moduleAttached) {
            startFeedbackTimer();
        }
    }

    protected final void clearTransientFeedback() {
        this.transientFeedbackLabel = null;
        this.transientFeedbackUntilMillis = 0L;
        stopFeedbackTimer();
    }

    protected final boolean hasTransientFeedback() {
        return transientFeedbackLabel != null && System.currentTimeMillis() < transientFeedbackUntilMillis;
    }

    private void startFeedbackTimer() {
        if (feedbackTimerRunning || disposed) {
            return;
        }
        feedbackTimerRunning = true;
        feedbackTimer.start();
    }

    private void stopFeedbackTimer() {
        if (!feedbackTimerRunning) {
            return;
        }
        feedbackTimer.stop();
        feedbackTimerRunning = false;
    }

    protected final double getTransientFeedbackOpacity() {
        long remaining = transientFeedbackUntilMillis - System.currentTimeMillis();
        if (remaining <= 0) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, remaining / (double) FEEDBACK_DURATION_MS));
    }

    protected final void drawTransientFeedbackOverlay() {
        if (!hasTransientFeedback()) {
            return;
        }

        double x = 18;
        double y = 16;
        double width = 178;
        double height = 34;

        gc.save();
        gc.setGlobalAlpha(getTransientFeedbackOpacity());
        gc.setFill(RAN_BLACK.deriveColor(0, 1, 1, 0.82));
        gc.fillRoundRect(x, y, width, height, 14, 14);
        gc.setStroke(RAN_GOLD.deriveColor(0, 1, 1, 0.9));
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(x, y, width, height, 14, 14);
        gc.setFill(RAN_WHITE);
        gc.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(transientFeedbackLabel, x + 14, y + height / 2);
        gc.restore();
    }

    protected String describeEvent(VisualizationEvent event) {
        return switch (event.actionType()) {
            case EXECUTION_START -> "RUN";
            case EXECUTION_PAUSE -> "PAUSE";
            case EXECUTION_RESUME -> "RESUME";
            case EXECUTION_RESET -> "RESET";
            case EXECUTION_REPLAY -> "REPLAY";
            case EXECUTION_EXPORT -> "EXPORT";
            case EXECUTION_COMPARE -> "COMPARE";
            case MODULE_SORT -> "SORT MODE";
            case MODULE_MAZE -> "MAZE MODE";
            case MODULE_TREE -> "TREE MODE";
            case MODULE_GRAPH -> "GRAPH MODE";
            case LANGUAGE_TOGGLE -> "LANGUAGE";
            case SORT_GENERATE -> "NEW ARRAY";
            case SORT_RUN -> "SORT";
            case MAZE_BUILD -> "BUILD";
            case MAZE_SOLVE -> "SOLVE";
            case TREE_INSERT -> "INSERT";
            case TREE_DELETE -> "DELETE";
            case TREE_RANDOM -> "RANDOM";
            case GRAPH_RUN -> "TRAVERSE";
            case GRAPH_ADD_NODE -> "ADD NODE";
            case GRAPH_DELETE_NODE -> "DELETE NODE";
            case GRAPH_LINK -> "LINK";
        };
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
