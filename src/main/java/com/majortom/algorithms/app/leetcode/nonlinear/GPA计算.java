package com.majortom.algorithms.app.leetcode.nonlinear;

import edu.princeton.cs.algs4.BinarySearchST;
import edu.princeton.cs.algs4.StdOut;

public class GPA计算 {
    public static void main(String[] args) {
        String[] rank = { "A+", "A", "A-", "B+", "B", "B-", "C+", "C", "C-", "D", "F" };
        Double[] grade = { 4.33, 4.00, 3.67, 3.33, 3.00, 2.67, 2.33, 2.00, 1.67, 1.00, 0.00 };
        BinarySearchST<String, Double> bst = new BinarySearchST<>(rank.length);

        // 填充数据
        for (int i = 0; i < rank.length; i++) {
            bst.put(rank[i], grade[i]);
        }

        double totalGPA = 0.0;
        for (String r : rank) {
            if (bst.contains(r)) {
                totalGPA += bst.get(r);
            }
        }

        StdOut.printf("基于符号表的平均GPA为: %.2f\n", totalGPA / rank.length);

        // 演示符号表的有序性操作
        System.out.println("Ceiling of C: " + bst.ceiling("C"));
        System.out.println("Floor of D: " + bst.floor("D"));
    }
}