
import basic.structure.node.ListNode;

public class 相交链表 extends Common{
    public static void main(String[] args) {
        int[] nums_a={0},nums_b={5,6,1,8,4,5};
        ListNode<Integer> head_A=buildLinkedList(nums_a);
        ListNode<Integer> head_B=buildLinkedList(nums_b);
        System.out.println(getIntersectionNode(head_A, head_B,head_A,head_B));
    }
    public ListNode<Integer> getIntersectionNode(ListNode<Integer> headA, ListNode<Integer> headB) {
        if(headA==null||headB==null){
            return null;
        }
        return getIntersectionNode(headA,headB,headA,headB);
    }
    //双指针
    public static ListNode<Integer> getIntersectionNode(ListNode<Integer> headA,ListNode<Integer> headB,
                  ListNode<Integer> origin_A,ListNode<Integer> origin_B) {
        if(headA==null&&headB==null){
            return null;
        }
        if(headA==null){ //如果A到头了,那么将其指向B链表的头部
            return getIntersectionNode(origin_B,headB.next,origin_A,origin_B);
        }
        if(headB==null){ //如果B到头了,那么将其指向A链表的头部
            return getIntersectionNode(headA.next,origin_A,origin_A,origin_B);
        }
        if(headA.equals(headB)){
            return headA;
        }
        ListNode<Integer> result=getIntersectionNode(headA.next, headB.next,origin_A,origin_B); //同步往下找
        return result;
    }
}
