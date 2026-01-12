package com.majortom.algorithms.core.maze;

import com.majortom.algorithms.core.base.BaseAlgorithms;

public abstract class BaseMaze<T> extends BaseAlgorithms<T> {
    protected T data;

    public abstract void initial();

    /**
     * 核心改进：Maze 实体作为数据的持有者，所有的修改都通过这里
     * type: 0-路, 1-墙, 3-起点, 5-终点, 4-路径, 2-探索中
     */
    public void setCellState(int r, int c, int type, boolean isAction) {
        updateInternalData(r, c, type);

        if (isAction)
            actionCount++;
        else
            compareCount++;

        // 调用基类的 sync，这会触发 Controller 里的渲染和 Semaphore 阻塞
        this.sync(data, r, c);
    }

    protected abstract void updateInternalData(int r, int c, int type);

    public abstract void pickRandomPoints();

    public abstract boolean isOverBorder(int r, int c);

    public T getData() {
        return data;
    }
    
    // 关键：Maze 作为一个容器，其 run 通常不直接使用，除非是自运行
    @Override
    public void run(T data) {
        // 默认空实现
    }
}