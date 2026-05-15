package com.majortom.algorithms.core.maze;

import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.maze.impl.GraphMaze;
import com.majortom.algorithms.core.runtime.ExecutionContext;

/**
 * 图结构迷宫算法基类。
 *
 * <p>它和 {@link BaseMazeAlgorithms} 分开，是为了明确图迷宫不复用 {@code BaseMaze<int[][]>}
 * 的生成和寻路算法。后续你实现图迷宫生成器或图迷宫寻路器时，统一继承这个类即可。</p>
 */
public abstract class BaseGraphMazeAlgorithms extends BaseAlgorithms<GraphMaze> {

    /**
     * 当前算法绑定的图迷宫实体。
     */
    protected GraphMaze mazeEntity;

    /**
     * 设置图迷宫实体。
     *
     * @param mazeEntity 图迷宫实体
     */
    public void setMazeEntity(GraphMaze mazeEntity) {
        this.mazeEntity = mazeEntity;
        syncContextToEntity();
    }

    /**
     * 设置执行上下文，并同步给已绑定的图迷宫实体。
     *
     * @param executionContext 执行上下文
     */
    @Override
    public void setExecutionContext(ExecutionContext<GraphMaze> executionContext) {
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
    public final void execute(GraphMaze maze) {
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
    public abstract void run(GraphMaze maze);
}
