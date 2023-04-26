import basic.structure.node.ListNode;

/*
 * @Date: 2023-04-26 16:51:29
 * @LastEditors: hujunhao hujunhao@rtczsz.com
 * @LastEditTime: 2023-04-26 17:33:10
 * @FilePath: /alg/App/Linear/删除链表节点.java
 */


public class 删除链表节点 extends Example{
    public static void main(String[] args){
        int[] nums={-3,5,-99};
        ListNode<Integer> head=buildLinkedList(nums);
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
