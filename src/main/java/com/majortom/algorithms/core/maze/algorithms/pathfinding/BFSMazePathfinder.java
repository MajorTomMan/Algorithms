package com.majortom.algorithms.core.maze.algorithms.pathfinding;

import java.util.LinkedList;
import java.util.Queue;

import com.majortom.algorithms.core.maze.BaseMazeAlgorithms;
import com.majortom.algorithms.core.maze.impl.ArrayMaze;

import static com.majortom.algorithms.core.maze.constants.MazeConstant.*;

/**
 * 广度优先搜索 (BFS) 寻路算法
 * 适配说明：完全对接双泛型架构，消除强转，利用实体自治。
 */
public class BFSMazePathfinder extends BaseMazeAlgorithms<int[][], ArrayMaze> {

    private boolean[][] visited;
    private Node[][] parent;
    private final int[][] neighbors = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };

    private record Node(int r, int c) {
    }

    // 记录起点和终点坐标
    private int startR, startC, endR, endC;

    @Override
    public void run(ArrayMaze maze) {
        // 1. 数据准备：直接从实体拿
        int[][] data = maze.getData();
        int rows = maze.getRows();
        int cols = maze.getCols();

        this.visited = new boolean[rows][cols];
        this.parent = new Node[rows][cols];

        // 2. 定位起点和终点
        if (!locatePoints(data, rows, cols))
            return;

        // 3. 队列初始化
        Queue<Node> queue = new LinkedList<>();
        queue.offer(new Node(startR, startC));
        visited[startR][startC] = true;

        // 4. 开始迭代寻路
        while (!queue.isEmpty()) {
            if (Thread.currentThread().isInterrupted())
                return;

            Node node = queue.poll();

            // 找到终点
            if (node.r == endR && node.c == endC) {
                drawBacktrackPath(maze);
                return;
            }

            for (int[] dir : neighbors) {
                int nextR = node.r + dir[0];
                int nextC = node.c + dir[1];

                // 边界与访问检查
                if (maze.isOverBorder(nextR, nextC) || visited[nextR][nextC])
                    continue;

                int cellType = data[nextR][nextC];
                if (cellType == WALL)
                    continue;

                // 标记访问并记录父节点
                visited[nextR][nextC] = true;
                parent[nextR][nextC] = node;

                // 只有原本是路的地方才渲染探索色
                if (cellType == ROAD) {
                    maze.setCellState(nextR, nextC, PATH, true);
                }
                queue.offer(new Node(nextR, nextC));
            }
        }
    }

    private boolean locatePoints(int[][] data, int rows, int cols) {
        boolean foundStart = false, foundEnd = false;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (data[i][j] == START) {
                    startR = i;
                    startC = j;
                    foundStart = true;
                } else if (data[i][j] == END) {
                    endR = i;
                    endC = j;
                    foundEnd = true;
                }
            }
        }
        return foundStart && foundEnd;
    }

    private void drawBacktrackPath(ArrayMaze maze) {
        Node curr = parent[endR][endC];
        while (curr != null) {
            if (curr.r == startR && curr.c == startC)
                break;
            // 绘制最短路径回溯
            maze.setCellState(curr.r, curr.c, BACKTRACK, true);
            curr = parent[curr.r][curr.c];
        }
    }
}