import Basic.Structure.Node.Node;

public class 环形链表 extends Example {
    public static void main(String[] args) {
        int[] nums={3,2,0,-4};
        System.out.println(hasCycle(buildLinkedList(nums)));
    }
    public static boolean hasCycle(Node<Integer> head) {
        if (head == null || head.next == null) {
            return false;
        }
        Node<Integer> slow = head;
        Node<Integer> fast = head.next;
        while (slow != fast) {
            if (fast == null || fast.next == null) {
                return false;
            }
            slow = slow.next;
            fast = fast.next.next;
        }
        return true;
    }
}
