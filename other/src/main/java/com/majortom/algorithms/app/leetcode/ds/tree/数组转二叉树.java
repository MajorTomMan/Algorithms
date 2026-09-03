package com.majortom.algorithms.app.leetcode.ds.tree;

import com.majortom.algorithms.library.basic.tree.AVLTreeNode;
import com.majortom.algorithms.library.basic.tree.BinaryTreeNode;

public class 数组转二叉树 {
    public static void main(String[] args) {
        Integer[] nums = { 5, 4, 8, 11, null, 13, 4, 7, 2, null, null, null, 1 };
        for (Integer integer : nums) {
            transform(null, integer);
        }

    }

    public static BinaryTreeNode<Integer> transform(BinaryTreeNode<Integer> node, int data) {
        if (node == null) {
            return new AVLTreeNode<Integer>(data);
        }
        if (node.getLeft() == null) {
            node.setLeft(transform(node.getLeft(), data));
        } else if (node.getRight() == null) {
            node.setRight(transform(node.getRight(), data));
        } else {
            node.setLeft(transform(node.getLeft(), data));
            node.setRight(transform(node.getRight(), data));
        }
        return node;
    }
}
