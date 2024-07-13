package linear;

import basic.structure.LinkedList;
import basic.structure.node.ListNode;

public class 双向链表 {
    public static void main(String[] args) {
        LinkedList<Integer> list = new LinkedList<>();
        Integer[] nums = { 1, 2, 3, 24, 1, 24, 12, 412, 4, 124, 12, 4 };
        System.out.println("原始链表：");
        showLink(list);
        for (int i = 0; i < 10; i++) {
            list.add(i);
        }
        System.out.println("向链表添加元素后：");
        showLink(list);
        System.out.println();
        System.out.println("向指定索引位置插入元素");
        list.add(nums);
        showLink(list);
        System.out.println();
        System.out.println("删除头元素和尾元素");
        list.remove(0);
        list.remove(list.size());
        showLink(list);
        System.out.println();
        System.out.println("替换指定索引位置的元素");
        list.replace(2, 5);
        showLink(list);
        System.out.println();
        System.out.println("查询指定元素");
        System.out.println("查询元素6:" + list.get(6));
        System.out.println("返回指定索引位置的元素");
        System.out.println("查询索引位置2的元素:" + list.get(2));
        System.out.println();
        System.out.println("Pres List:");
        list.foreach((v) -> {
            System.out.println(v + ",");
        }, true);
        System.out.println();
        System.out.println("for each:");
        for (Integer data : list) {
            System.out.print(" " + data);
        }
        System.out.println(list);
    }

    public static void showLink(LinkedList<?> list) {
        list.foreach((t) -> {
            System.out.print(t + ",");
        });
    }

}
