package com.majortom.algorithms.leetcode.ds.list;


import com.majortom.algorithms.structure.linked.ListNode;
import com.majortom.algorithms.leetcode.support.AlgorithmsUtils;

public class 删除链表元素 {
    public static void main(String[] args) {
        Integer[] nums={1,2,6,3,4,5,6};
        ListNode<Integer> head=AlgorithmsUtils.buildLinkedList(nums);
        System.out.println(removeElements(head,6));
    }
    public static ListNode<Integer> removeElements(ListNode<Integer> head, int val) {
        if(head==null){
            return null;
        }
        head.setNext(removeElements(head.getNext(), val));
        if(head.getValue()==val){
            head=head.getNext();
        }
        return head;
    }
}
