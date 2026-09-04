package com.majortom.algorithms.library.tree;

import com.majortom.algorithms.core.logging.Log;
import com.majortom.algorithms.library.basic.tree.AVLTree;

import java.util.List;
import java.util.Objects;

/** Applies an ordered command batch directly to an isolated AVL tree. */
public final class AvlTreeCommands implements TreeAlgorithm<Integer> {

    public static final int MAX_OPERATIONS = 100_000;

    public void execute(AVLTree<Integer> tree, List<AvlCommand> commands) {
        Objects.requireNonNull(tree, "tree");
        Objects.requireNonNull(commands, "commands");
        if ((long) tree.size() + commands.size() > MAX_OPERATIONS) {
            throw new IllegalArgumentException("AVL input must contain at most " + MAX_OPERATIONS + " operations");
        }
        Log.i("AVL", "Command batch start, commands=" + commands.size());
        for (AvlCommand command : commands) {
            if (command.operation() == AvlCommand.Operation.INSERT) {
                tree.insert(command.value());
            } else {
                tree.remove(command.value());
            }
        }
        Log.i("AVL", "Command batch completed, size=" + tree.size());
    }
}
