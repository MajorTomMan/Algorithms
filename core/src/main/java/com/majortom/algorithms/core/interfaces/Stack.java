package com.majortom.algorithms.core.interfaces;

/**
 * 项目自定义栈接口。
 *
 * @param <T> 元素类型
 */
public interface Stack<T> extends List<T> {
    /**
     * 弹出栈顶元素。
     *
     * @return 栈顶元素
     */
    T pop();

    /**
     * 压入单个元素。
     *
     * @param data 元素
     */
    void push(T data);

    /**
     * 批量压入元素。
     *
     * @param data 元素数组
     */
    void push(T[] data);
}
