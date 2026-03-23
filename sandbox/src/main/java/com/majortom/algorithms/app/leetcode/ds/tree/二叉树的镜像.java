package com.majortom.algorithms.app.leetcode.ds.tree;

import com.majortom.algorithms.core.tree.node.BinaryTreeNode;
import com.majortom.algorithms.utils.AlgorithmsUtils;

public class 二叉树的镜像 {
    public static void main(String[] args) {
        Integer[] a = { 4, 2, 7, 1, 3, 6, 9 };
        BinaryTreeNode<Integer> root = AlgorithmsUtils.buildBST(a);
        mirrorTree(root);
    }

    public static BinaryTreeNode<Integer> mirrorTree(BinaryTreeNode<Integer> root) {
        if (root == null) {
            return null;
        }
        BinaryTreeNode<Integer> left = mirrorTree(root.left);
        BinaryTreeNode<Integer> right = mirrorTree(root.right);
        root.left = right;
        root.right = left;
        return root;
    }
}
