package com.majortom.algorithms.core.tree.impl;

import java.util.ArrayList;
import java.util.List;
import com.majortom.algorithms.core.interfaces.BalancedTree;
import com.majortom.algorithms.core.tree.node.TreeNode;

public class AVLTree<T extends Comparable<T>> extends BalancedTree<T> {

    @Override
    public void put(T val) {
        root = doPut(val, root);
        this.currentHighlight = null;
        sync(this, null, null);
    }

    private TreeNode<T> doPut(T data, TreeNode<T> node) {
        if (node == null) {
            actionCount++;
            return new TreeNode<>(data);
        }

        // 查找埋点
        compareCount++;
        this.currentHighlight = node;
        sync(this, node, null);

        int cmp = data.compareTo(node.data);
        if (cmp < 0)
            node.left = doPut(data, node.left);
        else if (cmp > 0)
            node.right = doPut(data, node.right);
        else
            return node;

        updateMetrics(node);
        return rebalance(node);
    }

    @Override
    public void remove(T val) {
        root = doRemove(val, root);
        this.currentHighlight = null;
        sync(this, null, null);
    }

    private TreeNode<T> doRemove(T val, TreeNode<T> node) {
        if (node == null)
            return null;

        compareCount++;
        this.currentHighlight = node;
        sync(this, node, null);

        int cmp = val.compareTo(node.data);
        if (cmp < 0) {
            node.left = doRemove(val, node.left);
        } else if (cmp > 0) {
            node.right = doRemove(val, node.right);
        } else {
            actionCount++;
            if (node.left == null || node.right == null) {
                node = (node.left != null) ? node.left : node.right;
            } else {
                TreeNode<T> successor = findMin(node.right);
                node.data = successor.data;
                sync(this, node, successor);
                node.right = doRemove(successor.data, node.right);
            }
        }

        if (node == null)
            return null;
        updateMetrics(node);
        return rebalance(node);
    }

    private TreeNode<T> rebalance(TreeNode<T> node) {
        int balance = getBalance(node);

        // LL
        if (balance > 1 && getBalance(node.left) >= 0) {
            return rightRotation(node); // 调用父类重构后的旋转
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
            this.currentHighlight = current;
            sync(this, current, null);
            int cmp = val.compareTo(current.data);
            if (cmp < 0)
                current = current.left;
            else if (cmp > 0)
                current = current.right;
            else
                return current;
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
        return height(root);
    }

    @Override
    public boolean isEmpty() {
        return root == null;
    }

    @Override
    public void clear() {
        root = null;
        resetStatistics();
        sync(this, null, null);
    }

    @Override
    public List<T> toList() {
        // TODO Auto-generated method stub
        List<T> result = new ArrayList<>();
        inOrder(root, result);
        return result;
    }

    private void inOrder(TreeNode<T> node, List<T> list) {
        if (node == null)
            return;
        inOrder(node.left, list);
        list.add(node.data);
        inOrder(node.right, list);
    }
}