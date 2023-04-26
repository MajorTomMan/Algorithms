/*
 * @Date: 2023-04-26 16:51:29
 * @LastEditors: hujunhao hujunhao@rtczsz.com
 * @LastEditTime: 2023-04-26 17:34:07
 * @FilePath: /alg/App/Linear/环形链表.java
 */
import basic.structure.node.ListNode;

public class 环形链表 extends Example {
    public static void main(String[] args) {
        int[] nums={3,2,0,-4};
        System.out.println(hasCycle(buildLinkedList(nums)));
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
