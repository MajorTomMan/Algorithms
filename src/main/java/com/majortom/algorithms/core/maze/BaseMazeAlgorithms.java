package com.majortom.algorithms.core.maze;

import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.base.listener.StepListener;
import com.majortom.algorithms.core.base.listener.SyncListener;

/**
 * 迷宫算法基类
 * 职责：定义迷宫算法的通用行为，并持有对 Maze 实体的引用
 */
public abstract class BaseMazeAlgorithms<T> extends BaseAlgorithms<T> {

    protected BaseMaze<T> mazeEntity;

    /**
     * 注入实体并立即同步监听器
     */
    public void setMazeEntity(BaseMaze<T> mazeEntity) {
        this.mazeEntity = mazeEntity;
        syncEnvironmentToEntity();
    }

    /**
     * 重写环境设置，确保 Controller 随时更新监听器时，实体也能同步收到
     */
    @Override
    public void setEnvironment(SyncListener<T> syncListener, StepListener stepListener) {
        super.setEnvironment(syncListener, stepListener);
        syncEnvironmentToEntity();
    }

    /**
     * 核心接力：将算法拿到的 UI 回调授权给实体
     */
    private void syncEnvironmentToEntity() {
        if (this.mazeEntity != null && this.syncListener != null) {
            // 让实体具备发信号的能力
            this.mazeEntity.setEnvironment(this.syncListener, this.stepListener);
        }
    }

    @Override
    public abstract void run(T data);
}