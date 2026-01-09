package com.majortom.algorithms.app.leetcode.ds.tree;

import java.util.ArrayList;
import java.util.List;

import com.majortom.algorithms.core.tree.node.TreeNode;
import com.majortom.algorithms.utils.AlgorithmsUtils;

public class 二叉树的后序遍历 {
    public static void main(String[] args) {
        Integer[] nums = { 1, null, 2, 3 };
        TreeNode<Integer> root = AlgorithmsUtils.buildBST(nums);
        System.out.println(postTraversal(root));
    }

    public static List<Integer> postTraversal(TreeNode<Integer> root) {
        List<Integer> list = new ArrayList<>();
        postTraversal(root, list);
        return list;
    }

    public static void postTraversal(TreeNode<Integer> root, List<Integer> list) {
        if (root == null) {
            return;
        }
        postTraversal(root.left, list);
        postTraversal(root.right, list);
        list.add((Integer) root.data);
    }
}
