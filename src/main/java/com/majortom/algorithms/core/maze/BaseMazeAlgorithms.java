package com.majortom.algorithms.core.maze;

import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.base.listener.StepListener;
import com.majortom.algorithms.core.base.listener.SyncListener;

/**
 * 迷宫算法基类 
 * 职责：专注于对 BaseMaze<T> 的操作
 * * @param <T> 迷宫内部数据类型 (如 int[][])
 */
public abstract class BaseMazeAlgorithms<T> extends BaseAlgorithms<BaseMaze<T>> {

    protected BaseMaze<T> mazeEntity;

    public void setMazeEntity(BaseMaze<T> mazeEntity) {
        this.mazeEntity = mazeEntity;
        syncEnvironmentToEntity();
    }

    @Override
    public void setEnvironment(SyncListener<BaseMaze<T>> syncListener, StepListener stepListener) {
        super.setEnvironment(syncListener, stepListener);
        syncEnvironmentToEntity();
    }

    private void syncEnvironmentToEntity() {
        if (this.mazeEntity != null && this.syncListener != null) {
            this.mazeEntity.setEnvironment(this.syncListener, this.stepListener);
        }
    }

    @Override
    public abstract void run(BaseMaze<T> data);
}