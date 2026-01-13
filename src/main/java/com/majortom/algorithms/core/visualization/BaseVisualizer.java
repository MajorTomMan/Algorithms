package com.majortom.algorithms.core.visualization;

import com.majortom.algorithms.core.base.BaseStructure;
import javafx.application.Platform;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * 视觉呈现组件基类
 * 职责：管理 Canvas 生命周期，提供基础绘图工具，并响应数据结构的实时变化。
 * * @param <S> 结构类型，必须继承自 BaseStructure
 */
public abstract class BaseVisualizer<S extends BaseStructure<?>> extends StackPane {

    protected final Canvas canvas;
    protected final GraphicsContext gc;

    // 缓存最后一次渲染的数据，用于响应窗口尺寸变化时的重绘
    private S lastData;
    private Object lastA;
    private Object lastB;

    /** 默认绘图颜色配置 - 保持你一贯的深色调审美 */
    protected Color highlightColor = Color.web("#7E57C2"); // 忧郁紫
    protected Color baseColor = Color.web("#CFD8DC"); // 冷灰色
    protected Color backgroundColor = Color.web("#0A0A0E"); // 极夜黑

    public BaseVisualizer() {
        this.canvas = new Canvas();
        this.gc = canvas.getGraphicsContext2D();

        // 将 Canvas 放入 StackPane 容器
        this.getChildren().add(canvas);

        // 🚩 核心逻辑：Canvas 本身不具备自增长性，必须绑定到父容器的宽高
        canvas.widthProperty().bind(this.widthProperty());
        canvas.heightProperty().bind(this.heightProperty());

        // 监听宽高变化：当窗口缩放时触发自动重绘
        this.widthProperty().addListener((obs, oldVal, newVal) -> drawCurrent());
        this.heightProperty().addListener((obs, oldVal, newVal) -> drawCurrent());
    }

    /**
     * 清空画布并填充背景色
     */
    public void clear() {
        gc.setFill(backgroundColor);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    /**
     * 统一绘制入口
     * 此方法由 BaseController 调用，运行在 JavaFX 线程。
     */
    public final void render(S data, Object a, Object b) {
        // 更新快照数据
        this.lastData = data;
        this.lastA = a;
        this.lastB = b;

        // 执行渲染逻辑
        draw(data, a, b);
    }

    /**
     * 抽象绘制逻辑，由各子类根据具体数据结构实现（如 QuickSortVisualizer）
     */
    protected abstract void draw(S data, Object a, Object b);

    /**
     * 重绘当前快照
     * 用于非算法触发的场景（如缩放窗口、页面切换）
     */
    public void drawCurrent() {
        if (lastData != null) {
            // 确保在 JavaFX UI 线程执行
            if (Platform.isFxApplicationThread()) {
                draw(lastData, lastA, lastB);
            } else {
                Platform.runLater(() -> draw(lastData, lastA, lastB));
            }
        }
    }

    // --- 绘图辅助工具 ---

    /**
     * 绘制居中文字
     */
    protected void drawCenteredText(double x, double y, String text, Color color, Font font) {
        gc.save();
        gc.setFill(color);
        gc.setFont(font);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText(text, x, y);
        gc.restore();
    }

    // --- Getter & Setter ---

    public GraphicsContext getGraphicsContext() {
        return gc;
    }

    public S getLastData() {
        return lastData;
    }

    public Canvas getCanvas() {
        return canvas;
    }
}