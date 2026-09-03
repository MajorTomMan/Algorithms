package com.majortom.algorithms.server.request;

import com.majortom.algorithms.library.basic.tree.AVLTree;
import com.majortom.algorithms.library.basic.tree.AVLTreeNode;
import com.majortom.algorithms.library.tree.AvlCommand;
import com.majortom.algorithms.library.tree.AvlNodeSnapshot;
import com.majortom.algorithms.library.tree.AvlTreeCommands;

import java.util.List;
import java.util.Objects;

/** HTTP request body converted by the Server into an isolated AVLTree plus commands. */
public record AvlTreeRequest(AvlNodeSnapshot initialRoot, List<Integer> initialValues, List<AvlCommand> commands) {

    public AvlTreeRequest {
        initialValues = List.copyOf(Objects.requireNonNull(initialValues, "initialValues"));
        commands = List.copyOf(Objects.requireNonNull(commands, "commands"));
        if (initialRoot != null && !initialValues.isEmpty()) {
            throw new IllegalArgumentException("initialRoot and initialValues are mutually exclusive");
        }
        long initialCount = initialRoot == null ? initialValues.size() : count(initialRoot);
        if (initialCount + commands.size() > AvlTreeCommands.MAX_OPERATIONS) {
            throw new IllegalArgumentException("AVL input must contain at most " + AvlTreeCommands.MAX_OPERATIONS + " operations");
        }
    }

    public AVLTree<Integer> toTree() {
        if (initialRoot != null) {
            return AVLTree.fromRestoredRoot(reconstruct(initialRoot));
        }
        AVLTree<Integer> tree = new AVLTree<>();
        for (Integer value : initialValues) {
            tree.insert(value);
        }
        return tree;
    }

    private static AVLTreeNode<Integer> reconstruct(AvlNodeSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        return new AVLTreeNode<>(snapshot.id(), snapshot.value(), snapshot.height(),
                reconstruct(snapshot.left()), reconstruct(snapshot.right()));
    }

    private static long count(AvlNodeSnapshot node) {
        if (node == null) {
            return 0L;
        }
        return 1L + count(node.left()) + count(node.right());
    }
}
