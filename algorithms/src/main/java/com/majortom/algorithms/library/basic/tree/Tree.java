package com.majortom.algorithms.library.basic.tree;

import com.majortom.algorithms.core.runtime.StructureEvents;
import com.majortom.algorithms.core.snapshot.GeneralTreeSnapshot;
import com.majortom.algorithms.library.structure.GeneralTreeStructure;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;

public final class Tree<T> implements GeneralTreeStructure<T> {
    private GeneralTreeNode<T> root;
    private int size;

    public static <T> Tree<T> fromSnapshot(GeneralTreeSnapshot<T> snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        Set<GeneralTreeSnapshot.Node<T>> identities = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Long> nodeIds = new HashSet<>();
        Restoration<T> restoration = restoreNode(snapshot.root(), identities, nodeIds);
        if (restoration.size() != snapshot.size()) {
            throw new IllegalArgumentException("snapshot size does not match tree topology");
        }
        Tree<T> tree = new Tree<>();
        tree.root = restoration.node();
        tree.size = restoration.size();
        return tree;
    }

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
        StructureEvents.treeNodeInserted(node.getId(), value);
        root = node;
        size = 1;
        StructureEvents.treeRootChanged(null, node.getId());
        return node;
    }

    @Override
    public GeneralTreeNode<T> addChild(GeneralTreeNode<T> parent, T value) {
        GeneralTreeNode<T> node = requireNode(parent);
        return addChild(node, node.getChildren().size(), value);
    }

    @Override
    public GeneralTreeNode<T> addChild(GeneralTreeNode<T> parent, int index, T value) {
        GeneralTreeNode<T> node = requireNode(parent);
        if (!contains(node)) {
            throw new IllegalArgumentException("parent does not belong to this tree");
        }
        if (index < 0 || index > node.getChildren().size()) {
            throw new IndexOutOfBoundsException("index=" + index + ", size=" + node.getChildren().size());
        }
        GeneralTreeNode<T> child = new GeneralTreeNode<>(value);
        StructureEvents.treeNodeInserted(child.getId(), value);
        node.addChild(index, child);
        size++;
        return child;
    }

    @Override
    public T set(GeneralTreeNode<T> node, T value) {
        GeneralTreeNode<T> target = requireMember(node, "node");
        T previous = target.getValue();
        target.setValue(value);
        return previous;
    }

    @Override
    public boolean remove(GeneralTreeNode<T> node) {
        GeneralTreeNode<T> target = requireNode(node);
        if (target == root) {
            long rootId = root.getId();
            root = null;
            size = 0;
            StructureEvents.treeRootChanged(rootId, null);
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

    @Override
    public void move(GeneralTreeNode<T> node, GeneralTreeNode<T> newParent) {
        GeneralTreeNode<T> parent = requireMember(newParent, "newParent");
        move(node, parent, parent.getChildren().size());
    }

    @Override
    public void move(GeneralTreeNode<T> node, GeneralTreeNode<T> newParent, int index) {
        GeneralTreeNode<T> target = requireMember(node, "node");
        GeneralTreeNode<T> parent = requireMember(newParent, "newParent");
        if (target == root) {
            throw new IllegalArgumentException("root cannot be moved below another node");
        }
        if (target == parent || containsInSubtree(target, parent)) {
            throw new IllegalArgumentException("move would create a tree cycle");
        }
        GeneralTreeNode<T> previousParent = parentOf(target);
        if (previousParent == null) {
            throw new IllegalArgumentException("node does not belong to this tree");
        }
        if (index < 0 || index > parent.getChildren().size()) {
            throw new IndexOutOfBoundsException("index=" + index + ", size=" + parent.getChildren().size());
        }
        int previousIndex = previousParent.getChildren().indexOf(target);
        int destinationIndex = index;
        if (previousParent == parent && previousIndex < destinationIndex) {
            destinationIndex--;
        }
        if (previousParent == parent && previousIndex == destinationIndex) {
            return;
        }
        previousParent.removeChild(target);
        parent.addChild(destinationIndex, target);
    }

    private boolean contains(GeneralTreeNode<T> target) {
        return findById(target.getId()) == target;
    }

    private boolean containsInSubtree(GeneralTreeNode<T> root, GeneralTreeNode<T> target) {
        if (root == target) {
            return true;
        }
        for (GeneralTreeNode<T> child : root.getChildren()) {
            if (containsInSubtree(child, target)) {
                return true;
            }
        }
        return false;
    }

    private GeneralTreeNode<T> requireMember(GeneralTreeNode<T> node, String name) {
        GeneralTreeNode<T> target = requireNode(node);
        if (!contains(target)) {
            throw new IllegalArgumentException(name + " does not belong to this tree");
        }
        return target;
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
        StructureEvents.treeNodeRemoved(node.getId(), node.getValue());
    }

    private GeneralTreeNode<T> requireNode(GeneralTreeNode<T> node) {
        return Objects.requireNonNull(node, "node");
    }

    private static <T> Restoration<T> restoreNode(
            GeneralTreeSnapshot.Node<T> node,
            Set<GeneralTreeSnapshot.Node<T>> identities,
            Set<Long> nodeIds) {
        if (node == null) {
            return new Restoration<>(null, 0);
        }
        if (!identities.add(node)) {
            throw new IllegalArgumentException("snapshot reuses the same tree node in multiple locations");
        }
        if (!nodeIds.add(node.id())) {
            throw new IllegalArgumentException("snapshot contains duplicate tree node id: " + node.id());
        }
        java.util.List<GeneralTreeNode<T>> children = new java.util.ArrayList<>(node.children().size());
        int size = 1;
        for (GeneralTreeSnapshot.Node<T> child : node.children()) {
            if (child == null) {
                throw new IllegalArgumentException("general tree snapshot cannot contain null child entries");
            }
            Restoration<T> restoredChild = restoreNode(child, identities, nodeIds);
            children.add(restoredChild.node());
            size += restoredChild.size();
        }
        return new Restoration<>(new GeneralTreeNode<>(node.id(), node.value(), children), size);
    }

    private record Restoration<T>(GeneralTreeNode<T> node, int size) {
    }
}
