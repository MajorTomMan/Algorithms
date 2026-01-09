package com.majortom.algorithms.app.leetcode.ds.list;


import com.majortom.algorithms.core.basic.node.ListNode;
import com.majortom.algorithms.utils.AlgorithmsUtils;


public class 反转链表{
    public static void main(String[] args) {
        Integer[] nums={1,2,3,4,5};
        ListNode<Integer> head=AlgorithmsUtils.buildLinkedList(nums);
        System.out.println("反转完成后的链表:"+reverse(head));
    }
    // 递归
    public static ListNode<Integer> reverseList(ListNode<Integer> head) {
        if(head==null){
            return head; //空链表时的异常
        }
        if (head.next == null){
            return head; // 这里是重点
        }
        ListNode<Integer> last = reverseList(head.next);
        System.out.println("Last:"+last);
        System.out.println("反转前head:"+head);
        head.next.next = head;
        head.next = null;
        System.out.println("反转后head:"+head);
        return last;
    }
    // 非递归 三个指针, 当cur到达null时该链表反转已完成
    public static ListNode<Integer> reverse(ListNode<Integer> head) {
        if(head==null){
            return head;
        }
        ListNode<Integer> pre=null,cur=head,next=head.next;
        while(cur!=null){
            cur.next=pre;
            pre=cur;
            cur=next;
            if(next==null){
                continue;
            }
            else{
                next=next.next;
            }
        }
        return pre;
    }
}
