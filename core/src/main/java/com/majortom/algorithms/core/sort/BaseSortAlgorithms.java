package com.majortom.algorithms.core.sort;

import com.majortom.algorithms.core.base.BaseAlgorithms;

/**
 * 排序算法基类
 * 职责：定义排序算法的通用比较与交换接口，统筹 UI 同步与统计。
 * 
 * @param <T> 数组元素的类型（需实现 Comparable）
 */
public abstract class BaseSortAlgorithms<T extends Comparable<T>> extends BaseAlgorithms<BaseSort<T>> {

    @Override
    public void run(BaseSort<T> sortEntity) {
        // 执行前重置实体的统计状态
        sortEntity.resetStatistics();
        this.sort(sortEntity);
    }

    /**
     * 由具体排序算法（Bubble, Quick, Merge）实现
     */
    public abstract void sort(BaseSort<T> sortEntity);

    /**
     * 统一的交换接口
     * 
     */
    protected void swap(BaseSort<T> sortEntity, int i, int j) {
        if (i == j)
            return;

        // 实体内部完成数据交换、统计增加、状态更新
        sortEntity.swap(i, j);

        // 发射同步信号给 UI 线程
        sync(sortEntity, i, j);
    }

    /**
     * 统一的比较接口 (i 是否小于 j)
     * 
     */
    protected boolean less(BaseSort<T> sortEntity, int i, int j) {
        // 实体内部记录比较次数并更新高亮索引
        int result = sortEntity.compare(i, j);

        // 发射同步信号（渲染比较时的红蓝高亮）
        sync(sortEntity, i, j);

        return result < 0;
    }
    
}