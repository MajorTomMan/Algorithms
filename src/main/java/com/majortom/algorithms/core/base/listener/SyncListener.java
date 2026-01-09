package com.majortom.algorithms.core.base.listener;

/**
 * 核心同步监听器：负责算法状态到 UI 的桥梁
 * 
 * @param <T> 算法操作的数据结构（如 int[], BaseGraph, BaseTree）
 */
@FunctionalInterface
public interface SyncListener<T> {
    /**
     * @param data         当前数据全景
     * @param a            焦点A
     * @param b            焦点B
     * @param compareCount 统一比较计数
     * @param actionCount  统一操作计数
     */
    void onSync(T data, Object a, Object b, int compareCount, int actionCount);
}