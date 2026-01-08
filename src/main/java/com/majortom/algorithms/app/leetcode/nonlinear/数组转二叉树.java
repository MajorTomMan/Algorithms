package com.majortom.algorithms.app.leetcode.nonlinear;

import com.majortom.algorithms.core.basic.node.TreeNode;
import com.majortom.algorithms.utils.AlgorithmsUtils;

public class 数组转二叉树 {
    public static void main(String[] args) {
        Integer[] nums = { 5, 4, 8, 11, null, 13, 4, 7, 2, null, null, null, 1 };
        for (Integer integer : nums) {
            transform(null, integer);
        }

    }

    public static TreeNode<Integer, Integer> transform(TreeNode<Integer, Integer> node, int data) {
        if (node == null) {
            return new TreeNode<Integer, Integer>(data, null, null);
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
