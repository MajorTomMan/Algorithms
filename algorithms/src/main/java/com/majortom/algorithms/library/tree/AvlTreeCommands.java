package com.majortom.algorithms.library.tree;

import com.majortom.algorithms.core.logging.Log;
import com.majortom.algorithms.core.runtime.ExecutionEvents;
import com.majortom.algorithms.library.basic.tree.AVLTree;
import com.majortom.algorithms.library.basic.tree.AVLTreeNode;

import java.util.ArrayList;
import java.util.List;

public final class AvlTreeCommands implements TreeAlgorithm<Integer, AvlTreeInput, AvlTreeOutput> {
    @Override
    public AvlTreeOutput execute(AvlTreeInput input) {
        Log.i("AVL", "Command batch start, commands=" + input.commands().size());
        AVLTree<Integer> tree;
        if (input.initialRoot() != null) {
            tree = AVLTree.fromRestoredRoot(reconstructNode(input.initialRoot()));
        } else {
            tree = new AVLTree<>();
            for (int value : input.initialValues()) {
                tree.insert(value);
            }
        }
        for (AvlCommand command : input.commands()) {
            ExecutionEvents.checkpoint();
            if (command.operation() == AvlCommand.Operation.INSERT) {
                tree.insert(command.value());
            } else {
                tree.remove(command.value());
            }
        }
        List<Integer> values = new ArrayList<>();
        traverse(tree.root(), values);
        AvlTreeOutput output = new AvlTreeOutput(snapshot(tree.root()), values);
        Log.i("AVL", "Command batch completed, size=" + values.size());
        return output;
    }

    private AVLTreeNode<Integer> reconstructNode(AvlNodeSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        AVLTreeNode<Integer> left = reconstructNode(snapshot.left());
        AVLTreeNode<Integer> right = reconstructNode(snapshot.right());
        return new AVLTreeNode<>(snapshot.id(), snapshot.value(), snapshot.height(), left, right);
    }

    private AvlNodeSnapshot snapshot(AVLTreeNode<Integer> node) {
        if (node == null) {
            return null;
        }
        return new AvlNodeSnapshot(node.getId(), node.getValue(), node.getHeight(), snapshot(left(node)), snapshot(right(node)));
    }

    private void traverse(AVLTreeNode<Integer> node, List<Integer> values) {
        if (node == null) {
            return;
        }
        traverse(left(node), values);
        values.add(node.getValue());
        traverse(right(node), values);
    }

    @SuppressWarnings("unchecked")
    private AVLTreeNode<Integer> left(AVLTreeNode<Integer> node) {
        return node == null ? null : (AVLTreeNode<Integer>) node.getLeft();
    }

    @SuppressWarnings("unchecked")
    private AVLTreeNode<Integer> right(AVLTreeNode<Integer> node) {
        return node == null ? null : (AVLTreeNode<Integer>) node.getRight();
    }
}
