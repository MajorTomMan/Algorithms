/*
 * @Date: 2023-04-26 16:51:29
 * @LastEditors: hujunhao hujunhao@rtczsz.com
 * @LastEditTime: 2023-04-26 17:32:58
 * @FilePath: /alg/App/Linear/删除链表元素.java
 */
import basic.structure.node.ListNode;

public class 删除链表元素 extends Example {
    public static void main(String[] args) {
        int[] nums={1,2,6,3,4,5,6};
        ListNode<Integer> head=buildLinkedList(nums);
        System.out.println(removeElements(head,6));
    }
    public static ListNode<Integer> removeElements(ListNode<Integer> head, int val) {
        if(head==null){
            return null;
        }
        head.next=removeElements(head.next, val);
        if(head.data==val){
            head=head.next;
        }
        return head;
    }
}
