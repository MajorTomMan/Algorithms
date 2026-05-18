package com.majortom.algorithms.core.maze.constants;

/**
 * 迷宫格子状态定义。
 *
 * <p>这个类是迷宫模块的“状态语言”。结构层把每个格子当前处于什么状态记录为整数，
 * 算法层通过修改这些状态表达“打通道路”“正在探索”“找到最终路径”等过程，
 * 可视化层再根据状态选择颜色、图标和动画反馈。</p>
 *
 * <p>把状态统一放在这里后，读代码的人可以清楚区分两件事：
 * 一个类是在描述“格子是什么”，还是在描述“算法怎么移动”。前者属于本类，
 * 后者属于 {@link MazeDirections}。</p>
 */
public final class MazeCellType {

    /**
     * 可通行道路。
     */
    public static final int ROAD = 0;

    /**
     * 墙体。
     */
    public static final int WALL = 1;

    /**
     * 算法当前正在探索的路径。
     */
    public static final int PATH = 2;

    /**
     * 起点。
     */
    public static final int START = 3;

    /**
     * 探索后确认走不通的死路。
     */
    public static final int DEADEND = 4;

    /**
     * 终点。
     */
    public static final int END = 5;

    /**
     * 回溯后确认属于最终路径的格子。
     */
    public static final int BACKTRACK = 6;

    private MazeCellType() {
    }
}
