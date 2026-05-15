package com.majortom.algorithms.visualization.base;

import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.visualization.BaseVisualizer;
import com.majortom.algorithms.visualization.VisualizationActionType;
import com.majortom.algorithms.visualization.VisualizationEvent;


/**
 * 迷宫算法可视化基类。
 *
 * <p>它把迷宫行列数转换成屏幕单元格尺寸，并把具体绘制留给子类。
 * 方形迷宫、图迷宫或未来六边形迷宫都可以共用这个计算流程。</p>
 *
 * @param <S> 迷宫结构类型
 */
public abstract class BaseMazeVisualizer<S extends BaseMaze<?>> extends BaseVisualizer<S> {

    /**
     * 绘制迷宫快照。
     *
     * @param data 迷宫快照
     * @param a 第一个焦点，通常是行坐标
     * @param b 第二个焦点，通常是列坐标
     */
    @Override
    protected void draw(S data, Object a, Object b) {
        clear(); // 使用 BaseVisualizer 的极夜黑清空

        if (data == null)
            return;

        // 计算基础单元格尺寸
        double cellW = canvas.getWidth() / data.getCols();
        double cellH = canvas.getHeight() / data.getRows();

        drawMaze(data, a, b, cellW, cellH);
        drawFocus(a, b, cellW, cellH);
        drawTransientFeedbackOverlay();
    }

    /**
     * 绘制迷宫主体。
     *
     * @param mazeEntity 迷宫快照
     * @param a 第一个焦点
     * @param b 第二个焦点
     * @param cellW 单元格宽度
     * @param cellH 单元格高度
     */
    protected abstract void drawMaze(S mazeEntity, Object a, Object b, double cellW, double cellH);

    @Override
    public void onControlAction(VisualizationEvent event) {
        super.onControlAction(event);
        if (event.actionType() == VisualizationActionType.EXECUTION_RESET) {
            clear();
        }
    }

    @Override
    public void onVisualizationReset() {
        clear();
    }

    @Override
    public void onModuleDetached(String moduleId) {
        clear();
    }

    /**
     * 获取单元格左上角屏幕 X 坐标。
     *
     * @param col 列坐标
     * @param cellW 单元格宽度
     * @return 屏幕 X 坐标
     */
    protected double getX(int col, double cellW) {
        return col * cellW;
    }

    /**
     * 获取单元格左上角屏幕 Y 坐标。
     *
     * @param row 行坐标
     * @param cellH 单元格高度
     * @return 屏幕 Y 坐标
     */
    protected double getY(int row, double cellH) {
        return row * cellH;
    }

    /**
     * 绘制焦点高亮。
     *
     * @param a 第一个焦点
     * @param b 第二个焦点
     * @param w 单元格宽度
     * @param h 单元格高度
     */
    protected abstract void drawFocus(Object a, Object b, double w, double h);
}
