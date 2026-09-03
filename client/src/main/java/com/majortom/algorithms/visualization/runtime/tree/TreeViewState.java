package com.majortom.algorithms.visualization.runtime.tree;

import com.majortom.algorithms.core.snapshot.GeneralTreeSnapshot;
import com.majortom.algorithms.library.tree.AvlNodeSnapshot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Immutable JavaFX-neutral tree facts for general and binary tree families. */
public record TreeViewState(Kind kind, Long rootId, Map<Long, Node> nodes, boolean completed) {

    public TreeViewState {
        kind = Objects.requireNonNull(kind, "kind");
        nodes = Map.copyOf(Objects.requireNonNull(nodes, "nodes"));
    }

    public static TreeViewState empty(Kind kind) {
        return new TreeViewState(kind, null, Map.of(), false);
    }

    public static TreeViewState general(GeneralTreeSnapshot<Integer> snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (snapshot.root() == null) {
            return empty(Kind.GENERAL);
        }
        Map<Long, Node> nodes = new LinkedHashMap<>();
        collectGeneral(snapshot.root(), nodes);
        return new TreeViewState(Kind.GENERAL, snapshot.root().id(), nodes, false);
    }

    public static TreeViewState binary(AvlNodeSnapshot root) {
        if (root == null) {
            return empty(Kind.BINARY);
        }
        Map<Long, Node> nodes = new LinkedHashMap<>();
        collectBinary(root, nodes);
        return new TreeViewState(Kind.BINARY, root.id(), nodes, false);
    }

    public List<Integer> values() {
        List<Integer> result = new ArrayList<>();
        collectValues(rootId, new LinkedHashSet<>(), result);
        return List.copyOf(result);
    }

    public List<Long> childrenOf(Node node) {
        if (kind == Kind.GENERAL) {
            return node.childIds();
        }
        List<Long> children = new ArrayList<>(2);
        if (node.leftId() != null) {
            children.add(node.leftId());
        }
        if (node.rightId() != null) {
            children.add(node.rightId());
        }
        return List.copyOf(children);
    }

    private void collectValues(Long nodeId, Set<Long> visited, List<Integer> values) {
        if (nodeId == null || !visited.add(nodeId)) {
            return;
        }
        Node node = nodes.get(nodeId);
        if (node == null) {
            return;
        }
        if (kind == Kind.BINARY) {
            collectValues(node.leftId(), visited, values);
            values.add(node.value());
            collectValues(node.rightId(), visited, values);
            return;
        }
        values.add(node.value());
        for (Long childId : node.childIds()) {
            collectValues(childId, visited, values);
        }
    }

    private static void collectGeneral(GeneralTreeSnapshot.Node<Integer> node, Map<Long, Node> nodes) {
        if (node == null || nodes.containsKey(node.id())) {
            return;
        }
        List<Long> childIds = node.children().stream().map(GeneralTreeSnapshot.Node::id).toList();
        nodes.put(node.id(), Node.general(node.id(), node.value(), childIds));
        for (GeneralTreeSnapshot.Node<Integer> child : node.children()) {
            collectGeneral(child, nodes);
        }
    }

    private static void collectBinary(AvlNodeSnapshot node, Map<Long, Node> nodes) {
        if (node == null || nodes.containsKey(node.id())) {
            return;
        }
        Long leftId = node.left() == null ? null : node.left().id();
        Long rightId = node.right() == null ? null : node.right().id();
        nodes.put(node.id(), Node.binary(node.id(), node.value(), leftId, rightId));
        collectBinary(node.left(), nodes);
        collectBinary(node.right(), nodes);
    }

    public enum Kind {
        GENERAL,
        BINARY
    }

    public record Node(long id, int value, List<Long> childIds, Long leftId, Long rightId) {
        public Node {
            childIds = List.copyOf(Objects.requireNonNull(childIds, "childIds"));
        }

        public static Node general(long id, int value, List<Long> childIds) {
            return new Node(id, value, childIds, null, null);
        }

        public static Node binary(long id, int value, Long leftId, Long rightId) {
            return new Node(id, value, List.of(), leftId, rightId);
        }

        public Node withValue(int value) {
            return new Node(id, value, childIds, leftId, rightId);
        }

        public Node withChildren(List<Long> childIds) {
            return new Node(id, value, childIds, leftId, rightId);
        }

        public Node withLeft(Long leftId) {
            return new Node(id, value, childIds, leftId, rightId);
        }

        public Node withRight(Long rightId) {
            return new Node(id, value, childIds, leftId, rightId);
        }
    }
}
