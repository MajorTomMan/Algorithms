package com.majortom.algorithms.app.leetcode.ds.tree;

import com.majortom.algorithms.library.basic.tree.BinaryTreeNode;
import com.majortom.algorithms.library.basic.AlgorithmsUtils;

public class 二叉树的最长路径 {
    public static void main(String[] args) {
            BinaryTreeNode<Integer> root = AlgorithmsUtils.buildBST(AlgorithmsUtils.randomArray(20, 30));
        maxDepth(root);

    }

    public static int maxDepth(BinaryTreeNode<Integer> root) {
        return longestPath(root);
    }

    public static int longestPath(BinaryTreeNode<Integer> node) {
        if (node == null) {
            return 0;
        } else {
            int leftHeight = longestPath(node.getLeft());
            int rightHeight = longestPath(node.getRight());
            return Math.max(leftHeight, rightHeight) + 1;
        }
    }
}
