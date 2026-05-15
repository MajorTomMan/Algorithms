
package com.majortom.algorithms.core.interfaces;

import java.util.Comparator;
import java.util.function.Consumer;

/**
 * 项目自定义线性表接口。
 *
 * <p>它用于早期基础数据结构练习，与 Java 标准库的 {@link java.util.List} 区分开。
 * 主可视化执行层不直接依赖它，但基础结构实验可以通过这个接口保持统一操作口径。</p>
 *
 * @param <T> 元素类型
 */
public interface List<T> extends Iterable<T> {

    /**
     * 判断列表是否为空。
     *
     * @return 为空时返回 true
     */
    boolean isEmpty();

    /**
     * 获取元素数量。
     *
     * @return 元素数量
     */
    int size();

    /**
     * 追加单个元素。
     *
     * @param t 元素
     */
    void add(T t);

    /**
     * 批量追加元素。
     *
     * @param t 元素数组
     */
    void add(T[] t);

    /**
     * 按索引获取元素。
     *
     * @param index 索引
     * @return 元素
     */
    T get(int index);

    /**
     * 按索引删除元素。
     *
     * @param index 索引
     */
    void remove(int index);

    /**
     * 替换指定索引元素。
     *
     * @param t 新元素
     * @param index 索引
     */
    void replace(T t, int index);

    /**
     * 遍历元素。
     *
     * @param action 元素处理函数
     */
    void foreach(Consumer<T> action);

    /**
     * 按指定比较器排序。
     *
     * @param comparator 比较器
     */
    void sort(Comparator<T> comparator);

    /**
     * 使用默认顺序排序。
     */
    void sort();

    /**
     * 反转列表。
     */
    void reverse();

    /**
     * 判断是否包含元素。
     *
     * @param t 目标元素
     * @return 包含时返回 true
     */
    boolean contains(T t);
}
