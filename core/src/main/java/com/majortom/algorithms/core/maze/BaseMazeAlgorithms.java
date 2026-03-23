package com.majortom.algorithms.core.maze;

import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.runtime.ExecutionContext;

/**
 * 迷宫算法基类
 * 职责：专注于对 BaseMaze<T> 的操作
 * * @param <T> 迷宫内部数据类型 (如 int[][])
 */
public abstract class BaseMazeAlgorithms<T> extends BaseAlgorithms<BaseMaze<T>> {

    protected BaseMaze<T> mazeEntity;

    public void setMazeEntity(BaseMaze<T> mazeEntity) {
        this.mazeEntity = mazeEntity;
        syncContextToEntity();
    }

    @Override
    public void setExecutionContext(ExecutionContext<BaseMaze<T>> executionContext) {
        super.setExecutionContext(executionContext);
        syncContextToEntity();
    }

    private void syncContextToEntity() {
        if (this.mazeEntity != null && this.executionContext != null) {
            this.mazeEntity.setExecutionContext(this.executionContext);
        }
    }

    public final void execute(BaseMaze<T> data) {
        try {
            run(data);
        } catch (AlgorithmInterruptedException e) {
            // 算法被中断，保留现场，不标记 isGenerated = true
            System.out.println("Maze algorithm safely interrupted.");
        }
    }


    @Override
    public abstract void run(BaseMaze<T> data);
}
