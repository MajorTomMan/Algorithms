package linear;



import basic.structure.node.ListNode;
import utils.AlgorithmsUtils;

public class 环形链表 {
    public static void main(String[] args) {
        Integer[] nums={3,2,0,-4};
        System.out.println(hasCycle(AlgorithmsUtils.buildLinkedList(nums)));
    }
    public static boolean hasCycle(ListNode<Integer> head) {
        if (head == null || head.next == null) {
            return false;
        }
        ListNode<Integer> slow = head;
        ListNode<Integer> fast = head.next;
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
