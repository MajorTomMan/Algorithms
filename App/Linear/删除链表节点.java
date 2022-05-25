import Basic.Structure.Node.Node;

public class 删除链表节点 extends Example{
    public static void main(String[] args){
        int[] nums={-3,5,-99};
        Node<Integer> head=buildLinkedList(nums);
        System.out.println(deleteNode(head,-99));
    }
    public static Node<Integer> deleteNode(Node<Integer> head, int val) {
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
