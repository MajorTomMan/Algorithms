package com.majortom.algorithms.core.visualization;

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
 * 职责：管理 Canvas 生命周期，提供跨线程 UI 刷新保障及基础绘图工具。
 * * @param <T> 数据模型泛型，对应算法操作的实体对象
 */
public abstract class BaseVisualizer<T> extends StackPane {

    protected Canvas canvas;
    protected GraphicsContext gc;
    private T lastData;
    private Object lastA;
    private Object lastB;
    /** 默认绘图颜色配置 */
    protected Color highlightColor = Color.web("#7E57C2");
    protected Color baseColor = Color.web("#CFD8DC");
    protected Color backgroundColor = Color.web("#0A0A0E");

    public BaseVisualizer() {
        this.canvas = new Canvas();
        this.gc = canvas.getGraphicsContext2D();

        // 绑定画布到容器
        this.getChildren().add(canvas);

        // 监听容器尺寸变化
        this.widthProperty().addListener((obs, oldVal, newVal) -> resize());
        this.heightProperty().addListener((obs, oldVal, newVal) -> resize());
        this.widthProperty().addListener((obs, oldVal, newVal) -> drawCurrent());
        this.heightProperty().addListener((obs, oldVal, newVal) -> drawCurrent());
    }

    /**
     * 响应式调整画布尺寸
     */
    protected void resize() {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
    }

    /**
     * 清空画布并填充背景色
     */
    public void clear() {
        gc.setFill(backgroundColor);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    /**
     * 统一绘制入口：由算法同步钩子触发
     * 内部封装 Platform.runLater 以确保线程安全
     * * @param data 算法当前持有的数据实体
     * 
     * @param a 主操作焦点对象
     * @param b 次操作焦点对象
     */
    public final void render(T data, Object a, Object b) {
        this.lastData = data;
        this.lastA = a;
        this.lastB = b;
        if (Platform.isFxApplicationThread()) {
            draw(data, a, b);
        } else {
            Platform.runLater(() -> draw(data, a, b));
        }
    }

    /**
     * 渲染重载：仅传数据
     */
    public final void render(T data) {
        render(data, null, null);
    }

    /**
     * 渲染重载：传数据和主焦点
     */
    public final void render(T data, Object a) {
        render(data, a, null);
    }

    /**
     * 抽象绘制逻辑，由各子类根据具体数据结构实现
     * * @param data 数据实体
     * 
     * @param a 焦点 A
     * @param b 焦点 B
     */
    protected abstract void draw(T data, Object a, Object b);

    // --- 绘图辅助工具 ---

    /**
     * 绘制居中文字
     * * @param x 中心横坐标
     * 
     * @param y     中心纵坐标
     * @param text  文本内容
     * @param color 颜色
     * @param font  字体
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

    /**
     * 获取绘图上下文
     */
    public GraphicsContext getGraphicsContext() {
        return gc;
    }

    /**
     * 获取基础颜色（供子类调用）
     */
    protected Color getBaseColor() {
        return baseColor;
    }

    /**
     * 获取高亮颜色（供子类调用）
     */
    protected Color getHighlightColor() {
        return highlightColor;
    }

    /**
     * 核心：重绘当前快照
     * 当外部容器（如 MainController 里的 StackPane）尺寸变化时调用
     */
    public void drawCurrent() {
        if (lastData != null) {
            // 重绘必须在 FX 线程执行
            if (Platform.isFxApplicationThread()) {
                clear(); // 先刷背景
                draw(lastData, lastA, lastB);
            } else {
                Platform.runLater(() -> {
                    clear();
                    draw(lastData, lastA, lastB);
                });
            }
        }
    }
}