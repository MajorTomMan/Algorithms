package com.majortom.algorithms.core.maze.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import java.util.Queue;

import com.majortom.algorithms.core.maze.BaseMaze;

public class BFSMaze extends BaseMaze {

    private boolean[][] visited;
    private final int[][] DIRECTIONS = { { -1, 0 }, { 0, -1 }, { 1, 0 }, { 0, 1 } };

    public BFSMaze(int rows, int cols) {
        super(rows, cols);
        this.visited = new boolean[getRows()][getCols()];
    }

    @Override
    public void generate() {
        // 1. 确定起点与终点
        this.startRow = random.nextInt(getRows() / 2) * 2;
        this.startCol = random.nextInt(getCols() / 2) * 2;

        do {
            this.endRow = random.nextInt(getRows());
            this.endCol = random.nextInt(getCols());
        } while (endRow == startRow && endCol == startCol);

        // 2. BFS核心逻辑
        bfs(startRow, startCol);

        // 3. 标记身份
        setCell(startRow, startCol, START);
        setCell(endRow, endCol, END);
    }

    private void bfs(int r, int c) {
        Queue<int[]> queue = new LinkedList<>();
        queue.offer(new int[] { r, c });
        visited[r][c] = true;
        setCell(r, c, PATH);

        while (!queue.isEmpty()) {
            int[] current = queue.poll();

            List<Integer> orders = new ArrayList<>(List.of(0, 1, 2, 3));
            Collections.shuffle(orders);

            for (int i : orders) {
                // 1. 尝试探测四个方向
                incrementCheck();

                int nextR = current[0] + DIRECTIONS[i][0] * 2;
                int nextC = current[1] + DIRECTIONS[i][1] * 2;

                if (!isOutOfIndex(nextR, nextC) && !visited[nextR][nextC]) {
                    visited[nextR][nextC] = true;

                    // 2. 确定可以打通，计入“打通”次数
                    incrementBreak();

                    // 打通连接处
                    setCell(current[0] + DIRECTIONS[i][0], current[1] + DIRECTIONS[i][1], PATH);
                    // 设置新路径点
                    setCell(nextR, nextC, PATH);

                    queue.offer(new int[] { nextR, nextC });
                }
            }
        }
    }

    @Override
    public boolean isConnected() {
        // BFS生成的迷宫保证所有可达点都是联通的
        return true;
    }
}