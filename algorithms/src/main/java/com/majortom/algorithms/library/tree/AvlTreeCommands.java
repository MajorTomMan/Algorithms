package com.majortom.algorithms.library.tree;

import com.majortom.algorithms.core.runtime.ExecutionEvents;

import java.util.ArrayList;
import java.util.List;

/** AVL insertion/removal command processor with replayable semantic steps. */
public final class AvlTreeCommands {

    private long nextNodeId;
    private Node currentRoot;

    public AvlTreeOutput execute(AvlTreeInput input) {
        nextNodeId = 1;
        Node root;
        if (input.initialRoot() != null) {
            root = restore(input.initialRoot(), null);
            nextNodeId = maxNodeId(root) + 1;
        } else {
            root = null;
            for (int value : input.initialValues()) {
                root = insertInitial(root, null, value);
            }
        }
        currentRoot = root;
        ExecutionEvents.emit(new AvlTreeEvent.Initialized(snapshot(root)));
        Long focusId = null;
        Integer focusValue = null;
        for (AvlCommand command : input.commands()) {
            ExecutionEvents.checkpoint();
            if (command.operation() == AvlCommand.Operation.INSERT) {
                root = insert(root, command.value(), null, null, command);
            } else {
                root = remove(root, command.value(), null, command);
            }
            currentRoot = root;
            Node focus = findNearest(root, command.value());
            focusId = null;
            focusValue = null;
            if (focus != null) {
                focusId = focus.id;
                focusValue = focus.value;
            }
            ExecutionEvents.emit(new AvlTreeEvent.CommandCompleted(
                    command, snapshot(root), focusId, focusValue));
        }
        List<Integer> values = new ArrayList<>();
        traverse(root, values);
        AvlNodeSnapshot finalSnapshot = snapshot(root);
        ExecutionEvents.emit(new AvlTreeEvent.Completed(finalSnapshot, values, focusId, focusValue));
        return new AvlTreeOutput(finalSnapshot, values);
    }

    private Node restore(AvlNodeSnapshot snapshot, Node parent) {
        if (snapshot == null) return null;
        Node node = new Node(snapshot.id(), snapshot.value(), parent);
        node.height = snapshot.height();
        node.left = restore(snapshot.left(), node);
        node.right = restore(snapshot.right(), node);
        return node;
    }

    private long maxNodeId(Node node) {
        if (node == null) return 0L;
        return Math.max(node.id, Math.max(maxNodeId(node.left), maxNodeId(node.right)));
    }

    private Node insertInitial(Node node, Node parent, int value) {
        if (node == null) {
            return new Node(nextNodeId++, value, parent);
        }
        if (value < node.value) {
            node.left = insertInitial(node.left, node, value);
        } else if (value > node.value) {
            node.right = insertInitial(node.right, node, value);
        } else {
            return node;
        }
        Node balanced = rebalanceInitial(node);
        balanced.parent = parent;
        return balanced;
    }

    private Node insert(
            Node node,
            int value,
            Node parent,
            TreeStepEvent.Relation relation,
            AvlCommand command) {
        ExecutionEvents.checkpoint();
        if (node == null) {
            Node inserted = new Node(nextNodeId++, value, parent);
            Long parentId = null;
            if (parent != null) {
                parentId = parent.id;
                setChild(parent, relation, inserted);
            } else {
                currentRoot = inserted;
            }
            ExecutionEvents.emit(new TreeStepEvent.NodeInserted(
                    command, inserted.id, inserted.value, parentId, relation));
            if (parent != null) {
                ExecutionEvents.emit(new TreeStepEvent.LinkChanged(
                        command, parent.id, inserted.id, relation));
            }
            emitStructureChanged(
                    command, inserted.id, AvlTreeEvent.StructurePhase.INSERTED);
            return inserted;
        }
        emitVisitAndComparison(node, parent, value, command);
        if (value < node.value) {
            ExecutionEvents.emit(new TreeStepEvent.ChildSelected(
                    command, node.id, node.value, id(node.left), value(node.left),
                    TreeStepEvent.Relation.LEFT));
            node.left = insert(
                    node.left, value, node, TreeStepEvent.Relation.LEFT, command);
        } else if (value > node.value) {
            ExecutionEvents.emit(new TreeStepEvent.ChildSelected(
                    command, node.id, node.value, id(node.right), value(node.right),
                    TreeStepEvent.Relation.RIGHT));
            node.right = insert(
                    node.right, value, node, TreeStepEvent.Relation.RIGHT, command);
        } else {
            return node;
        }
        return rebalance(node, command);
    }

