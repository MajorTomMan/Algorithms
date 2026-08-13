package com.majortom.algorithms.library.tree;

import com.majortom.algorithms.core.api.AlgorithmEvent;
import com.majortom.algorithms.core.api.StatisticsContribution;

import java.util.Map;
import java.util.Objects;

/** Structure-neutral binary-tree steps shared by tree algorithm implementations. */
public sealed interface TreeStepEvent extends AlgorithmEvent, StatisticsContribution
        permits TreeStepEvent.NodeVisited, TreeStepEvent.NodeCompared,
        TreeStepEvent.ChildSelected, TreeStepEvent.NodeInserted,
        TreeStepEvent.NodeRemoved, TreeStepEvent.NodeValueReplaced,
        TreeStepEvent.LinkChanged {

    @Override
    default Map<String, Long> metricDeltas() {
        if (this instanceof NodeVisited) {
            return Map.of("nodes.visited", 1L);
        }
        if (this instanceof NodeCompared) {
            return Map.of("comparisons", 1L);
        }
        if (this instanceof ChildSelected) {
            return Map.of("children.selected", 1L);
        }
        if (this instanceof NodeInserted) {
            return Map.of("nodes.inserted", 1L);
        }
        if (this instanceof NodeRemoved) {
            return Map.of("nodes.removed", 1L);
        }
        if (this instanceof NodeValueReplaced) {
            return Map.of("values.replaced", 1L);
        }
        if (this instanceof LinkChanged) {
            return Map.of("links.changed", 1L);
        }
        return Map.of();
    }

    enum Relation {
        LEFT,
        RIGHT
    }

    enum Comparison {
        LESS,
        EQUAL,
        GREATER
    }

    record NodeVisited(AvlCommand command, long nodeId, int value, Long parentId)
            implements TreeStepEvent {
        public NodeVisited {
            Objects.requireNonNull(command, "command");
        }
    }

    record NodeCompared(
            AvlCommand command,
            int targetValue,
            long nodeId,
            int nodeValue,
            Comparison comparison) implements TreeStepEvent {
        public NodeCompared {
            Objects.requireNonNull(command, "command");
            Objects.requireNonNull(comparison, "comparison");
        }
    }

    record ChildSelected(
            AvlCommand command,
            long parentId,
            int parentValue,
            Long childId,
            Integer childValue,
            Relation relation) implements TreeStepEvent {
        public ChildSelected {
            Objects.requireNonNull(command, "command");
            Objects.requireNonNull(relation, "relation");
        }
    }

    record NodeInserted(
            AvlCommand command,
            long nodeId,
            int value,
            Long parentId,
            Relation relation) implements TreeStepEvent {
        public NodeInserted {
            Objects.requireNonNull(command, "command");
        }
    }

    record NodeRemoved(
            AvlCommand command,
            long nodeId,
            int value,
            Long parentId) implements TreeStepEvent {
        public NodeRemoved {
            Objects.requireNonNull(command, "command");
        }
    }

    record NodeValueReplaced(
            AvlCommand command,
            long nodeId,
            int previousValue,
            int newValue) implements TreeStepEvent {
        public NodeValueReplaced {
            Objects.requireNonNull(command, "command");
        }
    }

    record LinkChanged(
            AvlCommand command,
            long parentId,
            Long childId,
            Relation relation) implements TreeStepEvent {
        public LinkChanged {
            Objects.requireNonNull(command, "command");
            Objects.requireNonNull(relation, "relation");
        }
    }
}
