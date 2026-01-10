package com.majortom.algorithms.app.leetcode.others;

import com.majortom.algorithms.core.tree.node.BinaryTreeNode;
import com.majortom.algorithms.core.tree.node.TreeNode;
import com.majortom.algorithms.utils.AlgorithmsUtils;

/**
 * 路径总和
 */
public class 路径总和 {
    public static void main(String[] args) {
        TreeNode<Integer> root = AlgorithmsUtils.buildBST(AlgorithmsUtils.randomArray(20, 30));
        System.out.println(hasPathSum((BinaryTreeNode<Integer>) root, 1));
    }

    public static boolean hasPathSum(BinaryTreeNode<Integer> root, int targetSum) {
        if (root == null) {
            return false;
        }
        targetSum -= root.data;
        if (root.left == null && root.right == null) {
            return targetSum == 0;
        }
        return hasPathSum(root.left, targetSum) || hasPathSum(root.right, targetSum);
    }
}