    private Node remove(
            Node node,
            int target,
            Node parent,
            AvlCommand command) {
        ExecutionEvents.checkpoint();
        if (node == null) {
            return null;
        }
        emitVisitAndComparison(node, parent, target, command);
        if (target < node.value) {
            ExecutionEvents.emit(new TreeStepEvent.ChildSelected(
                    command, node.id, node.value, id(node.left), value(node.left),
                    TreeStepEvent.Relation.LEFT));
            node.left = remove(node.left, target, node, command);
        } else if (target > node.value) {
            ExecutionEvents.emit(new TreeStepEvent.ChildSelected(
                    command, node.id, node.value, id(node.right), value(node.right),
                    TreeStepEvent.Relation.RIGHT));
            node.right = remove(node.right, target, node, command);
        } else {
            if (node.left == null) {
                Node replacement = node.right;
                TreeStepEvent.Relation removedRelation = relationOf(parent, node);
                replaceNode(node, replacement);
                ExecutionEvents.emit(new TreeStepEvent.NodeRemoved(
                        command, node.id, node.value, id(parent)));
                emitReplacementLink(command, parent, replacement, removedRelation);
                emitStructureChanged(
                        command, focusAfterRemoval(replacement, parent),
                        AvlTreeEvent.StructurePhase.REMOVED);
                return replacement;
            }
            if (node.right == null) {
                Node replacement = node.left;
                TreeStepEvent.Relation removedRelation = relationOf(parent, node);
                replaceNode(node, replacement);
                ExecutionEvents.emit(new TreeStepEvent.NodeRemoved(
                        command, node.id, node.value, id(parent)));
                emitReplacementLink(command, parent, replacement, removedRelation);
                emitStructureChanged(
                        command, focusAfterRemoval(replacement, parent),
                        AvlTreeEvent.StructurePhase.REMOVED);
                return replacement;
            }
            Node successor = minimum(node.right, node, command);
            int previousValue = node.value;
            node.value = successor.value;
            ExecutionEvents.emit(new TreeStepEvent.NodeValueReplaced(
                    command, node.id, previousValue, successor.value));
            emitStructureChanged(
                    command, node.id, AvlTreeEvent.StructurePhase.VALUE_REPLACED);
            node.right = remove(node.right, successor.value, node, command);
        }
        return rebalance(node, command);
    }

    private void emitVisitAndComparison(
            Node node,
            Node parent,
            int target,
            AvlCommand command) {
        ExecutionEvents.emit(new TreeStepEvent.NodeVisited(command, node.id, node.value, id(parent)));
        TreeStepEvent.Comparison comparison = TreeStepEvent.Comparison.EQUAL;
        if (target < node.value) {
            comparison = TreeStepEvent.Comparison.LESS;
        } else if (target > node.value) {
            comparison = TreeStepEvent.Comparison.GREATER;
        }
        ExecutionEvents.emit(new TreeStepEvent.NodeCompared(
                command, target, node.id, node.value, comparison));
    }

    private Node rebalance(Node node, AvlCommand command) {
        updateHeight(node, command);
        int balance = height(node.left) - height(node.right);
        ExecutionEvents.emit(new AvlTreeEvent.BalanceChecked(command, node.id, node.value, balance));
        if (balance > 1) {
            if (height(node.left.left) < height(node.left.right)) {
                node.left = rotateLeft(node.left, command);
            }
            return rotateRight(node, command);
        }
        if (balance < -1) {
            if (height(node.right.right) < height(node.right.left)) {
                node.right = rotateRight(node.right, command);
            }
            return rotateLeft(node, command);
        }
        return node;
    }

