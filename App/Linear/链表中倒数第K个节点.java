package linear;


import basic.structure.node.ListNode;
import utils.AlgorithmsUtils;

public class 链表中倒数第K个节点{
    private static int end=0;
    public static void main(String[] args) {
        Integer[] nums={1,2,3,4,5};
        ListNode<Integer> head=AlgorithmsUtils.buildLinkedList(nums);
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
