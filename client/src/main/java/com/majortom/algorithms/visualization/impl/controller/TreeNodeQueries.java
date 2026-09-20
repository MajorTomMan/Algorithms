package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.structure.tree.AVLTreeNode;
import com.majortom.algorithms.structure.tree.GeneralTreeNode;
import com.majortom.algorithms.visualization.runtime.tree.TreeViewState;

/** Read-only node lookup and depth queries shared by tree controller and snapshot mapper. */
final class TreeNodeQueries {
    private TreeNodeQueries() {}

    @SuppressWarnings("unchecked")
    static AVLTreeNode<Object> left(AVLTreeNode<Object> node) {
        if (node == null || node.getLeft() == null) {
            return null;
        }
        return (AVLTreeNode<Object>) node.getLeft();
    }

    @SuppressWarnings("unchecked")
    static AVLTreeNode<Object> right(AVLTreeNode<Object> node) {
        if (node == null || node.getRight() == null) {
            return null;
        }
        return (AVLTreeNode<Object>) node.getRight();
    }

    static int avlHeight(AVLTreeNode<Object> node) {
        if (node == null) {
            return 0;
        }
        return node.getHeight();
    }

    static AVLTreeNode<Object> avlNodeById(AVLTreeNode<Object> node, long id) {
        if (node == null) {
            return null;
        }
        if (node.getId() == id) {
            return node;
        }
        AVLTreeNode<Object> found = avlNodeById(left(node), id);
        if (found != null) {
            return found;
        }
        return avlNodeById(right(node), id);
    }

    static GeneralTreeNode<Object> generalParentOf(
            GeneralTreeNode<Object> root,
            GeneralTreeNode<Object> target) {
        if (root == null) {
            return null;
        }
        for (GeneralTreeNode<Object> child : root.getChildren()) {
            if (child == target) {
                return root;
            }
            GeneralTreeNode<Object> found = generalParentOf(child, target);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    static AVLTreeNode<Object> avlParentOf(AVLTreeNode<Object> root, AVLTreeNode<Object> target) {
        if (root == null) {
            return null;
        }
        if (left(root) == target || right(root) == target) {
            return root;
        }
        AVLTreeNode<Object> found = avlParentOf(left(root), target);
        if (found != null) {
            return found;
        }
        return avlParentOf(right(root), target);
    }

    static int generalDepthOf(GeneralTreeNode<Object> root, GeneralTreeNode<Object> target, int depth) {
        if (root == null) {
            return -1;
        }
        if (root == target) {
            return depth;
        }
        for (GeneralTreeNode<Object> child : root.getChildren()) {
            int found = generalDepthOf(child, target, depth + 1);
            if (found >= 0) {
                return found;
            }
        }
        return -1;
    }

    static int avlDepthOf(AVLTreeNode<Object> root, AVLTreeNode<Object> target, int depth) {
        if (root == null) {
            return -1;
        }
        if (root == target) {
            return depth;
        }
        int leftDepth = avlDepthOf(left(root), target, depth + 1);
        if (leftDepth >= 0) {
            return leftDepth;
        }
        return avlDepthOf(right(root), target, depth + 1);
    }

    static Long presentationParentId(TreeViewState state, long nodeId) {
        for (TreeViewState.Node candidate : state.nodes().values()) {
            if (state.childrenOf(candidate).contains(nodeId)) {
                return candidate.id();
            }
        }
        return null;
    }

    static int presentationDepth(TreeViewState state, long nodeId) {
        return presentationDepth(state, state.rootId(), nodeId, 0, new java.util.HashSet<>());
    }

    static int presentationDepth(
            TreeViewState state,
            Long currentId,
            long targetId,
            int depth,
            java.util.Set<Long> visited) {
        if (currentId == null || !visited.add(currentId)) {
            return -1;
        }
        if (currentId == targetId) {
            return depth;
        }
        TreeViewState.Node current = state.nodes().get(currentId);
        if (current == null) {
            return -1;
        }
        for (Long childId : state.childrenOf(current)) {
            int found = presentationDepth(state, childId, targetId, depth + 1, visited);
            if (found >= 0) {
                return found;
            }
        }
        return -1;
    }
}
