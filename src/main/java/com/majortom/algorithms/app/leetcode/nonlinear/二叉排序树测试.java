package com.majortom.algorithms.app.leetcode.nonlinear;

import com.majortom.algorithms.core.basic.BinarySearchTree;
import com.majortom.algorithms.utils.AlgorithmsUtils;

public class 二叉排序树测试 {
    public static void main(String[] args) {
        BinarySearchTree<String, Integer> tree = new BinarySearchTree<>();
        Integer[] randomArray = AlgorithmsUtils.randomArray(20, 30);
        String[] randomStringArray = AlgorithmsUtils.randomStringArray(20, 1, true);
        for (int i = 0; i < randomStringArray.length; i++) {
            tree.put(randomStringArray[i], randomArray[i]);
        }
        System.out.println(tree.size());
        tree.foreach((k, v) -> {
            System.out.println(k + ":" + v + "");
        });
        System.out.println(tree.containsKey("L"));
        if (tree.containsKey("L")) {
            tree.remove("L");
        }
        System.out.println(tree.containsKey("L"));
        System.out.println("------------------------------------------");
        tree.foreach((k, v) -> {
            System.out.println(k + ":" + v + "");
        });
        System.out.println(tree.size());
    }
}