    private Node rebalanceInitial(Node node) {
        updateHeight(node);
        int balance = height(node.left) - height(node.right);
        if (balance > 1) {
            if (height(node.left.left) < height(node.left.right)) {
                node.left = rotateLeftInitial(node.left);
            }
            return rotateRightInitial(node);
        }
        if (balance < -1) {
            if (height(node.right.right) < height(node.right.left)) {
                node.right = rotateRightInitial(node.right);
            }
            return rotateLeftInitial(node);
        }
        return node;
    }

    private Node rotateLeft(Node root, AvlCommand command) {
        Node replacement = root.right;
        Node previousParent = root.parent;
        Node transfer = replacement.left;
        attachReplacement(previousParent, root, replacement);
        root.right = transfer;
        if (transfer != null) {
            transfer.parent = root;
        }
        replacement.left = root;
        root.parent = replacement;
        updateHeight(root);
        updateHeight(replacement);
        ExecutionEvents.emit(new AvlTreeEvent.Rotated(
                command, AvlTreeEvent.Direction.LEFT,
                root.id, root.value, replacement.id, replacement.value));
        emitStructureChanged(
                command, replacement.id, AvlTreeEvent.StructurePhase.ROTATED);
        emitHeightUpdated(root, command);
        emitHeightUpdated(replacement, command);
        return replacement;
    }

    private Node rotateRight(Node root, AvlCommand command) {
        Node replacement = root.left;
        Node previousParent = root.parent;
        Node transfer = replacement.right;
        attachReplacement(previousParent, root, replacement);
        root.left = transfer;
        if (transfer != null) {
            transfer.parent = root;
        }
        replacement.right = root;
        root.parent = replacement;
        updateHeight(root);
        updateHeight(replacement);
        ExecutionEvents.emit(new AvlTreeEvent.Rotated(
                command, AvlTreeEvent.Direction.RIGHT,
                root.id, root.value, replacement.id, replacement.value));
        emitStructureChanged(
                command, replacement.id, AvlTreeEvent.StructurePhase.ROTATED);
        emitHeightUpdated(root, command);
        emitHeightUpdated(replacement, command);
        return replacement;
    }

    private Node rotateLeftInitial(Node root) {
        Node replacement = root.right;
        Node previousParent = root.parent;
        Node transfer = replacement.left;
        root.right = transfer;
        if (transfer != null) {
            transfer.parent = root;
        }
        replacement.left = root;
        replacement.parent = previousParent;
        root.parent = replacement;
        updateHeight(root);
        updateHeight(replacement);
        return replacement;
    }

    private Node rotateRightInitial(Node root) {
        Node replacement = root.left;
        Node previousParent = root.parent;
        Node transfer = replacement.right;
        root.left = transfer;
        if (transfer != null) {
            transfer.parent = root;
        }
        replacement.right = root;
        replacement.parent = previousParent;
        root.parent = replacement;
        updateHeight(root);
        updateHeight(replacement);
        return replacement;
    }

    private Node minimum(
            Node node,
            Node parent,
            AvlCommand command) {
        Node current = node;
        Node currentParent = parent;
        while (true) {
            ExecutionEvents.checkpoint();
            ExecutionEvents.emit(new TreeStepEvent.NodeVisited(
                    command, current.id, current.value, id(currentParent)));
            if (current.left == null) {
                return current;
            }
            ExecutionEvents.emit(new TreeStepEvent.ChildSelected(
                    command, current.id, current.value, current.left.id, current.left.value,
                    TreeStepEvent.Relation.LEFT));
            currentParent = current;
            current = current.left;
        }
    }

