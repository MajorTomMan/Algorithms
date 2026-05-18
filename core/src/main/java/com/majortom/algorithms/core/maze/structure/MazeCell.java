package com.majortom.algorithms.core.maze.structure;

import com.majortom.algorithms.core.maze.constants.MazeCellType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * 图迷宫中的单元格节点数据。
 *
 * <p>{@code MazeCell} 是图节点里的业务数据：GraphStream 节点负责“图结构关系”，
 * 本类负责“迷宫语义”。也就是说，节点 ID 表示它在图里的名字，{@code row/col} 表示它在网格里的坐标，
 * {@code type} 决定可视化状态，{@code cost} 则给带权寻路算法提供扩展空间。</p>
 *
 * <p>这层拆分能让算法教学更清楚：图算法遍历的是节点和边，但可视化展示的是格子状态。
 * {@link com.majortom.algorithms.core.maze.impl.GraphMaze} 会在两者之间完成转换。</p>
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
     *
     * <p>类型值来自 {@link MazeCellType}。算法修改这个字段后，可视化器会根据类型绘制墙、道路、路径、起点或终点。</p>
     */
    private int type = MazeCellType.WALL; // 默认是墙

    /**
     * 通行代价，供 A*、Dijkstra 等带权寻路算法使用。
     *
     * <p>默认值为 1，表示普通道路。未来如果加入地形系统，可以用更大的 cost 表示更难通过的格子。</p>
     */
    private int cost = 1;

    /**
     * 创建带指定类型的迷宫单元格。
     *
     * <p>这个构造器适合快照复制或测试场景：调用方可以一次性确定坐标和初始状态，
     * cost 仍保持普通道路默认值。</p>
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
