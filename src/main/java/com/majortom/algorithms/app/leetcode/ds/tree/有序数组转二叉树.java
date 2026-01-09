package com.majortom.algorithms.app.leetcode.ds.tree;

import com.majortom.algorithms.core.tree.node.TreeNode;
import com.majortom.algorithms.utils.AlgorithmsUtils;

public class 有序数组转二叉树 {
    public static void main(String[] args) {
        Integer[] keys = AlgorithmsUtils.randomArray(20, 30);
        System.out.println(sortedArrayToBST(keys));
    }

    public static TreeNode<Integer> sortedArrayToBST(Integer[] nums) {
        TreeNode<Integer> root = null;
        for (int low = 0, high = nums.length - 1; low <= high; low++, high--) {
            int middle = low + (high - low) / 2;
            if (root == null) {
                root = new TreeNode<Integer>(nums[middle]);
            } else {
                rebuild(root, nums[middle]);
            }
        }
        return root;
    }

    public static TreeNode<Integer> rebuild(TreeNode<Integer> node, int data) {
        if (node == null) {
            return new TreeNode<Integer>(data);
        }
        if (data > node.data) {
            node.right = rebuild(node.right, data);
        } else if (data < node.data) {
            node.left = rebuild(node.left, data);
        }
        return node;
    }
}
