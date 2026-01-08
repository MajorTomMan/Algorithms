package com.majortom.algorithms.app.leetcode.nonlinear;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.majortom.algorithms.core.basic.node.TreeNode;
import com.majortom.algorithms.utils.AlgorithmsUtils;

public class 二叉树的中序遍历 {
    public static void main(String[] args) {
        TreeNode<Integer, Integer> root = AlgorithmsUtils.buildTree(AlgorithmsUtils.randomArray(20, 30),
                AlgorithmsUtils.randomArray(20, 30));
        inorderTraversal(root);
    }

    public static List<Integer> inorderTraversal(TreeNode<Integer, Integer> root) {
        List<Integer> list = new ArrayList<>();
        inorderTraversal(root, list);
        return list;
    }

    private static TreeNode<Integer, Integer> inorderTraversal(TreeNode<Integer, Integer> node, List<Integer> list) {
        if (node == null) {
            return node;
        }
        inorderTraversal(node.left, list);
        list.add(node.value);
        inorderTraversal(node.right, list);
        return node;
    }
}
