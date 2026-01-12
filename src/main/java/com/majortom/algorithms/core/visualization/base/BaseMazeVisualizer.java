package com.majortom.algorithms.core.visualization.base;

import com.majortom.algorithms.core.visualization.BaseVisualizer;
import javafx.scene.paint.Color;

/**
 * 迷宫视觉呈现基类
 * 职责：定义迷宫通用的色彩体系，规范不同几何结构的绘制入口。
 */
public abstract class BaseMazeVisualizer<T> extends BaseVisualizer<T> {

    // --- 色彩重构：使用 Color.rgb 静态工厂方法 ---
    protected static final Color BG_DEEP = Color.rgb(10, 10, 14);

    // 三原色霓虹化
    protected static final Color NEON_RED = Color.rgb(255, 30, 50); // 墙体
    protected static final Color NEON_BLUE = Color.rgb(0, 160, 255); // 路径
    protected static final Color NEON_GOLD = Color.rgb(255, 200, 0); // 回溯
    protected static final Color NEON_PINK = Color.rgb(255, 0, 150); // 终点
    protected static final Color START_VIOLET = Color.rgb(160, 80, 255);

    // JavaFX 的 Opacity 范围是 0.0 到 1.0，200/255 ≈ 0.78
    protected static final Color CRYSTAL_WHITE = Color.rgb(255, 255, 255, 0.78);

    @Override
    protected void draw(T data, Object a, Object b) {
        clear();
        // 1. 填充深色背景
        gc.setFill(BG_DEEP);
        gc.fillRect(0, 0, getWidth(), getHeight());

        if (data == null)
            return;

        // 2. 调用子类实现的具体几何绘制
        drawMaze(data, a, b);
    }

    /**
     * 子类需实现此方法以处理具体的几何布局（如方形、六边形、圆形等）
     */
    protected abstract void drawMaze(T data, Object a, Object b);
}