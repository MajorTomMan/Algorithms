package com.majortom.algorithms.practice.leetcode.ds.tree;

import java.util.Random;

import com.majortom.algorithms.structure.tree.BinaryTreeNode;
import com.majortom.algorithms.practice.support.AlgorithmsUtils;

public class 相同的树 {
    public static void main(String[] args) {
        BinaryTreeNode<Integer> p = AlgorithmsUtils.buildBST(AlgorithmsUtils.randomArray(20, 30));
        BinaryTreeNode<Integer> q = AlgorithmsUtils.buildBST(AlgorithmsUtils.randomArray(20, 30));
    }

    public static boolean isSameTree(BinaryTreeNode<Integer> p, BinaryTreeNode<Integer> q) {
        if (p == null && q == null) {
            return true;
        } else if (p == null || q == null) {
            return false;
        } else if (p.getValue() != q.getValue()) {
            return false;
        } else {
            return isSameTree(p.getLeft(), q.getLeft()) && isSameTree(p.getRight(), q.getRight());
        }
    }
}
