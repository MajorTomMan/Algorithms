package com.majortom.algorithms.library.basic;

import com.majortom.algorithms.library.basic.node.ListNode;

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
            temp = temp.getNext();
            i++;
        }
        head = temp.getNext();
        pre.setNext(head);
        System.out.println("被删除的节点是:" + temp.getValue());
    }

    public void add(T data) {
        // TODO Auto-generated method stub
        if (head == null) {
            head = new ListNode<>(data);
            head.setNext(null);
            return;
        }
        ListNode<T> temp = head;
        ListNode<T> node = new ListNode<>(data);
        while (temp.getNext() != head) {
            temp = temp.getNext();
        }
        node.setNext(head);
        temp.setNext(node);
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
        if (node.getNext() == head) {
            return;
        }
        show(node.getNext());
        System.out.println(node.getValue());
    }

    public boolean isEmpty() {
        // TODO Auto-generated method stub
        return head == null;
    }
}
