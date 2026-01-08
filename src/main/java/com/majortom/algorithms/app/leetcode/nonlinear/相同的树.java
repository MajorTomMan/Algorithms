package com.majortom.algorithms.app.leetcode.nonlinear;

import java.util.Random;

import com.majortom.algorithms.core.basic.node.TreeNode;
import com.majortom.algorithms.utils.AlgorithmsUtils;

public class 相同的树 {
    public static void main(String[] args) {
        TreeNode<Integer, Integer> p = AlgorithmsUtils.buildTree(AlgorithmsUtils.randomArray(20, 30),
                AlgorithmsUtils.randomArray(20, 30));
        TreeNode<Integer, Integer> q = AlgorithmsUtils.buildTree(AlgorithmsUtils.randomArray(20, 30),
                AlgorithmsUtils.randomArray(20, 30));
    }

    public static boolean isSameTree(TreeNode<Integer, Integer> p, TreeNode<Integer, Integer> q) {
        if (p == null && q == null) {
            return true;
        } else if (p == null || q == null) {
            return false;
        } else if (p.value != q.value) {
            return false;
        } else {
            return isSameTree(p.left, q.left) && isSameTree(p.right, q.right);
        }
    }
}
