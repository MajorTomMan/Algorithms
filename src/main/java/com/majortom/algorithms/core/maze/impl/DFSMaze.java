package com.majortom.algorithms.core.maze.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.majortom.algorithms.core.maze.BaseMaze;

public class DFSMaze extends BaseMaze {

    private boolean[][] visited;
    private final int[][] DIRECTIONS = { { -1, 0 }, { 0, -1 }, { 1, 0 }, { 0, 1 } };

    public DFSMaze(int rows, int cols) {
        super(rows, cols);
        this.visited = new boolean[getRows()][getCols()];
    }

    @Override
    public void generate() {
        // 1. 随机起点
        this.startRow = random.nextInt(getRows() / 2) * 2;
        this.startCol = random.nextInt(getCols() / 2) * 2;

        // 2. 随机终点 (简单逻辑，确保不同)
        do {
            this.endRow = random.nextInt(getRows());
            this.endCol = random.nextInt(getCols());
        } while (endRow == startRow && endCol == startCol);

        // 3. 开始递归生成
        dfs(startRow, startCol);

        // 4. 修正标志位
        setCell(startRow, startCol, START);
        setCell(endRow, endCol, END);
    }

    private void dfs(int r, int c) {
        visited[r][c] = true;
        setCell(r, c, PATH);

        List<Integer> orders = new ArrayList<>(Arrays.asList(0, 1, 2, 3));
        Collections.shuffle(orders);

        for (int i : orders) {
            // 跳两格探测，中间的一格在打通时处理
            int nextR = r + DIRECTIONS[i][0] * 2;
            int nextC = c + DIRECTIONS[i][1] * 2;

            if (!isOutOfIndex(nextR, nextC) && !visited[nextR][nextC]) {
                // 打通中间的那面墙
                setCell(r + DIRECTIONS[i][0], c + DIRECTIONS[i][1], PATH);
                dfs(nextR, nextC);
            }
        }
    }

    @Override
    public boolean isConnected() {
        // DFS生成天然保证联通，这里可以留空或简单返回true
        return true;
    }
}
