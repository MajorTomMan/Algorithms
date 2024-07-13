package linear;

import java.util.Random;

import basic.structure.LinkedList;
import basic.structure.node.ListNode;
import utils.AlgorithmsUtils;

public class 链表去重 {
    public static void main(String[] args) {
        Integer[] randomArray = AlgorithmsUtils.randomArray(100, 10);
        ListNode<Integer> head = AlgorithmsUtils.buildLinkedList(randomArray);
        LinkedList<Integer> list = new LinkedList<>(head);
        list.sort();
        list.foreach((i) -> {
            System.out.print(i + ",");
        });
        System.out.println();
        System.out.println(deleteDuplicates(head));
    }

    public static ListNode<Integer> deleteDuplicates(ListNode<Integer> head) {
        if (head == null) {
            return null;
        }
        if (head.next == null) {
            return head;
        }
        return deleteDuplicates(head.next, head);
    }

    private static ListNode<Integer> deleteDuplicates(ListNode<Integer> node, ListNode<Integer> previous) {
        if (node == null) {
            return node;
        }
        deleteDuplicates(node.next, node);
        if (node.data == previous.data) { // 如果前一个节点数据相同于后一个节点
            previous.next = node.next; // 则直接接上后一个节点的链表
        }
        return previous;
    }

    // 处理不了乱序时的链表重复
    private static ListNode<Integer> deleteDuplicatesFor(ListNode<Integer> head) {
        if (head == null) {
            return null;
        }
        ListNode<Integer> temp = head.next;
        ListNode<Integer> pre = head;
        for (; temp != null;) {
            if (pre.data == temp.data) {
                pre.next = temp.next;
                temp = temp.next;
            } else {
                pre = temp;
                temp = temp.next;
            }
        }
        return head;
    }
}
