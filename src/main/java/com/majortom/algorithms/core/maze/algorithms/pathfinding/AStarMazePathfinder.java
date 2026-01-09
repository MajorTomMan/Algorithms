package com.majortom.algorithms.core.maze.algorithms.pathfinding;

import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.strategies.PathfindingStrategy;
import java.util.PriorityQueue;
import static com.majortom.algorithms.core.maze.constants.MazeConstant.*;

/**
 * A* 寻路算法
 * 特点：启发式搜索，利用曼哈顿距离引导，搜索范围比 BFS 小得多，效率极高。
 */
public class AStarMazePathfinder implements PathfindingStrategy<int[][]> {

    // A* 专用的节点，增加了 g 和 f 两个权重属性
    private record Node(int r, int c, int g, int f) {
    }

    private Node[][] parent;
    private boolean[][] visited;
    private final int[][] neighbors = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };

    @Override
    public void findPath(BaseMaze<int[][]> maze) {
        int[][] data = maze.getData();
        int rows = data.length;
        int cols = data[0].length;
        this.visited = new boolean[rows][cols];
        this.parent = new Node[rows][cols];

        // 1. 定位起点和终点
        int[] start = findPoint(data, START);
        int[] end = findPoint(data, END);
        if (start == null || end == null)
            return;

        // 2. 核心：优先队列（PriorityQueue）
        // 比较规则：f 值（预估总代价）最小的节点优先出队
        PriorityQueue<Node> openList = new PriorityQueue<>((a, b) -> a.f - b.f);

        // 3. 起点入队
        int hStart = getManhattanDistance(start[0], start[1], end[0], end[1]);
        openList.offer(new Node(start[0], start[1], 0, hStart));
        visited[start[0]][start[1]] = true;

        while (!openList.isEmpty()) {
            if (Thread.currentThread().isInterrupted())
                return;

            // 弹出当前“潜力最大”的节点
            Node curr = openList.poll();

            // 找到终点
            if (curr.r == end[0] && curr.c == end[1]) {
                drawBacktrackPath(maze, start, end);
                break;
            }

            // 渲染探索痕迹
            if (data[curr.r][curr.c] == ROAD) {
                maze.setCellState(curr.r, curr.c, PATH, true);
            }

            for (int[] dir : neighbors) {
                int nr = curr.r + dir[0];
                int nc = curr.c + dir[1];

                if (!maze.isOverBorder(nr, nc) && !visited[nr][nc] && data[nr][nc] != WALL) {
                    visited[nr][nc] = true;

                    // 计算权重
                    int nextG = curr.g + 1; // 已走步数 + 1
                    int nextH = getManhattanDistance(nr, nc, end[0], end[1]); // 距离终点预期

                    // 记录父节点（为了回溯）
                    parent[nr][nc] = curr;

                    // 放入优先队列
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
        Node curr = parent[end[0]][end[1]];
        while (curr != null) {
            if (curr.r == start[0] && curr.c == start[1])
                break;
            maze.setCellState(curr.r, curr.c, BACKTRACK, true);
            curr = parent[curr.r][curr.c];
        }
    }
}