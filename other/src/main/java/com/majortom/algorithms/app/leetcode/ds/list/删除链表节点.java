package com.majortom.algorithms.app.leetcode.ds.list;



import com.majortom.algorithms.library.basic.node.ListNode;
import com.majortom.algorithms.library.basic.utils.AlgorithmsUtils;


public class 删除链表节点{
    public static void main(String[] args){
        Integer[] nums={-3,5,-99};
        ListNode<Integer> head=AlgorithmsUtils.buildLinkedList(nums);
        System.out.println(deleteNode(head,-99));
    }
    public static ListNode<Integer> deleteNode(ListNode<Integer> head, int val) {
        if(head==null){
            return null;
        }
        if(head.getValue()==val){
            return head.getNext();
        }
        else{
            head.setNext(deleteNode(head.getNext(), val));
        }
        return head;
    }
}
