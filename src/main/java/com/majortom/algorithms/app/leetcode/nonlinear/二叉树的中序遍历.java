package com.majortom.algorithms.app.leetcode.nonlinear;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.majortom.algorithms.core.tree.node.TreeNode;
import com.majortom.algorithms.utils.AlgorithmsUtils;

public class 二叉树的中序遍历 {
    public static void main(String[] args) {
        TreeNode<Integer> root = AlgorithmsUtils.buildBST(AlgorithmsUtils.randomArray(20, 30));
        inorderTraversal(root);
    }

    public static List<Integer> inorderTraversal(TreeNode<Integer> root) {
        List<Integer> list = new ArrayList<>();
        inorderTraversal(root, list);
        return list;
    }

    private static TreeNode<Integer> inorderTraversal(TreeNode<Integer> node, List<Integer> list) {
        if (node == null) {
            return node;
        }
        inorderTraversal(node.left, list);
        list.add(node.data);
        inorderTraversal(node.right, list);
        return node;
    }
}
