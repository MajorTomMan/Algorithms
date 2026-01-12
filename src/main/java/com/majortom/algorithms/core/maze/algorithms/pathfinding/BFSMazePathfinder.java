package com.majortom.algorithms.core.maze.algorithms.pathfinding;

import java.util.LinkedList;
import java.util.Queue;

import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.BaseMazeAlgorithms;

import static com.majortom.algorithms.core.maze.constants.MazeConstant.*;

/**
 * 广度优先搜索 (BFS) 寻路算法
 * 特点：逐层扩散，能够确保找到从起点到终点的最短路径。
 */
public class BFSMazePathfinder extends BaseMazeAlgorithms<int[][]> {

    private boolean[][] visited;

    private record Node(int r, int c) {
    }

    private Node[][] parent;

    // 扫描方向：右、左、下、上
    private final int[][] neighbors = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };

    private int rows;
    private int cols;

    // 记录起点和终点坐标
    private int startR, startC;
    private int endR, endC;

    /**
     * 辅助方法：定位起点和终点位置
     */
    private void locatePoints(int[][] data) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int type = data[i][j];
                if (type == START) {
                    startR = i;
                    startC = j;
                } else if (type == END) {
                    endR = i;
                    endC = j;
                }
            }
        }
    }

    /**
     * 辅助方法：从终点回溯到起点，绘制最短路径
     * 如果不需要展示回溯动画，可以跳过此逻辑
     */
    private void drawBacktrackPath(BaseMaze<int[][]> maze) {
        Node curr = parent[endR][endC];
        while (curr != null) {
            if (curr.r == startR && curr.c == startC)
                break;
            // 可以在这里将最短路径标记为另一种颜色，或者依然用 PATH
            maze.setCellState(curr.r, curr.c, BACKTRACK, true);
            curr = parent[curr.r][curr.c];
        }
    }

    @Override
    public void run(int[][] data) {
        // TODO Auto-generated method stub
        int[][] map = mazeEntity.getData();
        this.rows = map.length;
        this.cols = map[0].length;
        this.visited = new boolean[rows][cols];
        this.parent = new Node[rows][cols];

        // 1. 定位起点和终点
        locatePoints(data);

        // 2. 队列初始化
        Queue<Node> queue = new LinkedList<>();
        Node startNode = new Node(startR, startC);
        queue.offer(startNode);
        visited[startR][startC] = true;

        // 3. 开始迭代寻路
        while (!queue.isEmpty()) {
            // 检查线程中断，便于 UI 停止动画
            if (Thread.currentThread().isInterrupted())
                return;

            Node node = queue.poll();

            // 找到终点
            if (node.r == endR && node.c == endC) {
                drawBacktrackPath(mazeEntity); // 可选：回溯绘制最终最短路径
                break;
            }

            // 搜索邻居
            for (int[] neighbor : neighbors) {
                int nextR = node.r() + neighbor[0];
                int nextC = node.c() + neighbor[1];

                // 边界与访问检查
                if (mazeEntity.isOverBorder(nextR, nextC))
                    continue;
                if (visited[nextR][nextC])
                    continue;

                int cellType = data[nextR][nextC];

                // 遇到墙壁则跳过
                if (cellType == WALL)
                    continue;

                // 标记访问并记录父节点
                visited[nextR][nextC] = true;
                parent[nextR][nextC] = node;

                // 可视化探索过程：如果不是终点，则设为探索路径状态
                if (cellType == ROAD) {
                    mazeEntity.setCellState(nextR, nextC, PATH, true);
                }

                queue.offer(new Node(nextR, nextC));
            }
        }
    }
}