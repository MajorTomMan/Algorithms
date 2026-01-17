package com.majortom.algorithms.visualization;

import com.majortom.algorithms.core.base.BaseStructure;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.Glow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

/**
 * 视觉呈现组件基类
 * 承载《乱》高饱和色彩体系与核心渲染调度
 */
public abstract class BaseVisualizer<S extends BaseStructure<?>> extends StackPane {

    // --- 《乱》极致饱和色彩体系 ---
    public static final Color RAN_BLACK = Color.rgb(5, 5, 8); // 极夜黑
    public static final Color RAN_WHITE = Color.rgb(255, 255, 245); // 骨白
    public static final Color RAN_RED = Color.rgb(220, 0, 0); // 太郎红
    public static final Color RAN_BLUE = Color.rgb(0, 100, 255); // 次郎蓝
    public static final Color RAN_YELLOW = Color.rgb(255, 215, 0); // 三郎黄
    public static final Color RAN_VIOLET = Color.rgb(150, 0, 255); // 深紫

    protected final Canvas canvas;
    protected final GraphicsContext gc;

    private S lastData;
    private Object lastA;
    private Object lastB;

    // 默认高亮效果
    protected final Glow highIntensityGlow = new Glow(0.8);

    public BaseVisualizer() {
        this.canvas = new Canvas();
        this.gc = canvas.getGraphicsContext2D();
        this.getChildren().add(canvas);

        canvas.widthProperty().bind(this.widthProperty());
        canvas.heightProperty().bind(this.heightProperty());

        this.widthProperty().addListener((obs, oldVal, newVal) -> drawCurrent());
        this.heightProperty().addListener((obs, oldVal, newVal) -> drawCurrent());
    }

    /**
     * 渲染调度：确保 UI 更新在正确线程
     */
    public final void render(S data, Object a, Object b) {
        this.lastData = data;
        this.lastA = a;
        this.lastB = b;
        Platform.runLater(this::drawCurrent);
    }

    public final void render(S data) {
        render(data, null, null);
    }

    protected void drawCurrent() {
        if (lastData == null) {
            clear();
            return;
        }
        draw(lastData, lastA, lastB);
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

    protected abstract void draw(S data, Object a, Object b);

    public S getLastData() {
        return lastData;
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