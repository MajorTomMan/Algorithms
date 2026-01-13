package com.majortom.algorithms.core.tree.impl;

import com.majortom.algorithms.core.tree.BaseBalancedTree;
import com.majortom.algorithms.core.tree.BaseTree;
import com.majortom.algorithms.core.tree.node.AVLTreeNode;
import com.majortom.algorithms.core.tree.node.BinaryTreeNode;
import com.majortom.algorithms.core.tree.node.TreeNode;

/**
 * AVL 树具体实现
 * 适配说明：完全对接 BaseTree 的原子操作，实现统计自治，优化了类型强转。
 */
public class AVLTree<T extends Comparable<T>> extends BaseBalancedTree<T> {

    @Override
    protected TreeNode<T> createNode(T data) {
        return new AVLTreeNode<>(data);
    }

    // --- 外部接口实现 ---

    @Override
    public void put(BaseTree<T> tree, T val) {
        tree.setRoot(doPut(tree, (BinaryTreeNode<T>) tree.getRoot(), val));
        syncTree(tree, null, null);
    }

    @Override
    public void remove(BaseTree<T> tree, T val) {
        tree.setRoot(doRemove(tree, (BinaryTreeNode<T>) tree.getRoot(), val));
        syncTree(tree, null, null);
    }

    @Override
    public TreeNode<T> search(BaseTree<T> tree, T val) {
        return doSearch(tree, (BinaryTreeNode<T>) tree.getRoot(), val);
    }

    // --- 内部递归逻辑 ---

    private BinaryTreeNode<T> doPut(BaseTree<T> tree, BinaryTreeNode<T> node, T data) {
        if (node == null) {
            // 🚩 自动增加 actionCount 并发射信号
            tree.modifyStructure(null);
            return (BinaryTreeNode<T>) createNode(data);
        }

        // 🚩 自动处理 compareCount 并高亮扫描路径
        syncTree(tree, node, null);

        int cmp = data.compareTo(node.data);
        if (cmp < 0) {
            node.left = doPut(tree, (BinaryTreeNode<T>) node.left, data);
        } else if (cmp > 0) {
            node.right = doPut(tree, (BinaryTreeNode<T>) node.right, data);
        } else {
            return node;
        }

        updateMetrics(node);
        return rebalance(tree, node);
    }

    private BinaryTreeNode<T> doRemove(BaseTree<T> tree, BinaryTreeNode<T> node, T val) {
        if (node == null)
            return null;

        syncTree(tree, node, null);

        int cmp = val.compareTo(node.data);
        if (cmp < 0) {
            node.left = doRemove(tree, (BinaryTreeNode<T>) node.left, val);
        } else if (cmp > 0) {
            node.right = doRemove(tree, (BinaryTreeNode<T>) node.right, val);
        } else {
            tree.modifyStructure(tree.getRoot()); // 标记一次结构变更

            if (node.left == null || node.right == null) {
                node = (node.left != null) ? (BinaryTreeNode<T>) node.left : (BinaryTreeNode<T>) node.right;
            } else {
                BinaryTreeNode<T> successor = (BinaryTreeNode<T>) findMin(tree, node.right);
                node.data = successor.data;
                syncTree(tree, node, successor);
                node.right = doRemove(tree, (BinaryTreeNode<T>) node.right, successor.data);
            }
        }

        if (node == null)
            return null;
        updateMetrics(node);
        return rebalance(tree, node);
    }

    private BinaryTreeNode<T> doSearch(BaseTree<T> tree, BinaryTreeNode<T> node, T val) {
        if (node == null)
            return null;

        syncTree(tree, node, null);

        int cmp = val.compareTo(node.data);
        if (cmp == 0)
            return node;

        return cmp < 0 ? doSearch(tree, (BinaryTreeNode<T>) node.left, val)
                : doSearch(tree, (BinaryTreeNode<T>) node.right, val);
    }

    // --- 平衡判定与重整 ---

    private BinaryTreeNode<T> rebalance(BaseTree<T> tree, BinaryTreeNode<T> node) {
        int balance = getBalance(node);

        // LL Case
        if (balance > 1 && getBalance(node.left) >= 0) {
            return rightRotation(tree, node);
        }
        // RR Case
        if (balance < -1 && getBalance(node.right) <= 0) {
            return leftRotation(tree, node);
        }
        // LR Case
        if (balance > 1 && getBalance(node.left) < 0) {
            return leftRightRotation(tree, node);
        }
        // RL Case
        if (balance < -1 && getBalance(node.right) > 0) {
            return rightLeftRotation(tree, node);
        }

        return node;
    }

    private int getBalance(TreeNode<T> node) {
        if (node == null)
            return 0;
        BinaryTreeNode<T> bn = (BinaryTreeNode<T>) node;
        return height(bn.left) - height(bn.right);
    }

    private TreeNode<T> findMin(BaseTree<T> tree, TreeNode<T> node) {
        BinaryTreeNode<T> current = (BinaryTreeNode<T>) node;
        while (current.left != null) {
            syncTree(tree, current, null);
            current = (BinaryTreeNode<T>) current.left;
        }
        return current;
    }

    @Override
    public void traverse(BaseTree<T> tree) {
        // 实现层序遍历
    }
}