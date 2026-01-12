package com.majortom.algorithms.core.maze.algorithms.pathfinding;

import com.majortom.algorithms.core.maze.BaseMazeAlgorithms;
import java.util.PriorityQueue;
import static com.majortom.algorithms.core.maze.constants.MazeConstant.*;

/**
 * A* 寻路算法
 * 特点：利用 F = G + H 引导，g 为起点代价，h 为曼哈顿距离。
 * 相比 BFS，它具有极强的方向感，能显著减少搜索节点的数量。
 */
public class AStarMazePathfinder extends BaseMazeAlgorithms<int[][]> {

    // A* 专用的记录类
    private record Node(int r, int c, int g, int f) {
    }

    private Node[][] parentMap;
    private boolean[][] visited;
    private final int[][] neighbors = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };

    @Override
    public void run(int[][] data) {
        if (mazeEntity == null)
            return;

        int rows = data.length;
        int cols = data[0].length;
        this.visited = new boolean[rows][cols];
        this.parentMap = new Node[rows][cols];

        // 1. 定位起点和终点
        int[] start = findPoint(data, START);
        int[] end = findPoint(data, END);
        if (start == null || end == null)
            return;

        // 2. 核心：优先队列，f 值越小优先级越高
        PriorityQueue<Node> openList = new PriorityQueue<>((a, b) -> a.f - b.f);

        // 3. 起点初始化
        int hStart = getManhattanDistance(start[0], start[1], end[0], end[1]);
        openList.offer(new Node(start[0], start[1], 0, hStart));
        visited[start[0]][start[1]] = true;

        while (!openList.isEmpty()) {
            // 检查线程状态，支持 UI 随时中断算法
            if (Thread.currentThread().isInterrupted())
                return;

            Node curr = openList.poll();

            // 找到终点逻辑
            if (curr.r == end[0] && curr.c == end[1]) {
                drawBacktrackPath(start, end);
                return;
            }

            // 渲染探索痕迹：只有原本是 ROAD 的地方才变色，保护 START 和 END 不被覆盖
            if (data[curr.r][curr.c] == ROAD) {
                // 🚩 使用 mazeEntity 触发同步和节流
                mazeEntity.setCellState(curr.r, curr.c, PATH, true);
            }

            for (int[] dir : neighbors) {
                int nr = curr.r + dir[0];
                int nc = curr.c + dir[1];

                if (!mazeEntity.isOverBorder(nr, nc) && !visited[nr][nc] && data[nr][nc] != WALL) {
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

    /**
     * 回溯并绘制最终路径
     */
    private void drawBacktrackPath(int[] start, int[] end) {
        Node curr = parentMap[end[0]][end[1]];
        while (curr != null) {
            // 到达起点停止
            if (curr.r == start[0] && curr.c == start[1])
                break;

            mazeEntity.setCellState(curr.r, curr.c, BACKTRACK, true);
            curr = parentMap[curr.r][curr.c];
        }
    }
}