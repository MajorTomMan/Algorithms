package com.majortom.algorithms.leetcode.ds.list;

import com.majortom.algorithms.structure.linked.LinkedList;

public class 链表 {
    public static void main(String[] args) {
        LinkedList<Integer> list = new LinkedList<>();
        int[] values = {1, 2, 3, 6, 8, 10, 1, 2};
        for (int value : values) {
            list.enqueue(value);
        }
        print(list);

        list.remove(1);
        list.set(2, 1);
        System.out.println("--------------------------------------");
        print(list);
    }

    private static void print(LinkedList<Integer> list) {
        for (Integer value : list) {
            System.out.println(value);
        }
    }
}
