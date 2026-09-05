package com.majortom.algorithms.library.basic.tree;

import com.majortom.algorithms.core.runtime.StructureEvents;
import com.majortom.algorithms.library.structure.SearchTreeStructure;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;

public final class AVLTree<T extends Comparable<? super T>> implements SearchTreeStructure<T> {
    private AVLTreeNode<T> root;
    private int size;
    private long nextNodeId = 1L;

    public static <T extends Comparable<? super T>> AVLTree<T> fromRestoredRoot(AVLTreeNode<T> restoredRoot) {
        Set<AVLTreeNode<T>> identities = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Long> nodeIds = new HashSet<>();
        Validation validation = validateRestored(restoredRoot, null, null, identities, nodeIds);
        AVLTree<T> tree = new AVLTree<>();
        tree.root = restoredRoot;
        tree.size = validation.count();
        tree.nextNodeId = Math.max(1L, validation.maxId() + 1L);
        return tree;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public AVLTreeNode<T> root() {
        return root;
    }

    @Override
    public AVLTreeNode<T> find(T value) {
        Objects.requireNonNull(value, "value");
        AVLTreeNode<T> current = root;
        while (current != null) {
            int comparison = value.compareTo(current.getValue());
            if (comparison == 0) {
                return current;
            }
            current = comparison < 0 ? left(current) : right(current);
        }
        return null;
    }

    @Override
    public void insert(T value) {
        Objects.requireNonNull(value, "value");
        boolean[] inserted = {false};
        AVLTreeNode<T> changed = insert(root, value, inserted);
        setRoot(changed);
        if (inserted[0]) {
            size++;
        }
    }

    @Override
    public boolean remove(T value) {
        Objects.requireNonNull(value, "value");
        boolean[] removed = {false};
        AVLTreeNode<T> changed = remove(root, value, removed);
        setRoot(changed);
        if (removed[0]) {
            size--;
        }
        return removed[0];
    }

    public void rotateLeft(TreeNode<T> node) {
        AVLTreeNode<T> target = requireAvlNode(node);
        setRoot(rotateAt(root, target.getId(), true));
    }

    public void rotateRight(TreeNode<T> node) {
        AVLTreeNode<T> target = requireAvlNode(node);
        setRoot(rotateAt(root, target.getId(), false));
    }

    private AVLTreeNode<T> insert(AVLTreeNode<T> node, T value, boolean[] inserted) {
        if (node == null) {
            AVLTreeNode<T> created = new AVLTreeNode<>(nextNodeId++, value);
            inserted[0] = true;
            StructureEvents.treeNodeInserted(created.getId(), value);
            return created;
        }
        int comparison = value.compareTo(node.getValue());
        if (comparison < 0) {
            node.setLeft(insert(left(node), value, inserted));
        } else if (comparison > 0) {
            node.setRight(insert(right(node), value, inserted));
        } else {
            return node;
        }
        return rebalance(node);
    }

    private AVLTreeNode<T> remove(AVLTreeNode<T> node, T value, boolean[] removed) {
        if (node == null) {
            return null;
        }
        int comparison = value.compareTo(node.getValue());
        if (comparison < 0) {
            node.setLeft(remove(left(node), value, removed));
        } else if (comparison > 0) {
            node.setRight(remove(right(node), value, removed));
        } else if (left(node) == null || right(node) == null) {
            AVLTreeNode<T> replacement = left(node) != null ? left(node) : right(node);
            removed[0] = true;
            StructureEvents.treeNodeRemoved(node.getId(), node.getValue());
            return replacement;
        } else {
            AVLTreeNode<T> successor = minimum(right(node));
            node.setValue(successor.getValue());
            boolean[] successorRemoved = {false};
            node.setRight(remove(right(node), successor.getValue(), successorRemoved));
            removed[0] = successorRemoved[0];
        }
        return rebalance(node);
    }

    private AVLTreeNode<T> rebalance(AVLTreeNode<T> node) {
        update(node);
        int balance = height(left(node)) - height(right(node));
        if (balance > 1) {
            if (height(left(left(node))) < height(right(left(node)))) {
                node.setLeft(rotateLeftInternal(left(node)));
            }
            return rotateRightInternal(node);
        }
        if (balance < -1) {
            if (height(right(right(node))) < height(left(right(node)))) {
                node.setRight(rotateRightInternal(right(node)));
            }
            return rotateLeftInternal(node);
        }
        return node;
    }

    private AVLTreeNode<T> rotateLeftInternal(AVLTreeNode<T> node) {
        AVLTreeNode<T> replacement = right(node);
        if (replacement == null) {
            return node;
        }
        node.setRight(replacement.getLeft());
        replacement.setLeft(node);
        update(node);
        update(replacement);
        return replacement;
    }

    private AVLTreeNode<T> rotateRightInternal(AVLTreeNode<T> node) {
        AVLTreeNode<T> replacement = left(node);
        if (replacement == null) {
            return node;
        }
        node.setLeft(replacement.getRight());
        replacement.setRight(node);
        update(node);
        update(replacement);
        return replacement;
    }

    private AVLTreeNode<T> rotateAt(AVLTreeNode<T> node, long id, boolean leftRotation) {
        if (node == null) {
            return null;
        }
        if (node.getId() == id) {
            return leftRotation ? rotateLeftInternal(node) : rotateRightInternal(node);
        }
        node.setLeft(rotateAt(left(node), id, leftRotation));
        node.setRight(rotateAt(right(node), id, leftRotation));
        update(node);
        return node;
    }

    private AVLTreeNode<T> minimum(AVLTreeNode<T> node) {
        AVLTreeNode<T> current = node;
        while (left(current) != null) {
            current = left(current);
        }
        return current;
    }

    private static <T extends Comparable<? super T>> Validation validateRestored(
            AVLTreeNode<T> node,
            T lowerExclusive,
            T upperExclusive,
            Set<AVLTreeNode<T>> identities,
            Set<Long> nodeIds) {
        if (node == null) {
            return Validation.empty();
        }
        if (!identities.add(node)) {
            throw new IllegalArgumentException("restored AVL tree contains a cycle or reused node reference");
        }
        if (!nodeIds.add(node.getId())) {
            throw new IllegalArgumentException("restored AVL tree contains duplicate node id: " + node.getId());
        }
        T value = Objects.requireNonNull(node.getValue(), "restored AVL node value");
        if (lowerExclusive != null && value.compareTo(lowerExclusive) <= 0) {
            throw new IllegalArgumentException("restored AVL tree violates BST lower bound at node " + node.getId());
        }
        if (upperExclusive != null && value.compareTo(upperExclusive) >= 0) {
            throw new IllegalArgumentException("restored AVL tree violates BST upper bound at node " + node.getId());
        }

        @SuppressWarnings("unchecked")
        AVLTreeNode<T> left = (AVLTreeNode<T>) node.getLeft();
        @SuppressWarnings("unchecked")
        AVLTreeNode<T> right = (AVLTreeNode<T>) node.getRight();
        Validation leftValidation = validateRestored(left, lowerExclusive, value, identities, nodeIds);
        Validation rightValidation = validateRestored(right, value, upperExclusive, identities, nodeIds);
        int expectedHeight = Math.max(leftValidation.height(), rightValidation.height()) + 1;
        int expectedCount = leftValidation.count() + rightValidation.count() + 1;
        if (node.getHeight() != expectedHeight) {
            throw new IllegalArgumentException("restored AVL height mismatch at node " + node.getId()
                    + ": expected " + expectedHeight + ", actual " + node.getHeight());
        }
        if (node.getSubTreeCount() != expectedCount) {
            throw new IllegalArgumentException("restored AVL subtree count mismatch at node " + node.getId()
                    + ": expected " + expectedCount + ", actual " + node.getSubTreeCount());
        }
        if (Math.abs(leftValidation.height() - rightValidation.height()) > 1) {
            throw new IllegalArgumentException("restored AVL balance factor is invalid at node " + node.getId());
        }
        long maxId = Math.max(node.getId(), Math.max(leftValidation.maxId(), rightValidation.maxId()));
        return new Validation(expectedHeight, expectedCount, maxId);
    }

    private record Validation(int height, int count, long maxId) {
        private static Validation empty() {
            return new Validation(0, 0, 0L);
        }
    }

    private void update(AVLTreeNode<T> node) {
        if (node == null) {
            return;
        }
        node.updateMetrics(Math.max(height(left(node)), height(right(node))) + 1, count(left(node)) + count(right(node)) + 1);
    }

    private void setRoot(AVLTreeNode<T> newRoot) {
        if (root == newRoot) {
            return;
        }
        Long previousId = root == null ? null : root.getId();
        root = newRoot;
        StructureEvents.treeRootChanged(previousId, root == null ? null : root.getId());
    }

    private int height(AVLTreeNode<T> node) {
        return node == null ? 0 : node.getHeight();
    }

    private int count(AVLTreeNode<T> node) {
        return node == null ? 0 : node.getSubTreeCount();
    }

    private long maxId(AVLTreeNode<T> node) {
        return node == null ? 0L : Math.max(node.getId(), Math.max(maxId(left(node)), maxId(right(node))));
    }

    @SuppressWarnings("unchecked")
    private AVLTreeNode<T> left(AVLTreeNode<T> node) {
        return node == null ? null : (AVLTreeNode<T>) node.getLeft();
    }

    @SuppressWarnings("unchecked")
    private AVLTreeNode<T> right(AVLTreeNode<T> node) {
        return node == null ? null : (AVLTreeNode<T>) node.getRight();
    }

    @SuppressWarnings("unchecked")
    private AVLTreeNode<T> requireAvlNode(TreeNode<T> node) {
        if (!(node instanceof AVLTreeNode<?>)) {
            throw new IllegalArgumentException("node must be an AVLTreeNode");
        }
        return (AVLTreeNode<T>) node;
    }
}
