package com.majortom.algorithms.core.sort;

import com.majortom.algorithms.core.base.BaseStructure;

/**
 * 排序数据实体类
 * 职责：持有待排序数组，并记录当前 UI 高亮状态。
 * * @param <T> 必须是可比较的类型（如 Integer, Double）
 */
public abstract class BaseSort<T extends Comparable<T>> extends BaseStructure<T[]> {

    private T[] data;

    // 当前正在操作的索引（例如：绿色高亮表示交换，红色高亮表示比较）
    private int activeIndex = -1;
    private int compareIndex = -1;

    public BaseSort(T[] data) {
        this.data = data;
    }

    /**
     * 实现 BaseStructure 方法：返回底层原始数组
     */
    @Override
    public T[] getData() {
        return data;
    }

    @Override
    public void resetStatistics() {
        // TODO Auto-generated method stub
        super.resetStatistics();
        this.activeIndex = -1;
        this.compareIndex = -1;
    }

    // --- 排序专用原子操作（带同步暗示） ---

    public int compare(int i, int j) {
        incrementCompare();
        this.compareIndex = i;
        this.activeIndex = j;
        return data[i].compareTo(data[j]);
    }

    public void swap(int i, int j) {
        incrementAction();
        this.activeIndex = i;
        this.compareIndex = j;
        T temp = data[i];
        data[i] = data[j];
        data[j] = temp;
    }

    // --- 基础属性 ---

    public int size() {
        return data == null ? 0 : data.length;
    }

    public int getActiveIndex() {
        return activeIndex;
    }

    public int getCompareIndex() {
        return compareIndex;
    }

    @Override
    public void clear() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public BaseStructure<T[]> copy() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'copy'");
    }

}