package linear;


import basic.structure.node.ListNode;
import utils.AlgorithmsUtils;

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
        head.next=removeElements(head.next, val);
        if(head.data==val){
            head=head.next;
        }
        return head;
    }
}
