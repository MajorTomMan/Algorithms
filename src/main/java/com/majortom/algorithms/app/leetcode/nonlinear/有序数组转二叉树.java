package com.majortom.algorithms.app.leetcode.nonlinear;

import com.majortom.algorithms.core.basic.node.TreeNode;
import com.majortom.algorithms.utils.AlgorithmsUtils;

public class 有序数组转二叉树 {
    public static void main(String[] args) {
        Integer[] keys = AlgorithmsUtils.randomArray(20, 30);
        System.out.println(sortedArrayToBST(keys));
    }

    public static TreeNode<Integer, Integer> sortedArrayToBST(Integer[] nums) {
        TreeNode<Integer, Integer> root = null;
        for (int low = 0, high = nums.length - 1; low <= high; low++, high--) {
            int middle = low + (high - low) / 2;
            if (root == null) {
                root = new TreeNode<Integer, Integer>(nums[middle], nums[middle]);
            } else {
                rebuild(root, nums[middle]);
            }
        }
        return root;
    }

    public static TreeNode<Integer, Integer> rebuild(TreeNode<Integer, Integer> node, int data) {
        if (node == null) {
            return new TreeNode<Integer, Integer>(data, data);
        }
        if (data > node.value) {
            node.right = rebuild(node.right, data);
        } else if (data < node.value) {
            node.left = rebuild(node.left, data);
        }
        return node;
    }
}
