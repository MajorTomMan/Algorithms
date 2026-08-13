package com.majortom.algorithms.visualization.runtime.tree;

import com.majortom.algorithms.library.tree.AvlNodeSnapshot;
import com.majortom.algorithms.library.tree.AvlTreeEvent;

import java.util.List;
import java.util.Set;

/** Immutable tree state with stable-ID focus and balancing presentation metadata. */
public record AvlTreeViewState(
        AvlNodeSnapshot root,
        List<Integer> values,
        Long focusId,
        Integer focusedValue,
        Long parentId,
        Long childId,
        Set<Long> ancestorIds,
        Phase phase,
        AvlTreeEvent.Direction rotationDirection,
        Long rotationPivotId,
        Long rotationReplacementId,
        boolean completed) {

    public AvlTreeViewState {
        values = List.copyOf(values);
        ancestorIds = Set.copyOf(ancestorIds);
    }

    public static AvlTreeViewState empty() {
        return new AvlTreeViewState(null, List.of(), null, null, null, null, Set.of(),
                Phase.IDLE, null, null, null, false);
    }

    public enum Phase {
        IDLE,
        INITIALIZED,
        VISITING,
        COMPARING,
        CHILD_SELECTED,
        INSERTING,
        REMOVING,
        REPLACING,
        LINKING,
        BALANCE_CHECK,
        HEIGHT_UPDATE,
        ROTATING,
        COMMAND_COMPLETED,
        COMPLETED
    }
}
