package com.majortom.algorithms.visualization.runtime.linked;

import com.majortom.algorithms.library.basic.node.ListNode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable JavaFX-neutral linked-list facts keyed by stable node id. */
public record LinkedListViewState(Map<Long, Node> nodes) {
    public LinkedListViewState {
        nodes = Map.copyOf(Objects.requireNonNull(nodes, "nodes"));
    }

    public static LinkedListViewState empty() {
        return new LinkedListViewState(Map.of());
    }

    public static LinkedListViewState source(ListNode<Integer> head) {
        Map<Long, Node> nodes = new LinkedHashMap<>();
        ListNode<Integer> current = head;
        while (current != null && !nodes.containsKey(current.getId())) {
            Long nextId = current.getNext() == null ? null : current.getNext().getId();
            Long previousId = current.getPrevious() == null ? null : current.getPrevious().getId();
            nodes.put(current.getId(), new Node(current.getId(), current.getValue(), nextId, previousId));
            current = current.getNext();
        }
        return new LinkedListViewState(nodes);
    }

    public record Node(long id, Integer value, Long nextId, Long previousId) {
        public Node {
            if (id <= 0) {
                throw new IllegalArgumentException("node id must be positive");
            }
        }

        public Node withValue(Integer value) {
            return new Node(id, value, nextId, previousId);
        }

        public Node withNext(Long nextId) {
            return new Node(id, value, nextId, previousId);
        }

        public Node withPrevious(Long previousId) {
            return new Node(id, value, nextId, previousId);
        }
    }
}
