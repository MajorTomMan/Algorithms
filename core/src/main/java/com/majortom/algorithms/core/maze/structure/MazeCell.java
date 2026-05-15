package com.majortom.algorithms.core.maze.structure;

import com.majortom.algorithms.core.maze.constants.MazeConstant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * 图迷宫中的单元格节点数据。
 *
 * <p>{@code GraphMaze} 会把每个格子建成 GraphStream 节点，并把 MazeCell 存入节点 data 属性。
 * 算法读取 row/col/type/cost 来判断位置、状态和路径代价。</p>
 */
@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class MazeCell {

    /**
     * 行坐标。
     */
    private final int row;

    /**
     * 列坐标。
     */
    private final int col;

    /**
     * 单元格类型，默认是墙。
     */
    private int type = MazeConstant.WALL; // 默认是墙

    /**
     * 通行代价，供 A* 等带权寻路算法使用。
     */
    private int cost = 1;

    /**
     * 创建带指定类型的迷宫单元格。
     *
     * @param row 行坐标
     * @param col 列坐标
     * @param type 单元格类型
     */
    public MazeCell(int row, int col, int type) {
        this.row = row;
        this.col = col;
        this.type = type;
        this.cost = 1;
    }
}
