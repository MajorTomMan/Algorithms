package com.majortom.algorithms.app.leetcode.linear;



import com.majortom.algorithms.core.basic.node.ListNode;
import com.majortom.algorithms.utils.AlgorithmsUtils;


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
        if(head.data==val){
            return head.next;
        }
        else{
            head.next=deleteNode(head.next, val);
        }
        return head;
    }
}
