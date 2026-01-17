package com.majortom.algorithms.visualization;

import com.majortom.algorithms.core.base.BaseStructure;
import com.majortom.algorithms.visualization.manager.AlgorithmThreadManager;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

/**
 * 视觉呈现组件基类
 */
public abstract class BaseVisualizer<S extends BaseStructure<?>> extends StackPane {

    // --- 《乱》核心审美色彩体系 ---
    // 基础色
    public static final Color RAN_BLACK = Color.rgb(5, 5, 8); // 极夜黑/底色
    public static final Color RAN_WHITE = Color.rgb(240, 240, 230); // 骨白/文字/起点

    // 角色色（太郎、次郎、三郎）
    public static final Color RAN_RED = Color.rgb(180, 0, 0); // 太郎红：墙体/冲突
    public static final Color RAN_BLUE = Color.rgb(0, 120, 255); // 次郎蓝：路径/核心焦点
    public static final Color RAN_YELLOW = Color.rgb(240, 190, 0); // 三郎黄：邻居/已探索

    // 辅助色
    public static final Color RAN_PINK = Color.rgb(255, 50, 120); // 枫叶粉：特殊标记
    public static final Color RAN_VIOLET = Color.rgb(130, 70, 200); // 忧郁紫：回溯/深度
    public static final Color RAN_GOLD = Color.rgb(220, 180, 0); // 暮金：终点/高亮

    protected final Canvas canvas;
    protected final GraphicsContext gc;

    private S lastData;
    private Object lastA;
    private Object lastB;

    /** 兼容性配置 - 将原有变量指向新的体系 */
    protected Color highlightColor = RAN_BLUE;
    protected Color baseColor = Color.web("#CFD8DC");
    protected Color backgroundColor = RAN_BLACK;

    public BaseVisualizer() {
        this.canvas = new Canvas();
        this.gc = canvas.getGraphicsContext2D();
        this.getChildren().add(canvas);

        canvas.widthProperty().bind(this.widthProperty());
        canvas.heightProperty().bind(this.heightProperty());

        // 监听尺寸变化，确保缩放时画面不消失
        this.widthProperty().addListener((obs, oldVal, newVal) -> drawCurrent());
        this.heightProperty().addListener((obs, oldVal, newVal) -> drawCurrent());
    }

    /**
     * 外部调用核心入口：由控制器在算法同步点调用
     */
    public final void render(S data, Object a, Object b) {
        this.lastData = data;
        this.lastA = a;
        this.lastB = b;
        Platform.runLater(this::drawCurrent);
    }

    /**
     * 快捷重载：仅更新数据结构
     */
    public final void render(S data) {
        render(data, null, null);
    }

    /**
     * 调度重绘逻辑
     */
    protected void drawCurrent() {
        if (lastData == null) {
            clear();
            return;
        }
        // 执行具体的子类绘制逻辑
        draw(lastData, lastA, lastB);
    }

    /**
     * 清空画布，使用《乱》的极夜黑
     */
    public void clear() {
        gc.setFill(RAN_BLACK);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    /**
     * 子类必须实现：具体的算法渲染逻辑
     */
    protected abstract void draw(S data, Object a, Object b);

    // 获取当前缓存的数据，用于辅助逻辑
    public S getLastData() {
        return lastData;
    }
}