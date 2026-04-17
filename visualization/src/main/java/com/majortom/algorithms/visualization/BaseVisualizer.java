package com.majortom.algorithms.visualization;

import com.majortom.algorithms.core.base.BaseStructure;
import com.majortom.algorithms.core.maze.constants.MazeConstant;

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

    public static final Color RAN_BLACK = Color.rgb(5, 5, 8); // 极夜黑
    public static final Color RAN_WHITE = Color.rgb(255, 255, 245); // 骨白
    public static final Color RAN_RED = Color.rgb(220, 0, 0); // 太郎红
    public static final Color RAN_BLUE = Color.rgb(0, 100, 255); // 次郎蓝
    public static final Color RAN_YELLOW = Color.rgb(255, 215, 0); // 三郎黄
    public static final Color RAN_VIOLET = Color.rgb(150, 0, 255); // 终焉紫

    public static final Color RAN_STEEL = Color.rgb(160, 165, 170); // 钢印
    public static final Color RAN_IRON = Color.rgb(70, 72, 75); // 生铁
    public static final Color RAN_ASH = Color.rgb(40, 40, 45); // 灰烬
    public static final Color RAN_SILVER = Color.rgb(200, 205, 210); // 冷银
    public static final Color RAN_BRONZE = Color.rgb(150, 110, 70); // 古铜
    public static final Color RAN_SLATE = Color.rgb(100, 110, 120); // 岩板

    public static final Color RAN_DARK_RED = Color.rgb(80, 0, 0); // 枯红
    public static final Color RAN_DARK_BLUE = Color.rgb(0, 30, 80); // 墨蓝
    public static final Color RAN_DARK_GOLD = Color.rgb(120, 100, 0); // 暗金
    public static final Color RAN_BURNED = Color.rgb(20, 15, 10); // 焦灼
    public static final Color RAN_DEEP_VINE = Color.rgb(40, 0, 60); // 暗紫
    public static final Color RAN_VOID = Color.rgb(2, 2, 5); // 虚无 (绝对禁区)

    public static final Color RAN_GOLD = Color.rgb(255, 240, 150); // 描金
    public static final Color RAN_CYAN = Color.rgb(100, 220, 255); // 荧蓝
    public static final Color RAN_BLOOD_VIVID = Color.rgb(255, 40, 40); // 鲜红
    public static final Color RAN_EMERALD = Color.rgb(0, 200, 100); // 翠绿
    public static final Color RAN_AMBER = Color.rgb(255, 160, 0); // 琥珀 
    public static final Color RAN_GHOST_WHITE = Color.rgb(200, 220, 255, 0.4); // 幽灵白

    public static final Color RAN_ENEMY_GREEN = Color.rgb(0, 70, 40); // 诡绿
    public static final Color RAN_ENEMY_RUST = Color.rgb(130, 40, 20); // 铁锈红
    public static final Color RAN_ENEMY_SHADOW = Color.rgb(30, 0, 50); // 极暗紫
    public static final Color RAN_LIME_VIVID = Color.rgb(180, 255, 0); // 毒弩绿
    public static final Color RAN_WALL_STONE = Color.rgb(45, 50, 55); // 坚石
    public static final Color RAN_WALL_MOSS = Color.rgb(30, 40, 30); // 苔藓
    public static final Color RAN_WALL_OBSIDIAN = Color.rgb(15, 15, 20); // 黑曜石
    public static final Color RAN_WALL_CRACKED = Color.rgb(60, 55, 50); // 皲裂
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
        if (Platform.isFxApplicationThread()) {
            drawCurrent();
        } else {
            Platform.runLater(this::drawCurrent);
        }
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
     * * @param mx 中心点X
     * 
     * @param my          中心点Y
     * @param size        家纹大小
     * @param type        MazeConstant 中定义的单元格类型
     * @param strokeColor 线条颜色 (刻痕色)
     */
    protected void drawClanMon(double mx, double my, double size, int type, Color strokeColor) {
        gc.setStroke(strokeColor);
        gc.setLineWidth(Math.max(1.2, size * 0.15));

        switch (type) {
            case MazeConstant.PATH -> {
                // 大郎红：正统圆
                gc.strokeOval(mx - size / 2, my - size / 2, size, size);
            }
            case MazeConstant.ROAD -> {
                // 二郎蓝：一文字横线
                gc.strokeLine(mx - size * 0.45, my, mx + size * 0.45, my);
            }
            case MazeConstant.BACKTRACK, MazeConstant.DEADEND -> {
                // 三郎黄：三角
                double h = size * 0.866;
                gc.strokePolygon(
                        new double[] { mx, mx - size / 2, mx + size / 2 },
                        new double[] { my - h / 2, my + h / 2, my + h / 2 }, 3);
            }
            case MazeConstant.WALL -> {
                // 敌方墙壁：十字纹
                double offset = size * 0.35;
                gc.strokeLine(mx - offset, my - offset, mx + offset, my + offset);
                gc.strokeLine(mx + offset, my - offset, mx - offset, my + offset);
            }
            case MazeConstant.START, MazeConstant.END -> {
                // 起止点：同心圆
                gc.strokeOval(mx - size / 2, my - size / 2, size, size);
                gc.strokeOval(mx - size / 4, my - size / 4, size / 2, size / 2);
            }
            default -> {
                // 默认圆环
                gc.strokeOval(mx - size / 2, my - size / 2, size, size);
            }
        }
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
