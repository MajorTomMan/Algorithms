package com.majortom.algorithms.core.sort;

import com.majortom.algorithms.core.base.BaseAlgorithms;

/**
 * 排序算法基类
 * 泛型 T 表示数组元素的类型，BaseSort<T> 是算法操作的实体对象
 */
public abstract class BaseSortAlgorithms<T extends Comparable<T>> extends BaseAlgorithms<BaseSort<T>> {

    @Override
    public void run(BaseSort<T> sortEntity) {
        this.sort(sortEntity);
    }

    public abstract void sort(BaseSort<T> sortEntity);

    /**
     * 泛型交换逻辑
     */
    protected void swap(BaseSort<T> sortEntity, int i, int j) {
        if (i == j)
            return;

        T[] arr = sortEntity.getData();
        T temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;

        actionCount++;
        // 设置高亮并同步
        sortEntity.setActiveIndex(i);
        sortEntity.setCompareIndex(j);
        sync(sortEntity, i, j);
    }

    /**
     * 泛型比较逻辑
     */
    protected boolean less(BaseSort<T> sortEntity, int i, int j) {
        compareCount++;
        sortEntity.setActiveIndex(i);
        sortEntity.setCompareIndex(j);
        sync(sortEntity, i, j);

        T[] arr = sortEntity.getData();
        return arr[i].compareTo(arr[j]) < 0;
    }
}