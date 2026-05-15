package com.majortom.algorithms.core.sort;

import com.majortom.algorithms.core.base.BaseAlgorithms;

/**
 * 排序算法基类。
 *
 * <p>具体排序算法不应直接操作统计和 UI 同步，而是通过 {@link #less(BaseSort, int, int)}
 * 和 {@link #swap(BaseSort, int, int)} 访问数组。这样比较次数、动作次数、高亮索引和执行帧
 * 会保持一致。</p>
 * 
 * @param <T> 数组元素的类型（需实现 Comparable）
 */
public abstract class BaseSortAlgorithms<T extends Comparable<T>> extends BaseAlgorithms<BaseSort<T>> {

    /**
     * 排序算法统一入口。
     *
     * @param sortEntity 排序结构
     */
    @Override
    public void run(BaseSort<T> sortEntity) {
        // 执行前重置实体的统计状态
        sortEntity.resetStatistics();
        this.sort(sortEntity);
    }

    /**
     * 由具体排序算法实现实际排序逻辑。
     *
     * @param sortEntity 排序结构
     */
    public abstract void sort(BaseSort<T> sortEntity);

    /**
     * 交换两个位置并同步到可视化层。
     *
     * @param sortEntity 排序结构
     * @param i 第一个索引
     * @param j 第二个索引
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
     * 比较两个位置，并同步比较高亮。
     *
     * @param sortEntity 排序结构
     * @param i 左侧索引
     * @param j 右侧索引
     * @return 如果 i 位置元素小于 j 位置元素，返回 true
     */
    protected boolean less(BaseSort<T> sortEntity, int i, int j) {
        // 实体内部记录比较次数并更新高亮索引
        int result = sortEntity.compare(i, j);

        // 发射同步信号（渲染比较时的红蓝高亮）
        sync(sortEntity, i, j);

        return result < 0;
    }
    
}
