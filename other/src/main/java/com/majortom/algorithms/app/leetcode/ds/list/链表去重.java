package com.majortom.algorithms.app.leetcode.ds.list;

import java.util.Random;

import com.majortom.algorithms.library.basic.LinkedList;
import com.majortom.algorithms.library.basic.node.ListNode;
import com.majortom.algorithms.library.utils.AlgorithmsUtils;

public class 链表去重 {
    public static void main(String[] args) {
        Integer[] randomArray = AlgorithmsUtils.randomArray(100, 10);
        ListNode<Integer> head = AlgorithmsUtils.buildLinkedList(randomArray);
        System.out.println(deleteDuplicates(head));
    }

    public static ListNode<Integer> deleteDuplicates(ListNode<Integer> head) {
        if (head == null) {
            return null;
        }
        if (head.getNext() == null) {
            return head;
        }
        return deleteDuplicates(head.getNext(), head);
    }

    private static ListNode<Integer> deleteDuplicates(ListNode<Integer> node, ListNode<Integer> previous) {
        if (node == null) {
            return node;
        }
        deleteDuplicates(node.getNext(), node);
        if (node.getValue().equals(previous.getValue())) { // 如果前一个节点数据相同于后一个节点
            previous.setNext(node.getNext()); // 则直接接上后一个节点的链表
        }
        return previous;
    }

    // 处理不了乱序时的链表重复
    private static ListNode<Integer> deleteDuplicatesFor(ListNode<Integer> head) {
        if (head == null) {
            return null;
        }
        ListNode<Integer> temp = head.getNext();
        ListNode<Integer> pre = head;
        for (; temp != null;) {
            if (pre.getValue().equals(temp.getValue())) {
                pre.setNext(temp.getNext());
                temp = temp.getNext();
            } else {
                pre = temp;
                temp = temp.getNext();
            }
        }
        return head;
    }
}
