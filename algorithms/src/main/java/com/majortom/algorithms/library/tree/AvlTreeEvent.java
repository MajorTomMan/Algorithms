package com.majortom.algorithms.library.tree;

import com.majortom.algorithms.core.api.AlgorithmEvent;
import com.majortom.algorithms.core.api.StatisticsContribution;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** AVL-specific balancing events layered on top of common tree steps. */
public sealed interface AvlTreeEvent extends AlgorithmEvent, StatisticsContribution
        permits AvlTreeEvent.Initialized, AvlTreeEvent.BalanceChecked,
        AvlTreeEvent.HeightUpdated, AvlTreeEvent.Rotated,
        AvlTreeEvent.StructureChanged, AvlTreeEvent.CommandCompleted,
        AvlTreeEvent.Completed {

    @Override
    default Map<String, Long> metricDeltas() {
        if (this instanceof BalanceChecked) {
            return Map.of("balances.checked", 1L);
        }
        if (this instanceof HeightUpdated) {
            return Map.of("heights.updated", 1L);
        }
        if (this instanceof Rotated) {
            return Map.of("rotations", 1L);
        }
        if (this instanceof CommandCompleted) {
            return Map.of("commands.completed", 1L);
        }
        return Map.of();
    }

    record Initialized(AvlNodeSnapshot root) implements AvlTreeEvent {
    }

    record BalanceChecked(AvlCommand command, long nodeId, int value, int balance)
            implements AvlTreeEvent {
        public BalanceChecked {
            Objects.requireNonNull(command, "command");
        }
    }

    record HeightUpdated(AvlCommand command, long nodeId, int value, int height)
            implements AvlTreeEvent {
        public HeightUpdated {
            Objects.requireNonNull(command, "command");
            if (height < 1) {
                throw new IllegalArgumentException("node height must be positive");
            }
        }
    }

    record Rotated(
            AvlCommand command,
            Direction direction,
            long pivotId,
            int pivotValue,
            long replacementId,
            int replacementValue) implements AvlTreeEvent {
        public Rotated {
            Objects.requireNonNull(command, "command");
            Objects.requireNonNull(direction, "direction");
        }
    }

    /** Full immutable topology immediately after one structural mutation. */
    record StructureChanged(
            AvlCommand command,
            AvlNodeSnapshot root,
            Long focusId,
            StructurePhase phase) implements AvlTreeEvent {
        public StructureChanged {
            Objects.requireNonNull(command, "command");
            Objects.requireNonNull(phase, "phase");
        }
    }

    record CommandCompleted(
            AvlCommand command,
            AvlNodeSnapshot root,
            Long focusId,
            Integer focusValue) implements AvlTreeEvent {
        public CommandCompleted {
            Objects.requireNonNull(command, "command");
        }
    }

    record Completed(
            AvlNodeSnapshot root,
            List<Integer> values,
            Long focusId,
            Integer focusValue) implements AvlTreeEvent {
        public Completed {
            Objects.requireNonNull(values, "values");
            values = List.copyOf(values);
        }
    }

    enum Direction {
        LEFT,
        RIGHT
    }

    enum StructurePhase {
        INSERTED,
        REMOVED,
        VALUE_REPLACED,
        LINK_CHANGED,
        ROTATED
    }
}
