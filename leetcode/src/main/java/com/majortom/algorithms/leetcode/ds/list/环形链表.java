package com.majortom.algorithms.leetcode.ds.list;



import com.majortom.algorithms.structure.linked.ListNode;
import com.majortom.algorithms.leetcode.support.AlgorithmsUtils;

public class 环形链表 {
    public static void main(String[] args) {
        Integer[] nums={3,2,0,-4};
        System.out.println(hasCycle(AlgorithmsUtils.buildLinkedList(nums)));
    }
    public static boolean hasCycle(ListNode<Integer> head) {
        if (head == null || head.getNext() == null) {
            return false;
        }
        ListNode<Integer> slow = head;
        ListNode<Integer> fast = head.getNext();
        while (slow != fast) {
            if (fast == null || fast.getNext() == null) {
                return false;
            }
            slow = slow.getNext();
            fast = fast.getNext().getNext();
        }
        return true;
    }
}
