
package com.majortom.algorithms.core.basic.node;

public class ListNode<T> {
    public T data;
    public ListNode<T> next;
    public ListNode<T> pre;

    public ListNode() {

    }

    public ListNode(T data, ListNode<T> next, ListNode<T> pre) { // 构造方法，用于初始化数据区域以及指针
        this.data = data;
        this.next = next;
        this.pre = pre;
    }

    public ListNode(T data) {
        this.data = data;
        this.next = null;
        this.pre = null;
    }

    @Override
    public String toString() {
        return "data:" + data + "->" + next + " ";
    }
}
