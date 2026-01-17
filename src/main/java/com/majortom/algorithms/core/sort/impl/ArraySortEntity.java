package com.majortom.algorithms.core.sort.impl;

import com.majortom.algorithms.core.sort.BaseSort;
import com.majortom.algorithms.core.base.BaseStructure;

/**
 * 排序实体的标准实现
 */
public class ArraySortEntity<T extends Comparable<T>> extends BaseSort<T> {

    public ArraySortEntity(T[] array) {
        super(array);
    }

    @Override
    public BaseStructure<T[]> copy() {
        // 实现深拷贝：克隆数组并创建新的实体
        T[] clonedArray = this.getData().clone();
        ArraySortEntity<T> copy = new ArraySortEntity<>(clonedArray);
        copy.actionCount = this.actionCount;
        copy.compareCount = this.compareCount;
        return copy;
    }
}