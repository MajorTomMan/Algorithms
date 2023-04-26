/*
 * @Date: 2023-04-26 16:51:29
 * @LastEditors: hujunhao hujunhao@rtczsz.com
 * @LastEditTime: 2023-04-26 17:34:30
 * @FilePath: /alg/App/Linear/链表中倒数第K个节点.java
 */

import basic.structure.node.ListNode;

public class 链表中倒数第K个节点 extends Example{
    private static int end=0;
    public static void main(String[] args) {
        int[] nums={1,2,3,4,5};
        ListNode<Integer> head=buildLinkedList(nums);
        System.out.println(getKthFromEnd(head, 2));
    }
    public static ListNode<Integer> getKthFromEnd(ListNode<Integer> head, int k) {
        if(head==null){
            return null;
        }
        ListNode<Integer> result=getKthFromEnd(head.next, k);
        end++;
        return end==k?head:result;
    }
}
