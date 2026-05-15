package com.majortom.algorithms.core.interfaces;

/**
 * 项目自定义队列接口。
 *
 * @param <T> 元素类型
 */
public interface Queue<T> extends List<T> {
    /**
     * 查看队首元素但不移除。
     *
     * @return 队首元素
     */
    public T peak();

    /**
     * 取出并移除队首元素。
     *
     * @return 队首元素
     */
    public T poll();
}
