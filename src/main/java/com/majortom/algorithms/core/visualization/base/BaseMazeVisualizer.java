package com.majortom.algorithms.core.visualization.base;

import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.visualization.BaseVisualizer;
import javafx.scene.paint.Color;

public abstract class BaseMazeVisualizer<S extends BaseMaze<?>> extends BaseVisualizer<S> {

    // --- 《乱》核心三原色体系 ---
    protected static final Color BG_DEEP = Color.rgb(5, 5, 8);
    protected static final Color RAN_YELLOW = Color.rgb(240, 190, 0);
    // 核心色值定义
    protected final Color RAN_RED = Color.rgb(180, 0, 0); // 红色：墙体
    protected final Color RAN_BLUE = Color.rgb(0, 120, 255); // 蓝色：路径
    protected final Color RAN_GOLD = Color.rgb(220, 180, 0); // 黄色：回溯
    protected final Color BONE_WHITE = Color.rgb(240, 240, 230); // 骨白：焦点/起点
    protected static final Color RAN_PINK = Color.rgb(255, 50, 120);
    protected static final Color RAN_VIOLET = Color.rgb(130, 70, 200);

    @Override
    protected void draw(S data, Object a, Object b) {
        clear();
        gc.setFill(BG_DEEP);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        if (data == null)
            return;
        drawMaze(data, a, b);
    }

    protected abstract void drawMaze(S data, Object a, Object b);

    protected abstract void drawFocus(Object a, Object b, double w, double h);
}