package com.majortom.algorithms.app.leetcode.ds.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.majortom.algorithms.core.tree.node.BinaryTreeNode;
import com.majortom.algorithms.utils.AlgorithmsUtils;

public class 二叉树的中序遍历 {
    public static void main(String[] args) {
        BinaryTreeNode<Integer> root = AlgorithmsUtils.buildBST(AlgorithmsUtils.randomArray(20, 30));
        inorderTraversal(root);
    }

    public static List<Integer> inorderTraversal(BinaryTreeNode<Integer> root) {
        List<Integer> list = new ArrayList<>();
        inorderTraversal(root, list);
        return list;
    }

    private static BinaryTreeNode<Integer> inorderTraversal(BinaryTreeNode<Integer> node, List<Integer> list) {
        if (node == null) {
            return node;
        }
        inorderTraversal(node.left, list);
        list.add(node.data);
        inorderTraversal(node.right, list);
        return node;
    }
}
