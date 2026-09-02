package com.majortom.algorithms.library.tree;

import com.majortom.algorithms.core.logging.Log;
import com.majortom.algorithms.core.runtime.ExecutionEvents;
import com.majortom.algorithms.library.basic.tree.AVLTreeNode;
import com.majortom.algorithms.library.structure.MutableAvlTree;

import java.util.ArrayList;
import java.util.List;

/** Ordered AVL mutations executed against the real mutable AVL structure. */
public final class AvlTreeCommands {

    public AvlTreeOutput execute(AvlTreeInput input) {
        Log.i("AVL", "Command batch start, commands=" + input.commands().size());
        MutableAvlTree<Integer> tree = createTree(input);
        ExecutionEvents.emit(new AvlTreeEvent.Initialized(snapshot(tree.raw())));

        Long focusId = null;
        Integer focusValue = null;
        for (AvlCommand command : input.commands()) {
            ExecutionEvents.checkpoint();
            Log.d("AVL", command.operation() + " " + command.value());
            if (command.operation() == AvlCommand.Operation.INSERT) {
                tree.insert(command.value());
            } else {
                tree.remove(command.value());
            }

            AVLTreeNode<Integer> focus = tree.find(command.value());
            if (focus == null) {
                focus = tree.raw();
            }
            focusId = focus == null ? null : focus.id;
            focusValue = focus == null ? null : focus.data;
            ExecutionEvents.emit(new AvlTreeEvent.CommandCompleted(
                    command, snapshot(tree.raw()), focusId, focusValue));
        }

        AvlNodeSnapshot finalSnapshot = snapshot(tree.raw());
        List<Integer> values = inOrder(tree.raw());
        ExecutionEvents.emit(new AvlTreeEvent.Completed(finalSnapshot, values, focusId, focusValue));
        Log.i("AVL", "Command batch completed, size=" + values.size());
        return new AvlTreeOutput(finalSnapshot, values);
    }

    private MutableAvlTree<Integer> createTree(AvlTreeInput input) {
        MutableAvlTree<Integer> tree = new MutableAvlTree<>();
        if (input.initialRoot() != null) {
            tree.restore(restore(input.initialRoot()));
            return tree;
        }
        for (Integer value : input.initialValues()) {
            tree.insert(value);
        }
        return tree;
    }

    private AVLTreeNode<Integer> restore(AvlNodeSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        AVLTreeNode<Integer> node = new AVLTreeNode<>(snapshot.id(), snapshot.value());
        node.height = snapshot.height();
        node.left = restore(snapshot.left());
        node.right = restore(snapshot.right());
        return node;
    }

    private AvlNodeSnapshot snapshot(AVLTreeNode<Integer> node) {
        if (node == null) {
            return null;
        }
        return new AvlNodeSnapshot(
                node.id,
                node.data,
                node.height,
                snapshot(left(node)),
                snapshot(right(node)));
    }

    private List<Integer> inOrder(AVLTreeNode<Integer> root) {
        List<Integer> values = new ArrayList<>();
        traverse(root, values);
        return List.copyOf(values);
    }

    private void traverse(AVLTreeNode<Integer> node, List<Integer> values) {
        if (node == null) {
            return;
        }
        traverse(left(node), values);
        values.add(node.data);
        traverse(right(node), values);
    }

    @SuppressWarnings("unchecked")
    private AVLTreeNode<Integer> left(AVLTreeNode<Integer> node) {
        return node == null ? null : (AVLTreeNode<Integer>) node.left;
    }

    @SuppressWarnings("unchecked")
    private AVLTreeNode<Integer> right(AVLTreeNode<Integer> node) {
        return node == null ? null : (AVLTreeNode<Integer>) node.right;
    }
}
