package basic.structure;

import java.util.Iterator;
import java.util.function.Consumer;

import basic.structure.interfaces.Queue;
import basic.structure.interfaces.Stack;
import basic.structure.node.ListNode;

public class LinkedList<T> implements Queue<T>, Stack<T> {
    private ListNode<T> head;
    private ListNode<T> tail;
    protected int size;

    public LinkedList() {

    }

    public LinkedList(ListNode<T> head) {
        this.head = head;
    }

    @Override
    public boolean isEmpty() {
        // TODO Auto-generated method stub
        return head == null;
    }

    @Override
    public int size() {
        // TODO Auto-generated method stub
        return size;
    }

    @Override
    public void add(T t) {
        // TODO Auto-generated method stub
        if (isEmpty()) {
            head = new ListNode<T>(t);
            return;
        }
        // doAdd(t);
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
        head = node;
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

    @Override
    public T get(int index) {
        // TODO Auto-generated method stub
        if (index > size) {
            throw new ArrayIndexOutOfBoundsException("索引超出当前链表长度");
        }
        return doGet(0, index, head);
    }

    private T doGet(int deepth, int index, ListNode<T> node) {
        if (deepth == index) {
            return node.data;
        }
        return doGet(++deepth, index, node.next);
    }

    @Override
    public void remove(int index) {
        // TODO Auto-generated method stub
        if (index > size) {
            throw new ArrayIndexOutOfBoundsException("索引超出当前链表长度");
        }
        doRemove(0, index, head);
    }

    private void doRemove(int deepth, int index, ListNode<T> node) {
        if (node == null) {
            return;
        }
        if (deepth == index) {
            if (node == head) {
                head = head.next;
                if (head != null) {
                    head.pre = null;
                } else {
                    tail = null;
                }
            } else if (node == tail) {
                tail = tail.pre;
                if (tail != null) {
                    tail.next = null;
                } else {
                    head = null;
                }
            } else {
                node.pre.next = node.next;
                if (node.next != null) {
                    node.next.pre = node.pre;
                }
            }
            size--;
            return;
        }
        doRemove(++deepth, index, node.next);
    }

    @Override
    public void replace(T t, int index) {
        // TODO Auto-generated method stub
        if (index > size) {
            throw new ArrayIndexOutOfBoundsException("索引超出当前链表长度");
        }
        doReplace(0, index, t, head);
    }

    private ListNode<T> doReplace(int deepth, int index, T t, ListNode<T> node) {
        if (deepth == index) {
            node.data = t;
            return node;
        }
        return doReplace(++deepth, index, t, node.next);
    }

    @Override
    public void foreach(Consumer<T> action) {
        // TODO Auto-generated method stub
        doForeach(action, head);
    }

    public void foreach(Consumer<T> action, boolean fromLast) {
        // TODO Auto-generated method stub
        if (fromLast) {
            doForeach(action, tail);
        } else {
            doForeach(action, head);
        }
    }

    private void doForeach(Consumer<T> action, ListNode<T> node) {
        if (node == null) {
            return;
        }
        action.accept(node.data);
        doForeach(action, node.next);
    }

    @Override
    public void sort() {
        // TODO Auto-generated method stub
        doSort(head);
        handleTail(head);
    }

    private void handleTail(ListNode<T> node) {
        if (node.next == null) {
            tail = node;
            return;
        }
        handleTail(node.next);
    }

    private ListNode<T> doSort(ListNode<T> node) {
        if (node == null || node.next == null) {
            return node;
        }
        ListNode<T> middle = findMiddleNode(node);
        ListNode<T> next = middle.next;
        middle.next = null;
        next.pre = null;
        ListNode<T> left = doSort(node);
        ListNode<T> right = doSort(next);
        ListNode<T> list = doMerge(left, right);
        return list;
    }

    private ListNode<T> doMerge(ListNode<T> left, ListNode<T> right) {
        // TODO Auto-generated method stub
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        ListNode<T> list;
        if (compare(left.data, right.data) < 0) {
            list = left;
            list.next = doMerge(left.next, right);
        } else {
            list = right;
            list.next = doMerge(left, right.next);
        }
        if (list.next != null) {
            list.next.pre = list;
        }
        list.pre = null;
        return list;
    }

    private ListNode<T> findMiddleNode(ListNode<T> node) {
        ListNode<T> slow = node, fast = node.next;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }
        return slow;
    }

    @Override
    public Iterator<T> iterator() {
        // TODO Auto-generated method stub
        return new ListIterator();
    }

    private class ListIterator implements Iterator<T> {
        private ListNode<T> current = head;

        @Override
        public boolean hasNext() {
            // TODO Auto-generated method stub
            return current != null;
        }

        @Override
        public T next() {
            // TODO Auto-generated method stub
            T data = current.data;
            current = current.next;
            return data;
        }
    }

    @Override
    public T peak() {
        // TODO Auto-generated method stub
        return head != null ? head.data : null;
    }

    @Override
    public T poll() {
        return doPoll(head);
    }

    // 如果是头插法,删除尾节点,如果是正常插入,则删除头结点
    private T doPoll(ListNode<T> node) {
        if (node == null) {
            return null;
        }
        T data = node.data;
        node = node.next;
        node.pre = null;
        return data;
    }

    @Override
    public T pop() {
        // TODO Auto-generated method stub
        return doPop();
    }

    private T doPop() {
        T data = head.data;
        if (head.next != null) {
            head = head.next;
            head.pre = null;
        } else {
            head = null;
        }
        return data;
    }

    @Override
    public void push(T data) {
        // TODO Auto-generated method stub
        doAdd(data);
    }

    @Override
    public void push(T[] data) {
        // TODO Auto-generated method stub
        for (T t : data) {
            doAdd(t);
        }
    }

    @Override
    public void reverse() {
        // TODO Auto-generated method stub
        doReverse(head);
        handleTail(head);
    }

    private void doReverse(ListNode<T> node) {
        ListNode<T> current = node;
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
        // TODO Auto-generated method stub
        return doContains(t, head);
    }

    private boolean doContains(T t, ListNode<T> node) {
        // TODO Auto-generated method stub
        if (node == null) {
            return false;
        }
        if (compare(node.data, t) == 0) {
            return true;
        }
        return doContains(t, node.next);
    }
}
