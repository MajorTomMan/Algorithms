package com.majortom.algorithms.library.structure;

import com.majortom.algorithms.core.runtime.ExecutionEvents;
import com.majortom.algorithms.library.basic.tree.AVLTreeNode;
import com.majortom.algorithms.library.basic.tree.TreeNode;
import com.majortom.algorithms.library.structure.event.TreeStructureEvent;

import java.util.Objects;

public final class MutableAvlTree<T extends Comparable<? super T>> implements RotatableTree<T> {
    private AVLTreeNode<T> root;
    private int size;
    private long nextNodeId = 1L;

    @Override public int size() { return size; }
    @Override public boolean isEmpty() { return size == 0; }
    @Override public AVLTreeNode<T> raw() { return root; }

    @Override
    public AVLTreeNode<T> find(T value) {
        Objects.requireNonNull(value, "value");
        AVLTreeNode<T> current = root;
        while (current != null) {
            int comparison = value.compareTo(current.data);
            if (comparison == 0) return current;
            current = comparison < 0 ? left(current) : right(current);
        }
        return null;
    }

    @Override
    public void insert(T value) {
        Objects.requireNonNull(value, "value");
        boolean[] inserted = {false};
        root = insert(root, value, inserted);
        if (inserted[0]) size++;
    }

    @Override
    public boolean remove(T value) {
        Objects.requireNonNull(value, "value");
        boolean[] removed = {false};
        root = remove(root, value, removed, true);
        if (removed[0]) size--;
        return removed[0];
    }

    @Override
    public void rotateLeft(TreeNode<T> node) {
        if (!(node instanceof AVLTreeNode<?>)) throw new IllegalArgumentException("node must be an AVLTreeNode");
        @SuppressWarnings("unchecked") AVLTreeNode<T> target = (AVLTreeNode<T>) node;
        root = rotateAt(root, target.id, true);
    }

    @Override
    public void rotateRight(TreeNode<T> node) {
        if (!(node instanceof AVLTreeNode<?>)) throw new IllegalArgumentException("node must be an AVLTreeNode");
        @SuppressWarnings("unchecked") AVLTreeNode<T> target = (AVLTreeNode<T>) node;
        root = rotateAt(root, target.id, false);
    }

    public void restore(AVLTreeNode<T> restoredRoot) {
        root = restoredRoot;
        size = count(root);
        nextNodeId = Math.max(1L, maxId(root) + 1L);
        normalize(root);
    }

    private AVLTreeNode<T> insert(AVLTreeNode<T> node, T value, boolean[] inserted) {
        if (node == null) {
            AVLTreeNode<T> created = new AVLTreeNode<>(nextNodeId++, value);
            inserted[0] = true;
            ExecutionEvents.emit(new TreeStructureEvent.Inserted(created.id, value));
            return created;
        }
        int comparison = value.compareTo(node.data);
        if (comparison < 0) node.left = insert(left(node), value, inserted);
        else if (comparison > 0) node.right = insert(right(node), value, inserted);
        else return node;
        return rebalance(node);
    }

    private AVLTreeNode<T> remove(AVLTreeNode<T> node, T value, boolean[] removed, boolean emitRemoval) {
        if (node == null) return null;
        int comparison = value.compareTo(node.data);
        if (comparison < 0) node.left = remove(left(node), value, removed, emitRemoval);
        else if (comparison > 0) node.right = remove(right(node), value, removed, emitRemoval);
        else {
            if (left(node) == null || right(node) == null) {
                AVLTreeNode<T> replacement = left(node) != null ? left(node) : right(node);
                if (emitRemoval) ExecutionEvents.emit(new TreeStructureEvent.Removed(node.id, node.data));
                removed[0] = true;
                return replacement;
            }
            AVLTreeNode<T> successor = minimum(right(node));
            T oldValue = node.data;
            node.data = successor.data;
            boolean[] successorRemoved = {false};
            node.right = remove(right(node), successor.data, successorRemoved, false);
            if (emitRemoval) ExecutionEvents.emit(new TreeStructureEvent.Removed(successor.id, oldValue));
            removed[0] = true;
        }
        return rebalance(node);
    }

    private AVLTreeNode<T> rebalance(AVLTreeNode<T> node) {
        update(node);
        int balance = height(left(node)) - height(right(node));
        if (balance > 1) {
            if (height(left(left(node))) < height(right(left(node)))) node.left = rotateLeftInternal(left(node));
            return rotateRightInternal(node);
        }
        if (balance < -1) {
            if (height(right(right(node))) < height(left(right(node)))) node.right = rotateRightInternal(right(node));
            return rotateLeftInternal(node);
        }
        return node;
    }

    private AVLTreeNode<T> rotateLeftInternal(AVLTreeNode<T> node) {
        AVLTreeNode<T> replacement = right(node);
        if (replacement == null) return node;
        node.right = replacement.left;
        replacement.left = node;
        update(node);
        update(replacement);
        ExecutionEvents.emit(new TreeStructureEvent.RotatedLeft(node.id, replacement.id));
        return replacement;
    }

    private AVLTreeNode<T> rotateRightInternal(AVLTreeNode<T> node) {
        AVLTreeNode<T> replacement = left(node);
        if (replacement == null) return node;
        node.left = replacement.right;
        replacement.right = node;
        update(node);
        update(replacement);
        ExecutionEvents.emit(new TreeStructureEvent.RotatedRight(node.id, replacement.id));
        return replacement;
    }

    private AVLTreeNode<T> rotateAt(AVLTreeNode<T> node, long id, boolean leftRotation) {
        if (node == null) return null;
        if (node.id == id) return leftRotation ? rotateLeftInternal(node) : rotateRightInternal(node);
        node.left = rotateAt(left(node), id, leftRotation);
        node.right = rotateAt(right(node), id, leftRotation);
        update(node);
        return node;
    }

    private AVLTreeNode<T> minimum(AVLTreeNode<T> node) {
        AVLTreeNode<T> current = node;
        while (left(current) != null) current = left(current);
        return current;
    }

    private void normalize(AVLTreeNode<T> node) {
        if (node == null) return;
        normalize(left(node));
        normalize(right(node));
        update(node);
    }

    private void update(AVLTreeNode<T> node) {
        if (node == null) return;
        node.height = Math.max(height(left(node)), height(right(node))) + 1;
        node.subTreeCount = count(left(node)) + count(right(node)) + 1;
    }

    private int height(AVLTreeNode<T> node) { return node == null ? 0 : node.height; }
    private int count(AVLTreeNode<T> node) { return node == null ? 0 : 1 + count(left(node)) + count(right(node)); }
    private long maxId(AVLTreeNode<T> node) { return node == null ? 0L : Math.max(node.id, Math.max(maxId(left(node)), maxId(right(node)))); }

    @SuppressWarnings("unchecked") private AVLTreeNode<T> left(AVLTreeNode<T> node) { return node == null ? null : (AVLTreeNode<T>) node.left; }
    @SuppressWarnings("unchecked") private AVLTreeNode<T> right(AVLTreeNode<T> node) { return node == null ? null : (AVLTreeNode<T>) node.right; }
}
