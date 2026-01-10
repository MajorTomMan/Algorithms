package com.majortom.algorithms.app.leetcode.ds.tree;

import java.util.Random;

import com.majortom.algorithms.core.tree.node.BinaryTreeNode;
import com.majortom.algorithms.utils.AlgorithmsUtils;

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
        } else if (p.data != q.data) {
            return false;
        } else {
            return isSameTree(p.left, q.left) && isSameTree(p.right, q.right);
        }
    }
}
