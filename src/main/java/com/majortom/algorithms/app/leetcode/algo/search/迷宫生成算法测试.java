
package com.majortom.algorithms.app.leetcode.algo.search;

import com.majortom.algorithms.core.maze.algorithms.generate.DFSMazeGenerator;
import com.majortom.algorithms.core.maze.algorithms.generate.UnionFindMazeGenerator;
import com.majortom.algorithms.core.maze.algorithms.pathfinding.DFSMazePathfinder;
import com.majortom.algorithms.core.maze.impl.ArrayMaze;
import com.majortom.algorithms.core.visualization.impl.frame.MazeFrame;

public class 迷宫生成算法测试 {
    public static void main(String[] args) {
        ArrayMaze container = new ArrayMaze(31, 31);

        // 生成算法：使用并查集
        DFSMazeGenerator generator = new DFSMazeGenerator();
        // 寻路算法：使用 DFS
        DFSMazePathfinder pathfinder = new DFSMazePathfinder();

        // 启动窗体，注入两个灵魂
        MazeFrame.launch(container, 20, generator, pathfinder);
    }
}
