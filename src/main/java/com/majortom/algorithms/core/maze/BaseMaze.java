package com.majortom.algorithms.core.maze;

import com.majortom.algorithms.core.base.BaseAlgorithm;

public abstract class BaseMaze<T> extends BaseAlgorithm<T> {
    protected T data;

    public abstract void initial();

    /**
     * 提供给外部算法的接口：修改状态并自动同步
     */
    public void setCellState(int r, int c, int type, boolean isAction) {
        // 1. 具体的数组修改逻辑（由子类 ArrayMaze 实现）
        updateInternalData(r, c, type);

        // 2. 统计数据管理
        if (isAction)
            actionCount++;
        else
            compareCount++;

        // 3. 呼叫 BaseFrame 进行 UI 刷新
        // 传入 r, c 作为当前操作的焦点坐标
        this.sync(data, r, c);
    }

    protected abstract void updateInternalData(int r, int c, int type);

    public T getData() {
        return data;
    }
}