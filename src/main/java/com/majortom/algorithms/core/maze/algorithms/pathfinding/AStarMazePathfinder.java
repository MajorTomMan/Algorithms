package com.majortom.algorithms.core.maze.algorithms.pathfinding;

import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.BaseMazeAlgorithms;

import java.util.PriorityQueue;

import static com.majortom.algorithms.core.maze.constants.MazeConstant.*;

/**
 * A* 寻路算法
 * 职责：利用曼哈顿距离作为启发函数，在极夜背景下寻找最短路径。
 * 适配说明：单泛型重构，完全解耦具体的 ArrayMaze 实现。
 */
public class AStarMazePathfinder extends BaseMazeAlgorithms<int[][]> {

    // A* 专用的节点记录
    private record Node(int r, int c, int g, int f) {
    }

    private Node[][] parentMap;
    private boolean[][] visited;
    private final int[][] neighbors = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };

    @Override
    public void run(BaseMaze<int[][]> maze) {
        if (maze == null)
            return;

        int rows = maze.getRows();
        int cols = maze.getCols();
        int[][] data = maze.getData();

        this.visited = new boolean[rows][cols];
        this.parentMap = new Node[rows][cols];

        // 1. 定位起点和终点
        int[] start = findPoint(data, START);
        int[] end = findPoint(data, END);
        if (start == null || end == null)
            return;

        // 2. 核心：按 f 值 (g + h) 排序的优先队列
        PriorityQueue<Node> openList = new PriorityQueue<>((a, b) -> Integer.compare(a.f, b.f));

        // 3. 起点初始化
        int hStart = getManhattanDistance(start[0], start[1], end[0], end[1]);
        openList.offer(new Node(start[0], start[1], 0, hStart));
        visited[start[0]][start[1]] = true;

        while (!openList.isEmpty()) {
            // 响应线程中断，确保在 UI 切换或停止时能即时退出
            if (Thread.currentThread().isInterrupted())
                return;

            Node curr = openList.poll();

            // 逻辑判定：到达终点
            if (curr.r == end[0] && curr.c == end[1]) {
                drawBacktrackPath(maze, start, end);
                return;
            }

            // 渲染探索痕迹：将经过的 ROAD 标记为 PATH（忧郁紫）
            if (data[curr.r][curr.c] == ROAD) {
                // isAction=true 触发视觉同步和动画节流
                maze.setCellState(curr.r, curr.c, PATH, true);
            }

            for (int[] dir : neighbors) {
                int nr = curr.r + dir[0];
                int nc = curr.c + dir[1];

                // 越界、访问、墙体检查
                if (!maze.isOverBorder(nr, nc) && !visited[nr][nc] && data[nr][nc] != WALL) {
                    visited[nr][nc] = true;
                    int nextG = curr.g + 1;
                    int nextH = getManhattanDistance(nr, nc, end[0], end[1]);

                    parentMap[nr][nc] = curr;
                    openList.offer(new Node(nr, nc, nextG, nextG + nextH));
                }
            }
        }
    }

    private int getManhattanDistance(int r1, int c1, int r2, int c2) {
        return Math.abs(r1 - r2) + Math.abs(c1 - c2);
    }

    private int[] findPoint(int[][] data, int type) {
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++) {
                if (data[i][j] == type)
                    return new int[] { i, j };
            }
        }
        return null;
    }

    private void drawBacktrackPath(BaseMaze<int[][]> maze, int[] start, int[] end) {
        // 从终点的前驱开始回溯
        Node curr = parentMap[end[0]][end[1]];
        while (curr != null) {
            // 到达起点停止
            if (curr.r == start[0] && curr.c == start[1])
                break;

            // 绘制最终最短路径：设为 BACKTRACK（琥珀金）
            maze.setCellState(curr.r, curr.c, BACKTRACK, true);
            curr = parentMap[curr.r][curr.c];
        }
    }
}