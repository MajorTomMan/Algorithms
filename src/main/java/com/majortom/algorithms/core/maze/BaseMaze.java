package com.majortom.algorithms.core.maze;

import com.majortom.algorithms.core.base.BaseAlgorithms;

/**
 * 迷宫实体抽象基类
 * 职责：
 * 1. 持有并维护迷宫的原始数据模型 T。
 * 2. 提供统一的原子化操作接口 (setCellState)，并自动触发 UI 同步。
 * 3. 定义迷宫通用的生命周期方法 (初始化、边界检查、随机点位)。
 * * @param <T> 数据模型类型 (如 int[][])
 */
public abstract class BaseMaze<T> extends BaseAlgorithms<T> {

    protected T data; // 核心数据模型
    protected int rows; // 迷宫行数
    protected int cols; // 迷宫列数

    public BaseMaze(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
    }

    /**
     * 全局初始化：将迷宫重置为“满墙”状态
     * 场景：模块首次加载、用户点击 RESET 按钮。
     */
    public abstract void initial();

    /**
     * 静默初始化：仅重置内存数据，不触发 UI 同步信号
     * 场景：用于后台预加载或不需要动画的瞬间重置。
     */
    public abstract void initialSilent();

    /**
     * 核心修改接口：修改单个单元格状态并触发同步
     * * @param r 行索引
     * 
     * @param c        列索引
     * @param type     状态常量 (见 MazeConstant)
     * @param isAction 是否计入算法步数 (决定是否触发节流阻塞)
     */
    public void setCellState(int r, int c, int type, boolean isAction) {
        // 1. 更新内部数据矩阵
        updateInternalData(r, c, type);

        // 2. 统计量计数
        if (isAction) {
            actionCount++;
        } else {
            compareCount++;
        }

        // 3. 触发同步：这会调用从 Algorithm 传过来的 syncListener
        // 将当前最新的 data 发送到 UI 线程渲染，并根据 stepListener 进行节流
        this.sync(data, r, c);
    }

    /**
     * 子类需实现：具体的内存数据修改逻辑
     */
    protected abstract void updateInternalData(int r, int c, int type);

    /**
     * 获取指定坐标的状态值
     */
    public abstract int getCell(int r, int c);

    /**
     * 边界检查：防止算法运行过程中数组越界
     */
    public boolean isOverBorder(int r, int c) {
        return r < 0 || r >= rows || c < 0 || c >= cols;
    }

    /**
     * 随机选取起点和终点
     * 场景：迷宫生成完毕后，为寻路算法做准备。
     */
    public abstract void pickRandomPoints();

    /**
     * 获取底层数据对象
     */
    public T getData() {
        return data;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    /**
     * 实体作为数据容器，不直接实现算法逻辑，run 留空。
     */
    @Override
    public void run(T data) {
    }
}