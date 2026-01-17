package com.majortom.algorithms.core.visualization;

import com.majortom.algorithms.core.base.BaseStructure;
import javafx.application.Platform;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * 视觉呈现组件基类
 * 职责：
 * 1. 统筹 JavaFX Canvas 生命周期与响应式布局。
 * 2. 固化黑泽明《乱》配色体系。
 * 3. 维护数据快照，支持窗口缩放时的实时重绘。
 * * @param <S> 结构类型，继承自 BaseStructure
 */
public abstract class BaseVisualizer<S extends BaseStructure<?>> extends StackPane {

    protected final Canvas canvas;
    protected final GraphicsContext gc;

    // --- 渲染快照 (用于窗口重绘) ---
    private S lastData;
    private Object lastA;
    private Object lastB;

    // --- 黑泽明《乱》配色体系 (Ran Aesthetics) ---
    protected static final Color RAN_RED = Color.rgb(180, 0, 0); // 太郎：稳固、墙体、常规节点
    protected static final Color RAN_BLUE = Color.rgb(0, 120, 255); // 次郎：行动、路径、活跃焦点
    protected static final Color RAN_GOLD = Color.rgb(220, 180, 0); // 三郎：变动、回溯、比较/关联
    protected static final Color BONE_WHITE = Color.rgb(240, 240, 230); // 骨白：核心文字、高亮轮廓
    protected static final Color ARMOR_BLACK = Color.rgb(10, 10, 12); // 铠甲黑：背景底色
    protected static final Color IRON_GRAY = Color.rgb(60, 60, 70); // 铁灰：次要装饰、连线

    public BaseVisualizer() {
        this.canvas = new Canvas();
        this.gc = canvas.getGraphicsContext2D();

        // 布局绑定：使 Canvas 随父容器 StackPane 自动伸缩
        this.getChildren().add(canvas);
        canvas.widthProperty().bind(this.widthProperty());
        canvas.heightProperty().bind(this.heightProperty());

        // 监听尺寸变化：当窗口被拉伸时，利用快照重新触发绘制
        this.widthProperty().addListener((obs, oldVal, newVal) -> drawCurrent());
        this.heightProperty().addListener((obs, oldVal, newVal) -> drawCurrent());
    }

    /**
     * 统一渲染入口
     * 职责：记录数据快照并调度到 UI 线程执行。
     * * @param data 当前数据结构状态
     * 
     * @param a 活跃焦点 (Primary Focus)
     * @param b 辅助焦点 (Secondary Focus)
     */
    public final void render(S data, Object a, Object b) {
        this.lastData = data;
        this.lastA = a;
        this.lastB = b;

        if (Platform.isFxApplicationThread()) {
            executeRedraw(data, a, b);
        } else {
            Platform.runLater(() -> executeRedraw(data, a, b));
        }
    }

    /**
     * 执行实际重绘逻辑
     */
    public void executeRedraw(S data, Object a, Object b) {
        clearCanvas();
        if (data != null) {
            draw(data, a, b);
        }
    }

    /**
     * 抽象绘制逻辑：由具体的子类实现
     */
    protected abstract void draw(S data, Object a, Object b);

    /**
     * 自动重绘当前状态
     * 用于响应 UI 线程发起的非算法性更新（如窗口缩放）。
     */
    public void drawCurrent() {
        if (lastData != null) {
            if (Platform.isFxApplicationThread()) {
                executeRedraw(lastData, lastA, lastB);
            } else {
                Platform.runLater(() -> executeRedraw(lastData, lastA, lastB));
            }
        }
    }

    /**
     * 清空画布并填充极夜背景色
     */
    public void clearCanvas() {
        gc.setFill(ARMOR_BLACK);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    // --- 绘图辅助工具 (子类通用) ---

    /**
     * 绘制具有《乱》风格的居中文字
     */
    protected void drawText(String text, double x, double y, Color color, double fontSize, boolean bold) {
        gc.save();
        gc.setFill(color);
        gc.setFont(Font.font("Consolas", bold ? FontWeight.BOLD : FontWeight.NORMAL, fontSize));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(text, x, y);
        gc.restore();
    }

    /**
     * 绘制带阴影的光晕效果 (用于增强活跃节点的视觉表现)
     */
    protected void drawGlow(double x, double y, double radius, Color color) {
        gc.save();
        gc.setFill(color.deriveColor(0, 1, 1, 0.2));
        gc.fillOval(x - radius - 5, y - radius - 5, (radius + 5) * 2, (radius + 5) * 2);
        gc.restore();
    }

    // --- Getter 接口 ---

    public S getLastData() {
        return lastData;
    }

    public GraphicsContext getGraphicsContext() {
        return gc;
    }

    public Canvas getCanvas() {
        return canvas;
    }
}