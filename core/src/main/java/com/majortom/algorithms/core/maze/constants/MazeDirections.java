package com.majortom.algorithms.core.maze.constants;

/**
 * 迷宫网格移动语义定义。
 *
 * <p>这个类是迷宫模块的“移动语言”。它不关心格子当前是墙、路还是终点，
 * 只负责说明在二维网格模型中有哪些基础移动方向，以及数组迷宫生成时为什么要
 * 跨过一格墙去探测下一个候选路点。</p>
 *
 * <p>教学上可以把它理解成一张小型坐标规则表：
 * 寻路算法通常逐格移动，所以使用单步四方向；
 * 数组迷宫生成器为了在“路-墙-路”的布局中打通通道，所以使用双步方向和挖通步长。</p>
 */
public final class MazeDirections {

    /**
     * 数组迷宫生成时的挖通步长。
     *
     * <p>在当前数组迷宫模型里，道路格与道路格之间通常隔着一堵墙。
     * 因此生成器要跨两格寻找下一个候选路点，中间那一格则是待打通的墙。</p>
     */
    public static final int CARVE_STEP = 2;

    /**
     * 单步四联通方向：右、下、左、上。
     *
     * <p>这组方向用于逐格扩展邻居的算法，例如 DFS/BFS/A* 寻路。</p>
     */
    public static final int[][] CARDINAL_DIRECTIONS = {
            { 0, 1 },
            { 1, 0 },
            { 0, -1 },
            { -1, 0 }
    };

    /**
     * 双步四联通方向：右两格、下两格、左两格、上两格。
     *
     * <p>这组方向用于数组迷宫生成时的“隔墙探测”。算法先跨到下一个候选路点，
     * 再根据方向的一半定位中间那堵墙并将其打通。</p>
     */
    public static final int[][] CARVE_DIRECTIONS = {
            { 0, CARVE_STEP },
            { CARVE_STEP, 0 },
            { 0, -CARVE_STEP },
            { -CARVE_STEP, 0 }
    };

    private MazeDirections() {
    }
}
