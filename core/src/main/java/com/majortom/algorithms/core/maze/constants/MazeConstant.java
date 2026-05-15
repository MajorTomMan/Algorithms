package com.majortom.algorithms.core.maze.constants;

/**
 * 迷宫单元格状态常量。
 *
 * <p>迷宫算法和迷宫可视化都通过这些整数值沟通状态。算法负责写入状态，
 * Visualizer 根据状态选择颜色、符号和焦点效果。</p>
 */
public class MazeConstant {
    /**
     * 可通行道路。
     */
    public static final int ROAD = 0;

    /**
     * 墙体。
     */
    public static final int WALL = 1;

    /**
     * 当前找到的路径。
     */
    public static final int PATH = 2;

    /**
     * 起点。
     */
    public static final int START = 3;

    /**
     * 死路。
     */
    public static final int DEADEND = 4;

    /**
     * 终点。
     */
    public static final int END = 5;

    /**
     * 回溯路径。
     */
    public static final int BACKTRACK = 6;
}
