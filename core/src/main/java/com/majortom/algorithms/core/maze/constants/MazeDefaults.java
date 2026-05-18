package com.majortom.algorithms.core.maze.constants;

/**
 * 迷宫模块的默认约定值。
 *
 * <p>这个类用于收拢那些不属于“格子状态”也不属于“移动方向”，
 * 但在多个迷宫类中稳定重复出现的默认约定。当前先只保留已经明确复用的少量默认值，
 * 给后续扩展预留清晰的位置，避免再把这些值塞回某个大而杂的常量总类。</p>
 */
public final class MazeDefaults {

    /**
     * 数组迷宫生成器默认起始行坐标。
     */
    public static final int START_ROW = 1;

    /**
     * 数组迷宫生成器默认起始列坐标。
     */
    public static final int START_COL = 1;

    /**
     * 可视化和局部状态恢复时默认采用的普通地形。
     */
    public static final int DEFAULT_TERRAIN = MazeCellType.ROAD;

    private MazeDefaults() {
    }
}
