package com.majortom.algorithms.app.leetcode.ds.tree;

import com.majortom.algorithms.library.basic.tree.AVLTreeNode;
import com.majortom.algorithms.library.basic.tree.BinaryTreeNode;
import com.majortom.algorithms.library.utils.AlgorithmsUtils;

public class 有序数组转二叉树 {
    public static void main(String[] args) {
        Integer[] keys = AlgorithmsUtils.randomArray(20, 30);
        System.out.println(sortedArrayToBST(keys));
    }

    public static BinaryTreeNode<Integer> sortedArrayToBST(Integer[] nums) {
        BinaryTreeNode<Integer> root = null;
        for (int low = 0, high = nums.length - 1; low <= high; low++, high--) {
            int middle = low + (high - low) / 2;
            if (root == null) {
                root = new AVLTreeNode<Integer>(nums[middle]);
            } else {
                rebuild(root, nums[middle]);
            }
        }
        return root;
    }

    public static BinaryTreeNode<Integer> rebuild(BinaryTreeNode<Integer> node, int data) {
        if (node == null) {
            return new AVLTreeNode<Integer>(data);
        }
        if (data > node.getValue()) {
            node.setRight(rebuild(node.getRight(), data));
        } else if (data < node.getValue()) {
            node.setLeft(rebuild(node.getLeft(), data));
        }
        return node;
    }
}
