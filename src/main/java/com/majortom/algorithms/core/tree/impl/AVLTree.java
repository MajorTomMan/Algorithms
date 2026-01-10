package com.majortom.algorithms.core.tree.impl;

import java.util.ArrayList;
import java.util.List;
import com.majortom.algorithms.core.tree.node.TreeNode;
import com.majortom.algorithms.core.tree.BaseBalancedTree;
import com.majortom.algorithms.core.tree.node.AVLTreeNode;
import com.majortom.algorithms.core.tree.node.BinaryTreeNode;

/**
 * AVL 树实现
 * 专注于平衡因子的计算与旋转逻辑的触发
 */
public class AVLTree<T extends Comparable<T>> extends BaseBalancedTree<T> {

    @Override
    protected TreeNode<T> createNode(T data) {
        return new AVLTreeNode<>(data);
    }

    @Override
    public void put(T val) {
        root = doPut(val, root);
        syncTree(null, null);
    }

    private TreeNode<T> doPut(T data, TreeNode<T> node) {
        if (node == null) {
            actionCount++;
            return createNode(data);
        }

        compareCount++;
        syncTree(node, null);

        int cmp = data.compareTo(node.data);
        BinaryTreeNode<T> bNode = (BinaryTreeNode<T>) node;

        if (cmp < 0)
            bNode.left = (BinaryTreeNode<T>) doPut(data, bNode.left);
        else if (cmp > 0)
            bNode.right = (BinaryTreeNode<T>) doPut(data, bNode.right);
        else
            return node;

        updateMetrics(node);
        return rebalance(node);
    }

    @Override
    public void remove(T val) {
        // 只有在入口处将根节点强转一次
        root = doRemove(val, (BinaryTreeNode<T>) root);
        syncTree(null, null);
    }

    private TreeNode<T> doRemove(T val, TreeNode<T> node) {
        if (node == null)
            return null;

        compareCount++;
        syncTree(node, null);

        int cmp = val.compareTo(node.data);
        BinaryTreeNode<T> bNode = (BinaryTreeNode<T>) node;

        if (cmp < 0) {
            bNode.left = (BinaryTreeNode<T>) doRemove(val, bNode.left);
        } else if (cmp > 0) {
            bNode.right = (BinaryTreeNode<T>) doRemove(val, bNode.right);
        } else {
            actionCount++;
            if (bNode.left == null || bNode.right == null) {
                node = (bNode.left != null) ? bNode.left : bNode.right;
            } else {
                TreeNode<T> successor = findMin(bNode.right);
                node.data = successor.data;
                syncTree(node, successor);
                bNode.right = (BinaryTreeNode<T>) doRemove(successor.data, bNode.right);
            }
        }

        if (node == null)
            return null;
        updateMetrics(node);
        return rebalance(node);
    }

    private TreeNode<T> rebalance(TreeNode<T> node) {
        int balance = getBalance(node);
        BinaryTreeNode<T> bNode = (BinaryTreeNode<T>) node;

        // LL
        if (balance > 1 && getBalance(bNode.left) >= 0) {
            return rightRotation(node);
        }
        // RR
        if (balance < -1 && getBalance(bNode.right) <= 0) {
            return leftRotation(node);
        }
        // LR
        if (balance > 1 && getBalance(bNode.left) < 0) {
            return leftRightRotation(node);
        }
        // RL
        if (balance < -1 && getBalance(bNode.right) > 0) {
            return rightLeftRotation(node);
        }
        return node;
    }

    private int getBalance(TreeNode<T> node) {
        if (!(node instanceof BinaryTreeNode))
            return 0;
        BinaryTreeNode<T> bn = (BinaryTreeNode<T>) node;
        return height(bn.left) - height(bn.right);
    }

    private TreeNode<T> findMin(TreeNode<T> node) {
        BinaryTreeNode<T> current = (BinaryTreeNode<T>) node;
        while (current.left != null)
            current = (BinaryTreeNode<T>) current.left;
        return current;
    }

    @Override
    public TreeNode<T> search(T val) {
        TreeNode<T> current = root;
        while (current != null) {
            compareCount++;
            syncTree(current, null);
            int cmp = val.compareTo(current.data);
            BinaryTreeNode<T> bNode = (BinaryTreeNode<T>) current;
            if (cmp < 0)
                current = bNode.left;
            else if (cmp > 0)
                current = bNode.right;
            else
                return current;
        }
        return null;
    }

    @Override
    public void traverse() {
        // 实现遍历逻辑
    }

    @Override
    public List<T> toList() {
        List<T> result = new ArrayList<>();
        inOrder(root, result);
        return result;
    }

    private void inOrder(TreeNode<T> node, List<T> list) {
        if (node == null)
            return;
        BinaryTreeNode<T> bNode = (BinaryTreeNode<T>) node;
        inOrder(bNode.left, list);
        list.add(node.data);
        inOrder(bNode.right, list);
    }
}