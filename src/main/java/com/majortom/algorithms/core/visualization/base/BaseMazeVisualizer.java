package com.majortom.algorithms.core.visualization.base;

import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.visualization.BaseVisualizer;
import javafx.scene.paint.Color;

/**
 * 迷宫视觉呈现基类
 * 职责：定义迷宫通用的色彩体系，规范背景渲染。
 */
public abstract class BaseMazeVisualizer<S extends BaseMaze<?>> extends BaseVisualizer<S> {

    protected static final Color BG_DEEP = Color.rgb(10, 10, 14); // 极夜黑
    protected static final Color NEON_RED = Color.rgb(255, 30, 50); // 墙体 (危险/阻隔)
    protected static final Color NEON_BLUE = Color.rgb(0, 160, 255); // 路径 (探索/忧郁紫蓝)
    protected static final Color NEON_GOLD = Color.rgb(255, 200, 0); // 回溯 (琥珀金)
    protected static final Color NEON_PINK = Color.rgb(255, 0, 150); // 终点 (欲望/目标)
    protected static final Color START_VIOLET = Color.rgb(160, 80, 255); // 起点 (源头)
    protected static final Color CRYSTAL_WHITE = Color.rgb(255, 255, 255, 0.78); // 亮芯

    @Override
    protected void draw(S data, Object a, Object b) {
        // 1. 预处理：清空并填充极夜背景
        clear();
        gc.setFill(BG_DEEP);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        if (data == null)
            return;

        // 2. 委派给子类进行几何绘制 (如方形网格)
        drawMaze(data, a, b);
    }

    protected abstract void drawMaze(S data, Object a, Object b);
}