
package com.majortom.algorithms.app.leetcode.nonlinear;

import com.majortom.algorithms.app.visualization.MazePanel;
import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.maze.impl.BFSMaze;
import com.majortom.algorithms.core.maze.impl.DFSMaze;
import com.majortom.algorithms.core.maze.impl.UnionFindMaze;

public class 迷宫生成算法测试 {
    public static void main(String[] args) {
        BaseMaze maze = new UnionFindMaze(31, 31);
        MazePanel.launch(maze, 20, 50);
    }
}
