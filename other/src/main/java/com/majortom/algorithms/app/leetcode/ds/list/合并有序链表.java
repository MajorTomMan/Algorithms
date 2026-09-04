package com.majortom.algorithms.app.leetcode.ds.list;



import com.majortom.algorithms.library.basic.node.ListNode;
import com.majortom.algorithms.library.utils.AlgorithmsUtils;

public class 合并有序链表{
    public static void main(String[] args) {
        Integer[] nums_1={1,2,4},nums_2={1,3,4};
        ListNode<Integer> head_1=AlgorithmsUtils.buildLinkedList(nums_1),head_2=AlgorithmsUtils.buildLinkedList(nums_2);
        ListNode<Integer> result=mergeTwoLists(head_1, head_2);
        AlgorithmsUtils.printList(result);
    }
    public static ListNode<Integer> mergeTwoLists(ListNode<Integer> list1, ListNode<Integer> list2) {
        if(list1==null){
            return list2;
        }
        else if(list2==null){
            return list1;
        }
        else if (list1.getValue()<list2.getValue()) {
            list1.setNext(mergeTwoLists(list1.getNext(),list2));
            return list1;
        } else {
            list2.setNext(mergeTwoLists(list1,list2.getNext()));
            return list2;
        }
    }
}
