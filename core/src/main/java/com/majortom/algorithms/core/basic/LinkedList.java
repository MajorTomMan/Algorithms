package com.majortom.algorithms.core.basic;

import java.util.Comparator;
import java.util.Iterator;
import java.util.function.Consumer;

import com.majortom.algorithms.core.interfaces.Queue;
import com.majortom.algorithms.core.interfaces.Stack;
import com.majortom.algorithms.core.basic.node.ListNode;

public class LinkedList<T> implements Queue<T>, Stack<T> {

    private ListNode<T> head;
    private ListNode<T> tail;
    protected int size;

    public LinkedList() {
    }

    public LinkedList(T t) {
        this.head = new ListNode<>(t);
    }
    // ================= 基础 =================

    @Override
    public boolean isEmpty() {
        return head == null;
    }

    @Override
    public int size() {
        return size;
    }

    // ================= 添加 =================

    @Override
    public void add(T t) {
        if (isEmpty()) {
            head = new ListNode<>(t);
            tail = head;
            size++;
            return;
        }
        head = doAdd(t, null, head);
    }

    @Override
    public void add(T[] args) {
        for (T t : args) {
            head = doAdd(t, null, head);
        }
    }

    private void doAdd(T t) {
        ListNode<T> node = new ListNode<>(t, head, null);
        if (head != null) {
            head.pre = node;
        }
        head = node;
        if (tail == null) {
            tail = node;
        }

    }

    private ListNode<T> doAdd(T t, ListNode<T> pre, ListNode<T> current) {
        if (current == null) {
            size++;
            ListNode<T> node = new ListNode<>(t, null, pre);
            tail = node;
            return node;
        }
        current.next = doAdd(t, current, current.next);
        return current;
    }

    // ================= 查询 =================

    @Override
    public T get(int index) {
        checkIndex(index);
        return doGet(0, index, head);
    }

    private T doGet(int depth, int index, ListNode<T> node) {
        if (depth == index)
            return node.data;
        return doGet(depth + 1, index, node.next);
    }

    // ================= 删除 =================

    @Override
    public void remove(int index) {
        checkIndex(index);
        doRemove(0, index, head);
    }

    private void doRemove(int depth, int index, ListNode<T> node) {
        if (node == null)
            return;

        if (depth == index) {
            if (node == head) {
                head = head.next;
                if (head != null)
                    head.pre = null;
                else
                    tail = null;
            } else if (node == tail) {
                tail = tail.pre;
                if (tail != null)
                    tail.next = null;
                else
                    head = null;
            } else {
                node.pre.next = node.next;
                if (node.next != null)
                    node.next.pre = node.pre;
            }
            size--;
            return;
        }
        doRemove(depth + 1, index, node.next);
    }

    public void removeTail() {
        if (tail == null)
            return;

        if (tail.pre == null) {
            head = null;
            tail = null;
        } else {
            tail = tail.pre;
            tail.next = null;
        }
        size--;
    }

    // ================= 修改 =================

    @Override
    public void replace(T t, int index) {
        checkIndex(index);
        doReplace(0, index, t, head);
    }

    private void doReplace(int depth, int index, T t, ListNode<T> node) {
        if (depth == index) {
            node.data = t;
            return;
        }
        doReplace(depth + 1, index, t, node.next);
    }

    // ================= 遍历 =================

    @Override
    public void foreach(Consumer<T> action) {
        doForeach(action, head);
    }

    private void doForeach(Consumer<T> action, ListNode<T> node) {
        if (node == null)
            return;
        action.accept(node.data);
        doForeach(action, node.next);
    }

    // ================= 排序 =================

    @Override
    public void sort(Comparator<T> comparator) {
        head = doSort(head, comparator);
        handleTail(head);
    }

    @Override
    public void sort() {
        sort((a, b) -> ((Comparable<T>) a).compareTo(b));
    }

    private void handleTail(ListNode<T> node) {
        if (node == null)
            return;
        if (node.next == null) {
            tail = node;
            return;
        }
        handleTail(node.next);
    }

    private ListNode<T> doSort(ListNode<T> node, Comparator<T> comparator) {
        if (node == null || node.next == null)
            return node;

        ListNode<T> middle = findMiddleNode(node);
        ListNode<T> next = middle.next;

        middle.next = null;
        if (next != null)
            next.pre = null;

        ListNode<T> left = doSort(node, comparator);
        ListNode<T> right = doSort(next, comparator);

        return doMerge(left, right, comparator);
    }

    private ListNode<T> doMerge(ListNode<T> left, ListNode<T> right, Comparator<T> comparator) {
        if (left == null)
            return right;
        if (right == null)
            return left;

        ListNode<T> list;

        if (comparator.compare(left.data, right.data) <= 0) {
            list = left;
            list.next = doMerge(left.next, right, comparator);
        } else {
            list = right;
            list.next = doMerge(left, right.next, comparator);
        }

        if (list.next != null)
            list.next.pre = list;
        list.pre = null;

        return list;
    }

    private ListNode<T> findMiddleNode(ListNode<T> node) {
        ListNode<T> slow = node, fast = node;

        while (fast.next != null && fast.next.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }
        return slow;
    }

    // ================= Queue =================

    @Override
    public T peak() {
        return head != null ? head.data : null;
    }

    @Override
    public T poll() {
        if (head == null)
            return null;

        T data = head.data;

        if (head.next != null) {
            head = head.next;
            head.pre = null;
        } else {
            head = null;
            tail = null;
        }

        size--;
        return data;
    }

    // ================= Stack =================

    @Override
    public T pop() {
        return poll();
    }

    @Override
    public void push(T data) {
        doAdd(data);
        size++;
    }

    @Override
    public void push(T[] data) {
        for (T t : data) {
            doAdd(t);
            size++;
        }
    }

    // ================= 其他 =================

    @Override
    public void reverse() {
        ListNode<T> current = head;
        ListNode<T> prev = null;
        tail = head;

        while (current != null) {
            ListNode<T> next = current.next;
            current.next = prev;
            current.pre = next;
            prev = current;
            current = next;
        }

        head = prev;
    }

    @Override
    public boolean contains(T t) {
        return doContains(t, head);
    }

    private boolean doContains(T t, ListNode<T> node) {
        if (node == null)
            return false;

        if (t == null ? node.data == null : t.equals(node.data)) {
            return true;
        }

        return doContains(t, node.next);
    }

    // ================= Iterator =================

    @Override
    public Iterator<T> iterator() {
        return new ListIterator();
    }

    private class ListIterator implements Iterator<T> {
        private ListNode<T> current = head;

        public boolean hasNext() {
            return current != null;
        }

        public T next() {
            T data = current.data;
            current = current.next;
            return data;
        }
    }

    // ================= 工具 =================

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new ArrayIndexOutOfBoundsException("索引越界");
        }
    }

    @Override
    public String toString() {
        String result = "";
        ListNode<T> node = head;

        while (node != null) {
            result += node.data;
            if (node.next != null)
                result += " -> ";
            node = node.next;
        }

        return result;
    }

    public ListNode<T> getHead() {
        return head;
    }

    public int getSize() {
        return size;
    }

    public ListNode<T> getTail() {
        return tail;
    }
}