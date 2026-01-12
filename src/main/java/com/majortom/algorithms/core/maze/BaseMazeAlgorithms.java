package com.majortom.algorithms.core.maze;

import com.majortom.algorithms.core.base.BaseAlgorithms;

/**
 * 迷宫算法基类
 * 职责：定义迷宫算法的通用行为，并持有对 Maze 实体的引用
 */
public abstract class BaseMazeAlgorithms<T> extends BaseAlgorithms<T> {

    protected BaseMaze<T> mazeEntity;

    /**
     * 为算法注入迷宫实体，以便算法内部能通过实体触发 setCellState 和 sync
     */
    public void setMazeEntity(BaseMaze<T> mazeEntity) {
        this.mazeEntity = mazeEntity;
        // 关键点：将算法的监听器也同步给实体，实现双向绑定
        if (this.mazeEntity != null) {
            this.mazeEntity.setEnvironment(this.syncListener, this.stepListener);
        }
    }

    @Override
    public abstract void run(T data);
}