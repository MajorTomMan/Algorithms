package com.majortom.algorithms.app.leetcode.ds.tree;

import com.majortom.algorithms.core.tree.node.AVLTreeNode;
import com.majortom.algorithms.core.tree.node.BinaryTreeNode;

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
        if (node.left == null) {
            node.left = transform(node.left, data);
        } else if (node.right == null) {
            node.right = transform(node.right, data);
        } else {
            node.left = transform(node.left, data);
            node.right = transform(node.right, data);
        }
        return node;
    }
}
