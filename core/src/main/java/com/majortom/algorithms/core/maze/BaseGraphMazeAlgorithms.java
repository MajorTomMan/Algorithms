package com.majortom.algorithms.core.maze;

import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.maze.impl.GraphMaze;
import com.majortom.algorithms.core.runtime.ExecutionContext;

/**
 * 图结构迷宫算法基类。
 *
 * <p>它和 {@link BaseArrayMazeAlgorithms} 分开，是为了明确图迷宫不复用 {@code BaseMaze<int[][]>}
 * 的生成和寻路算法。为了让类型边界和 {@link BaseArrayMazeAlgorithms} 保持一致，
 * 具体的图迷宫实现类型由子类决定并向下传入。</p>
 *
 * @param <M> 具体图迷宫类型，默认可以是 {@link GraphMaze} 或它的子类
 */
public abstract class BaseGraphMazeAlgorithms<M extends GraphMaze> extends BaseAlgorithms<M> {

    /**
     * 当前算法绑定的图迷宫实体。
     */
    protected M mazeEntity;

    /**
     * 设置图迷宫实体。
     *
     * @param mazeEntity 图迷宫实体
     */
    public void setMazeEntity(M mazeEntity) {
        this.mazeEntity = mazeEntity;
        syncContextToEntity();
    }

    /**
     * 设置执行上下文，并同步给已绑定的图迷宫实体。
     *
     * @param executionContext 执行上下文
     */
    @Override
    public void setExecutionContext(ExecutionContext<M> executionContext) {
        super.setExecutionContext(executionContext);
        syncContextToEntity();
    }

    /**
     * 将算法层执行上下文传递给图迷宫实体。
     */
    private void syncContextToEntity() {
        if (this.mazeEntity != null && this.executionContext != null) {
            this.mazeEntity.setGraphMazeExecutionContext(this.executionContext);
        }
    }

    /**
     * 安全执行图迷宫算法。
     *
     * @param maze 图迷宫实体
     */
    public final void execute(M maze) {
        try {
            run(maze);
        } catch (AlgorithmInterruptedException e) {
            System.out.println("Graph maze algorithm safely interrupted.");
        }
    }

    /**
     * 执行具体图迷宫算法。
     *
     * @param maze 图迷宫实体
     */
    @Override
    public abstract void run(M maze);
}
