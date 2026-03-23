package com.majortom.algorithms.core.basic;

import com.majortom.algorithms.core.basic.node.ListNode;

public class CycleList<T> {
    private ListNode<T> head;
    private int size;

    public CycleList() {
    }

    public void remove(int index) {
        // TODO Auto-generated method stub
        int i = 0;
        if (head == null) {
            return;
        }
        ListNode<T> temp = head;
        ListNode<T> pre = new ListNode<>();
        while (i != index) {
            pre = temp;
            temp = temp.next;
            i++;
        }
        head = temp.next;
        pre.next = head;
        System.out.println("被删除的节点是:" + temp.data);
    }

    public void add(T data) {
        // TODO Auto-generated method stub
        if (head == null) {
            head = new ListNode<>(data);
            head.next = null;
            return;
        }
        ListNode<T> temp = head;
        ListNode<T> node = new ListNode<>(data);
        while (temp.next != head) {
            temp = temp.next;
        }
        node.next = head;
        temp.next = node;
    }

    public int size() {
        // TODO Auto-generated method stub
        return size;
    }

    public void show() {
        // TODO Auto-generated method stub
        show(head);
    }

    private void show(ListNode<T> node) {
        // TODO Auto-generated method stub
        if (node.next == head) {
            return;
        }
        show(node.next);
        System.out.println(node.data);
    }

    public boolean isEmpty() {
        // TODO Auto-generated method stub
        return head == null;
    }
}
