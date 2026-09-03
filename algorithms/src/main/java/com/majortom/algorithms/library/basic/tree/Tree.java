package com.majortom.algorithms.library.basic.tree;

import com.majortom.algorithms.core.event.structure.TreeStructureEvent;
import com.majortom.algorithms.core.runtime.ExecutionEvents;
import com.majortom.algorithms.library.structure.GeneralTreeStructure;

import java.util.ArrayDeque;
import java.util.Objects;

public final class Tree<T> implements GeneralTreeStructure<T> {
    private GeneralTreeNode<T> root;
    private int size;

    @Override
    public int size() {
        return size;
    }

    @Override
    public GeneralTreeNode<T> root() {
        return root;
    }

    @Override
    public GeneralTreeNode<T> addRoot(T value) {
        if (root != null) {
            throw new IllegalStateException("tree already has a root");
        }
        GeneralTreeNode<T> node = new GeneralTreeNode<>(value);
        ExecutionEvents.emit(new TreeStructureEvent.NodeInserted(node.getId(), value));
        root = node;
        size = 1;
        ExecutionEvents.emit(new TreeStructureEvent.RootChanged(null, node.getId()));
        return node;
    }

    @Override
    public GeneralTreeNode<T> addChild(TreeNode<T> parent, T value) {
        GeneralTreeNode<T> node = requireNode(parent);
        return addChild(node, node.getChildren().size(), value);
    }

    @Override
    public GeneralTreeNode<T> addChild(TreeNode<T> parent, int index, T value) {
        GeneralTreeNode<T> node = requireNode(parent);
        if (!contains(node)) {
            throw new IllegalArgumentException("parent does not belong to this tree");
        }
        if (index < 0 || index > node.getChildren().size()) {
            throw new IndexOutOfBoundsException("index=" + index + ", size=" + node.getChildren().size());
        }
        GeneralTreeNode<T> child = new GeneralTreeNode<>(value);
        ExecutionEvents.emit(new TreeStructureEvent.NodeInserted(child.getId(), value));
        node.addChild(index, child);
        size++;
        return child;
    }

    @Override
    public boolean remove(TreeNode<T> node) {
        GeneralTreeNode<T> target = requireNode(node);
        if (target == root) {
            long rootId = root.getId();
            root = null;
            size = 0;
            ExecutionEvents.emit(new TreeStructureEvent.RootChanged(rootId, null));
            emitRemovedSubtree(target);
            return true;
        }
        GeneralTreeNode<T> parent = parentOf(target);
        if (parent == null) {
            return false;
        }
        int removedSize = subtreeSize(target);
        if (!parent.removeChild(target)) {
            return false;
        }
        size -= removedSize;
        emitRemovedSubtree(target);
        return true;
    }

    public GeneralTreeNode<T> findById(long id) {
        if (root == null) {
            return null;
        }
        ArrayDeque<GeneralTreeNode<T>> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            GeneralTreeNode<T> current = queue.removeFirst();
            if (current.getId() == id) {
                return current;
            }
            queue.addAll(current.getChildren());
        }
        return null;
    }

    public GeneralTreeNode<T> findFirstByValue(T value) {
        if (root == null) {
            return null;
        }
        ArrayDeque<GeneralTreeNode<T>> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            GeneralTreeNode<T> current = queue.removeFirst();
            if (Objects.equals(current.getValue(), value)) {
                return current;
            }
            queue.addAll(current.getChildren());
        }
        return null;
    }

    public void restore(GeneralTreeNode<T> restoredRoot) {
        root = restoredRoot;
        size = subtreeSize(restoredRoot);
    }

    private boolean contains(GeneralTreeNode<T> target) {
        return findById(target.getId()) == target;
    }

    private GeneralTreeNode<T> parentOf(GeneralTreeNode<T> target) {
        if (root == null || root == target) {
            return null;
        }
        ArrayDeque<GeneralTreeNode<T>> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            GeneralTreeNode<T> current = queue.removeFirst();
            for (GeneralTreeNode<T> child : current.getChildren()) {
                if (child == target) {
                    return current;
                }
                queue.addLast(child);
            }
        }
        return null;
    }

    private int subtreeSize(GeneralTreeNode<T> node) {
        if (node == null) {
            return 0;
        }
        int count = 1;
        for (GeneralTreeNode<T> child : node.getChildren()) {
            count += subtreeSize(child);
        }
        return count;
    }

    private void emitRemovedSubtree(GeneralTreeNode<T> node) {
        for (GeneralTreeNode<T> child : node.getChildren()) {
            emitRemovedSubtree(child);
        }
        ExecutionEvents.emit(new TreeStructureEvent.NodeRemoved(node.getId(), node.getValue()));
    }

    @SuppressWarnings("unchecked")
    private GeneralTreeNode<T> requireNode(TreeNode<T> node) {
        Objects.requireNonNull(node, "node");
        if (!(node instanceof GeneralTreeNode<?>)) {
            throw new IllegalArgumentException("node must be a GeneralTreeNode");
        }
        return (GeneralTreeNode<T>) node;
    }
}
