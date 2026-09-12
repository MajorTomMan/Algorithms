package com.majortom.algorithms.visualization.runtime.linked;

import com.majortom.algorithms.structure.linked.ListNode;
import com.majortom.algorithms.visualization.runtime.VisualValue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable JavaFX-neutral linked-list facts keyed by stable node id. */
public record LinkedListViewState(Map<Long, Node> nodes) {
    public LinkedListViewState { nodes = Map.copyOf(Objects.requireNonNull(nodes, "nodes")); }
    public static LinkedListViewState empty() { return new LinkedListViewState(Map.of()); }

    public static LinkedListViewState fromValues(List<?> values) {
        List<?> source = List.copyOf(Objects.requireNonNull(values, "values"));
        Map<Long, Node> nodes = new LinkedHashMap<>();
        for (int index = 0; index < source.size(); index++) {
            long id = index + 1L;
            Long previousId = index > 0 ? id - 1L : null;
            Long nextId = index + 1 < source.size() ? id + 1L : null;
            nodes.put(id, new Node(id, VisualValue.of(source.get(index)), nextId, previousId));
        }
        return new LinkedListViewState(nodes);
    }

    public static LinkedListViewState source(ListNode<?> head) {
        Map<Long, Node> nodes = new LinkedHashMap<>();
        ListNode<?> current = head;
        while (current != null && !nodes.containsKey(current.getId())) {
            Long nextId = current.getNext() == null ? null : current.getNext().getId();
            Long previousId = current.getPrevious() == null ? null : current.getPrevious().getId();
            nodes.put(current.getId(), new Node(current.getId(), VisualValue.of(current.getValue()), nextId, previousId));
            current = current.getNext();
        }
        return new LinkedListViewState(nodes);
    }

    public record Node(long id, VisualValue value, Long nextId, Long previousId) {
        public Node {
            if (id <= 0) throw new IllegalArgumentException("node id must be positive");
            value = Objects.requireNonNull(value, "value");
        }
        public Node withValue(Object value) { return new Node(id, VisualValue.of(value), nextId, previousId); }
        public Node withNext(Long nextId) { return new Node(id, value, nextId, previousId); }
        public Node withPrevious(Long previousId) { return new Node(id, value, nextId, previousId); }
    }
}
