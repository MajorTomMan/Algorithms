
import Basic.Structure.Node.Node;

public class 链表中倒数第K个节点 extends Example{
    private static int end=0;
    public static void main(String[] args) {
        int[] nums={1,2,3,4,5};
        Node<Integer> head=buildLinkedList(nums);
        System.out.println(getKthFromEnd(head, 2));
    }
    public static Node<Integer> getKthFromEnd(Node<Integer> head, int k) {
        if(head==null){
            return null;
        }
        Node<Integer> result=getKthFromEnd(head.next, k);
        end++;
        return end==k?head:result;
    }
}
