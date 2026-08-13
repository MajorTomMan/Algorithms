package com.majortom.algorithms.core.maze.constants;

/**
 * JavaFX 迷宫渲染仍在使用的格子状态常量。
 *
 * <p>这是迁移期间保留的兼容类型。算法实现已经改用自身的 UI 中立模型；待客户端
 * 完成本地化后，应把这些常量移动到 client 并从 core 删除。</p>
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
