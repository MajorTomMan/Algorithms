package com.majortom.algorithms.app.leetcode.nonlinear;

import com.majortom.algorithms.core.basic.node.TreeNode;
import com.majortom.algorithms.utils.AlgorithmsUtils;

public class 二叉树的最长路径 {
    public static void main(String[] args) {
        TreeNode<Integer, Integer> root = AlgorithmsUtils.buildTree(AlgorithmsUtils.randomArray(20, 30),
                AlgorithmsUtils.randomArray(20, 30));
        maxDepth(root);

    }

    public static int maxDepth(TreeNode<Integer, Integer> root) {
        return longestPath(root);
    }

    public static int longestPath(TreeNode<Integer, Integer> node) {
        if (node == null) {
            return 0;
        } else {
            int leftHeight = longestPath(node.left);
            int rightHeight = longestPath(node.right);
            return Math.max(leftHeight, rightHeight) + 1;
        }
    }
}
