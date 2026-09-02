package com.majortom.algorithms.app.leetcode.ds.list;


import java.util.Collections;
import java.util.LinkedList;

public class 链表 {
    public static void main(String[] args) {
        LinkedList<Integer> list = new LinkedList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(6);
        list.add(8);
        list.add(10);
        list.add(1);
        list.add(2);
        list.sort(null);
        list.forEach((t) -> {
            System.out.println(t);
        });
        list.remove(1);
        list.set(1, 6);
        System.out.println("--------------------------------------");
        list.forEach((t) -> {
            System.out.println(t);
        });
        Collections.reverse(list);
        System.out.println("--------------------------------------");
        list.forEach((t) -> {
            System.out.println(t);
        });
        System.out.println(list.contains(1111));

    }
}