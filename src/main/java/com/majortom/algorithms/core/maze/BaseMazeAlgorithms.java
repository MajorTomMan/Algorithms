package com.majortom.algorithms.core.maze;

import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.base.listener.StepListener;
import com.majortom.algorithms.core.base.listener.SyncListener;

/**
 * 迷宫算法基类
 * 
 * @param <T> 迷宫内部数据类型 (如 int[][])
 * @param <S> 迷宫实体类型
 */
public abstract class BaseMazeAlgorithms<T, S extends BaseMaze<T>> extends BaseAlgorithms<S> {

    protected S mazeEntity;

    public void setMazeEntity(S mazeEntity) {
        this.mazeEntity = mazeEntity;
        syncEnvironmentToEntity();
    }

    @Override
    public void setEnvironment(SyncListener<S> syncListener, StepListener stepListener) {
        super.setEnvironment(syncListener, stepListener);
        syncEnvironmentToEntity();
    }

    private void syncEnvironmentToEntity() {
        if (this.mazeEntity != null && this.syncListener != null) {
            this.mazeEntity.setEnvironment((SyncListener<? super BaseMaze<T>>) this.syncListener, this.stepListener);
        }
    }

    @Override
    public abstract void run(S data);
}