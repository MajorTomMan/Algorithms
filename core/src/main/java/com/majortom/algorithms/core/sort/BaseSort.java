package com.majortom.algorithms.core.sort;

import com.majortom.algorithms.core.base.BaseStructure;

/**
 * 排序数据结构基类。
 *
 * <p>它持有待排序数组，并记录当前比较/交换涉及的索引。
 * {@link BaseSortAlgorithms} 通过 {@link #compare(int, int)} 和 {@link #swap(int, int)}
 * 操作它，执行层再把这些高亮索引交给排序可视化组件绘制。</p>
 *
 * @param <T> 可比较元素类型
 */
public abstract class BaseSort<T extends Comparable<T>> extends BaseStructure<T[]> {

    /**
     * 待排序数组。
     */
    private T[] data;

    /**
     * 当前正在操作的索引。
     */
    private int activeIndex = -1;

    /**
     * 当前正在比较的索引。
     */
    private int compareIndex = -1;

    /**
     * 创建排序结构。
     *
     * @param data 待排序数组
     */
    public BaseSort(T[] data) {
        this.data = data;
    }

    /**
     * 返回底层数组。
     *
     * @return 待排序数组
     */
    @Override
    public T[] getData() {
        return data;
    }

    /**
     * 重置统计和排序高亮索引。
     */
    @Override
    public void resetStatistics() {
        // TODO Auto-generated method stub
        super.resetStatistics();
        this.activeIndex = -1;
        this.compareIndex = -1;
    }

    /**
     * 比较两个索引位置的元素。
     *
     * @param i 第一个索引
     * @param j 第二个索引
     * @return compareTo 结果
     */
    public int compare(int i, int j) {
        incrementCompare();
        this.compareIndex = i;
        this.activeIndex = j;
        return data[i].compareTo(data[j]);
    }

    /**
     * 交换两个索引位置的元素。
     *
     * @param i 第一个索引
     * @param j 第二个索引
     */
    public void swap(int i, int j) {
        incrementAction();
        this.activeIndex = i;
        this.compareIndex = j;
        T temp = data[i];
        data[i] = data[j];
        data[j] = temp;
    }

    /**
     * 获取数组长度。
     *
     * @return 数组长度；数组为空时返回 0
     */
    public int size() {
        return data == null ? 0 : data.length;
    }

    /**
     * 获取当前操作索引。
     *
     * @return 当前操作索引，未设置时为 -1
     */
    public int getActiveIndex() {
        return activeIndex;
    }

    /**
     * 获取当前比较索引。
     *
     * @return 当前比较索引，未设置时为 -1
     */
    public int getCompareIndex() {
        return compareIndex;
    }

    /**
     * 清空排序结构。
     */
    @Override
    public void clear() {
        // TODO Auto-generated method stub
        
    }

    /**
     * 创建排序结构快照。
     *
     * @return 排序结构快照
     */
    @Override
    public BaseStructure<T[]> copy() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'copy'");
    }

}
