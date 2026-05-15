package com.majortom.algorithms.core.maze;

import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.runtime.ExecutionContext;

/**
 * 迷宫算法基类。
 *
 * <p>迷宫结构本身会在 {@link BaseMaze#setCellState(int, int, int, boolean)}
 * 中直接触发执行上下文同步，因此本类负责把算法上下文同步给迷宫实体，
 * 让生成器和寻路器只专注于修改格子状态。</p>
 *
 * @param <T> 迷宫内部数据类型，例如 int[][] 或图结构
 */
public abstract class BaseMazeAlgorithms<T> extends BaseAlgorithms<BaseMaze<T>> {

    /**
     * 当前算法绑定的迷宫实体。
     */
    protected BaseMaze<T> mazeEntity;

    /**
     * 设置迷宫实体，并把已有执行上下文同步给它。
     *
     * @param mazeEntity 迷宫实体
     */
    public void setMazeEntity(BaseMaze<T> mazeEntity) {
        this.mazeEntity = mazeEntity;
        syncContextToEntity();
    }

    /**
     * 设置执行上下文，并同步给已绑定的迷宫实体。
     *
     * @param executionContext 执行上下文
     */
    @Override
    public void setExecutionContext(ExecutionContext<BaseMaze<T>> executionContext) {
        super.setExecutionContext(executionContext);
        syncContextToEntity();
    }

    /**
     * 将算法层上下文传递给迷宫实体。
     */
    private void syncContextToEntity() {
        if (this.mazeEntity != null && this.executionContext != null) {
            this.mazeEntity.setExecutionContext(this.executionContext);
        }
    }

    /**
     * 安全执行迷宫算法。
     *
     * <p>用户停止运行时会抛出 {@link AlgorithmInterruptedException}。
     * 这里捕获它并保留当前现场，不把迷宫标记为完整生成。</p>
     *
     * @param data 迷宫实体
     */
    public final void execute(BaseMaze<T> data) {
        try {
            run(data);
        } catch (AlgorithmInterruptedException e) {
            // 算法被中断，保留现场，不标记 isGenerated = true
            System.out.println("Maze algorithm safely interrupted.");
        }
    }


    /**
     * 执行具体迷宫算法。
     *
     * @param data 迷宫实体
     */
    @Override
    public abstract void run(BaseMaze<T> data);
}
