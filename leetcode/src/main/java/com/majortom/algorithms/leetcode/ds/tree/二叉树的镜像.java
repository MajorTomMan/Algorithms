package com.majortom.algorithms.leetcode.ds.tree;

import com.majortom.algorithms.structure.tree.BinaryTreeNode;
import com.majortom.algorithms.leetcode.support.AlgorithmsUtils;

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
        BinaryTreeNode<Integer> left = mirrorTree(root.getLeft());
        BinaryTreeNode<Integer> right = mirrorTree(root.getRight());
        root.setLeft(right);
        root.setRight(left);
        return root;
    }
}
