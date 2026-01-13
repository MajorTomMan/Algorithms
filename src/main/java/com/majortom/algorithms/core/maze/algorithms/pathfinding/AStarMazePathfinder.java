package com.majortom.algorithms.core.maze.algorithms.pathfinding;

import com.majortom.algorithms.core.maze.BaseMazeAlgorithms;
import com.majortom.algorithms.core.maze.impl.ArrayMaze;
import java.util.PriorityQueue;
import static com.majortom.algorithms.core.maze.constants.MazeConstant.*;

/**
 * A* 寻路算法
 * 适配说明：通过 ArrayMaze 实体直接获取数据并更新状态，消除强转。
 */
public class AStarMazePathfinder extends BaseMazeAlgorithms<int[][], ArrayMaze> {

    // A* 专用的记录类
    private record Node(int r, int c, int g, int f) {}

    private Node[][] parentMap;
    private boolean[][] visited;
    private final int[][] neighbors = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };

    @Override
    public void run(ArrayMaze maze) {
        // 1. 从实体获取基础数据，不再通过参数传入
        int[][] data = maze.getData();
        int rows = maze.getRows();
        int cols = maze.getCols();
        
        this.visited = new boolean[rows][cols];
        this.parentMap = new Node[rows][cols];

        // 2. 定位起点和终点
        int[] start = findPoint(data, START);
        int[] end = findPoint(data, END);
        if (start == null || end == null) return;

        // 3. 核心：优先队列
        PriorityQueue<Node> openList = new PriorityQueue<>((a, b) -> Integer.compare(a.f, b.f));

        // 4. 起点初始化
        int hStart = getManhattanDistance(start[0], start[1], end[0], end[1]);
        openList.offer(new Node(start[0], start[1], 0, hStart));
        visited[start[0]][start[1]] = true;

        while (!openList.isEmpty()) {
            if (Thread.currentThread().isInterrupted()) return;

            Node curr = openList.poll();

            // 找到终点逻辑
            if (curr.r == end[0] && curr.c == end[1]) {
                drawBacktrackPath(maze, start, end);
                return;
            }

            // 渲染探索痕迹：直接使用传入的 maze 实体
            if (data[curr.r][curr.c] == ROAD) {
                // 自动触发计数、同步和节流
                maze.setCellState(curr.r, curr.c, PATH, true);
            }

            for (int[] dir : neighbors) {
                int nr = curr.r + dir[0];
                int nc = curr.c + dir[1];

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
                if (data[i][j] == type) return new int[] { i, j };
            }
        }
        return null;
    }

    private void drawBacktrackPath(ArrayMaze maze, int[] start, int[] end) {
        Node curr = parentMap[end[0]][end[1]];
        while (curr != null) {
            if (curr.r == start[0] && curr.c == start[1]) break;
            // 绘制最终路径
            maze.setCellState(curr.r, curr.c, BACKTRACK, true);
            curr = parentMap[curr.r][curr.c];
        }
    }
}