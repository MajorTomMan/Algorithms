package com.majortom.algorithms.core.tree.impl;

import com.majortom.algorithms.core.interfaces.BalancedTree;
import com.majortom.algorithms.core.tree.node.TreeNode;

public class AVLTree<T extends Comparable<T>> extends BalancedTree<T> {

    @Override
    public void put(T val) {
        root = doPut(val, root);
    }

    private TreeNode<T> doPut(T val, TreeNode<T> node) {
        if (node == null) {
            TreeNode<T> newNode = new TreeNode<>(val);
            onStep(); // 1. 发现空位，新节点诞生的瞬间
            return newNode;
        }

        compareCount++;
        int cmp = val.compareTo(node.data);
        if (cmp < 0) {
            node.left = doPut(val, node.left);
        } else if (cmp > 0) {
            node.right = doPut(val, node.right);
        }

        updateMetrics(node);

        // 关键点：旋转发生前
        TreeNode<T> balanced = rebalance(node);

        if (balanced != node) {
            onStep(); // 2. 结构发生了旋转突变，触发回调
        }

        return balanced;
    }

    @Override
    public void remove(T val) {
        root = doRemove(val, root);
    }

    private TreeNode<T> doRemove(T val, TreeNode<T> node) {
        if (node == null)
            return null;

        compareCount++;
        int cmp = val.compareTo(node.data);
        if (cmp < 0) {
            node.left = doRemove(val, node.left);
        } else if (cmp > 0) {
            node.right = doRemove(val, node.right);
        } else {
            // 找到节点了！
            actionCount++; // 记录一次有效删除操作
            if (node.left == null || node.right == null) {
                node = (node.left != null) ? node.left : node.right;
                onStep(); // 埋点：节点被替换或删除的瞬间
            } else {
                // 找后继节点
                TreeNode<T> successor = findMin(node.right);
                node.data = successor.data;
                onStep(); // 埋点：观察值被覆盖的过程
                node.right = doRemove(successor.data, node.right);
            }
        }

        if (node == null)
            return null;
        updateMetrics(node);

        TreeNode<T> balanced = rebalance(node);
        if (balanced != node) {
            onStep(); // 埋点：删除导致的失衡被修复（旋转）
        }
        return balanced;
    }

    private TreeNode<T> rebalance(TreeNode<T> node) {
        int balance = getBalance(node);

        // LL
        if (balance > 1 && getBalance(node.left) >= 0) {
            return rightRotation(node);
        }
        // RR
        if (balance < -1 && getBalance(node.right) <= 0) {
            return leftRotation(node);
        }
        // LR
        if (balance > 1 && getBalance(node.left) < 0) {
            return leftRightRotation(node);
        }
        // RL
        if (balance < -1 && getBalance(node.right) > 0) {
            return rightLeftRotation(node);
        }

        return node;
    }

    private int getBalance(TreeNode<T> node) {
        return node == null ? 0 : height(node.left) - height(node.right);
    }

    private TreeNode<T> findMin(TreeNode<T> node) {
        while (node.left != null)
            node = node.left;
        return node;
    }

    @Override
    public TreeNode<T> search(T val) {
        TreeNode<T> current = root;
        while (current != null) {
            compareCount++;
            int cmp = val.compareTo(current.data);
            if (cmp < 0) {
                current = current.left;
            } else if (cmp > 0) {
                current = current.right;
            } else {
                return current;
            }
        }
        return null;
    }

    @Override
    public void traverse() {
    }

    @Override
    public int size() {
        return root == null ? 0 : root.subTreeCount;
    }

    @Override
    public int height() {
        return root == null ? 0 : root.height;
    }

    @Override
    public boolean isEmpty() {
        return root == null;
    }

    @Override
    public void clear() {
        root = null;
        compareCount = 0;
        actionCount = 0;
    }
}