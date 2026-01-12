package com.majortom.algorithms.core.sort;

import com.majortom.algorithms.core.base.BaseAlgorithms;

/**
 * 排序数据实体类
 * 职责：持有待排序数组及当前操作的状态（如高亮索引）
 * 
 * @param <T> 必须是可比较的类型
 */
public class BaseSort<T extends Comparable<T>> extends BaseAlgorithms<BaseSort<T>> {

    private T[] data;

    // 当前正在操作（比较/交换）的索引，用于 UI 高亮
    private int activeIndex = -1;
    private int compareIndex = -1;

    public BaseSort(T[] data) {
        this.data = data;
    }

    // --- Getters & Setters ---
    public T[] getData() {
        return data;
    }

    public void setData(T[] data) {
        this.data = data;
    }

    public int getActiveIndex() {
        return activeIndex;
    }

    public void setActiveIndex(int activeIndex) {
        this.activeIndex = activeIndex;
    }

    public int getCompareIndex() {
        return compareIndex;
    }

    public void setCompareIndex(int compareIndex) {
        this.compareIndex = compareIndex;
    }

    public int size() {
        return data == null ? 0 : data.length;
    }

    public void clearStatus() {
        this.activeIndex = -1;
        this.compareIndex = -1;
    }

    @Override
    public void run(BaseSort<T> data) {
        // TODO Auto-generated method stub
    }
}