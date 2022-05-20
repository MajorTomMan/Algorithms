import Basic.Structure.Node.Node;
import NonLinear.Example;

public class 相交链表 extends Example{
    public static void main(String[] args) {
        int[] nums_a={0},nums_b={5,6,1,8,4,5};
        Node<Integer> head_A=buildLinkedList(nums_a);
        Node<Integer> head_B=buildLinkedList(nums_b);
        System.out.println(getIntersectionNode(head_A, head_B,head_A,head_B));
    }
    public Node<Integer> getIntersectionNode(Node<Integer> headA, Node<Integer> headB) {
        if(headA==null||headB==null){
            return null;
        }
        return getIntersectionNode(headA,headB,headA,headB);
    }
    //双指针
    public static Node<Integer> getIntersectionNode(Node<Integer> headA,Node<Integer> headB,
                  Node<Integer> origin_A,Node<Integer> origin_B) {
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
        Node<Integer> result=getIntersectionNode(headA.next, headB.next,origin_A,origin_B); //同步往下找
        return result;
    }
}