    private Node findNearest(Node node, int target) {
        Node current = node;
        Node nearest = null;
        while (current != null) {
            nearest = current;
            if (target == current.value) {
                return current;
            }
            if (target < current.value) {
                current = current.left;
            } else {
                current = current.right;
            }
        }
        return nearest;
    }

    private void traverse(Node node, List<Integer> values) {
        if (node == null) {
            return;
        }
        traverse(node.left, values);
        values.add(node.value);
        traverse(node.right, values);
    }

    private AvlNodeSnapshot snapshot(Node node) {
        if (node == null) {
            return null;
        }
        AvlNodeSnapshot left = snapshot(node.left);
        AvlNodeSnapshot right = snapshot(node.right);
        int derivedHeight = Math.max(snapshotHeight(left), snapshotHeight(right)) + 1;
        return new AvlNodeSnapshot(
                node.id, node.value, derivedHeight, left, right);
    }

    private int snapshotHeight(AvlNodeSnapshot node) {
        if (node == null) {
            return 0;
        }
        return node.height();
    }

    private void emitStructureChanged(
            AvlCommand command,
            Long focusId,
            AvlTreeEvent.StructurePhase phase) {
        ExecutionEvents.emit(new AvlTreeEvent.StructureChanged(
                command, snapshot(currentRoot), focusId, phase));
    }

    private void setChild(Node parent, TreeStepEvent.Relation relation, Node child) {
        if (relation == TreeStepEvent.Relation.LEFT) {
            parent.left = child;
        } else {
            parent.right = child;
        }
        if (child != null) {
            child.parent = parent;
        }
    }

    private void replaceNode(Node node, Node replacement) {
        Node parent = node.parent;
        if (parent == null) {
            currentRoot = replacement;
            if (replacement != null) {
                replacement.parent = null;
            }
            return;
        }
        if (parent.left == node) {
            setChild(parent, TreeStepEvent.Relation.LEFT, replacement);
        } else {
            setChild(parent, TreeStepEvent.Relation.RIGHT, replacement);
        }
    }

    private void emitReplacementLink(
            AvlCommand command,
            Node parent,
            Node replacement,
            TreeStepEvent.Relation relation) {
        if (parent == null) {
            return;
        }
        ExecutionEvents.emit(new TreeStepEvent.LinkChanged(
                command, parent.id, id(replacement), relation));
    }

    private TreeStepEvent.Relation relationOf(Node parent, Node child) {
        if (parent == null) {
            return null;
        }
        if (parent.left == child) {
            return TreeStepEvent.Relation.LEFT;
        }
        return TreeStepEvent.Relation.RIGHT;
    }

    private void attachReplacement(Node parent, Node previous, Node replacement) {
        replacement.parent = parent;
        if (parent == null) {
            currentRoot = replacement;
            return;
        }
        if (parent.left == previous) {
            parent.left = replacement;
        } else {
            parent.right = replacement;
        }
    }

    private Long focusAfterRemoval(Node replacement, Node parent) {
        if (replacement != null) {
            return replacement.id;
        }
        return id(parent);
    }

    private void updateHeight(Node node, AvlCommand command) {
        updateHeight(node);
        emitHeightUpdated(node, command);
    }

    private void emitHeightUpdated(Node node, AvlCommand command) {
        ExecutionEvents.emit(new AvlTreeEvent.HeightUpdated(
                command, node.id, node.value, node.height));
    }

    private void updateHeight(Node node) {
        node.height = Math.max(height(node.left), height(node.right)) + 1;
    }

    private int height(Node node) {
        if (node == null) {
            return 0;
        }
        return node.height;
    }

    private Long id(Node node) {
        if (node == null) {
            return null;
        }
        return node.id;
    }

    private Integer value(Node node) {
        if (node == null) {
            return null;
        }
        return node.value;
    }

    private static final class Node {
        private final long id;
        private int value;
        private int height = 1;
        private Node left;
        private Node right;
        private Node parent;

        private Node(long id, int value, Node parent) {
            this.id = id;
            this.value = value;
            this.parent = parent;
        }
    }
}
