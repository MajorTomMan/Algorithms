package com.majortom.algorithms.visualization.runtime.linked;

import com.majortom.algorithms.structure.linked.ListNode;

import java.util.LinkedHashMap;
import java.util.List;
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

    /** Builds a read-only preview state from a value-only sequence snapshot. */
    public static LinkedListViewState fromValues(List<Integer> values) {
        List<Integer> source = List.copyOf(Objects.requireNonNull(values, "values"));
        Map<Long, Node> nodes = new LinkedHashMap<>();
        for (int index = 0; index < source.size(); index++) {
            long id = index + 1L;
            Long previousId = null;
            if (index > 0) {
                previousId = id - 1L;
            }
            Long nextId = null;
            if (index + 1 < source.size()) {
                nextId = id + 1L;
            }
            nodes.put(id, new Node(id, source.get(index), nextId, previousId));
        }
        return new LinkedListViewState(nodes);
    }

    public static LinkedListViewState source(ListNode<Integer> head) {
        Map<Long, Node> nodes = new LinkedHashMap<>();
        ListNode<Integer> current = head;
        while (current != null && !nodes.containsKey(current.getId())) {
            Long nextId;
            if (current.getNext() == null) {
                nextId = null;
            } else {
                nextId = current.getNext().getId();
            }
            Long previousId;
            if (current.getPrevious() == null) {
                previousId = null;
            } else {
                previousId = current.getPrevious().getId();
            }
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
