package com.majortom.algorithms.app.leetcode.nonlinear;

import com.majortom.algorithms.core.tree.node.TreeNode;
import com.majortom.algorithms.utils.AlgorithmsUtils;

public class 二叉树的镜像 {
    public static void main(String[] args) {
        Integer[] a = { 4, 2, 7, 1, 3, 6, 9 };
        TreeNode<Integer> root = AlgorithmsUtils.buildBST(a);
        mirrorTree(root);
    }

    public static TreeNode<Integer> mirrorTree(TreeNode<Integer> root) {
        if (root == null) {
            return null;
        }
        TreeNode<Integer> left = mirrorTree(root.left);
        TreeNode<Integer> right = mirrorTree(root.right);
        root.left = right;
        root.right = left;
        return root;
    }
}
