package com.majortom.algorithms.visualization.runtime.tree;

import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.library.structure.event.TreeStructureEvent;
import com.majortom.algorithms.library.tree.AvlNodeSnapshot;
import com.majortom.algorithms.library.tree.AvlTreeEvent;
import com.majortom.algorithms.visualization.runtime.EventImportance;
import com.majortom.algorithms.visualization.runtime.EventReducer;
import com.majortom.algorithms.visualization.runtime.Reduction;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Reduces real AVL structure events into the client view state. */
public final class AvlTreeEventReducer implements EventReducer<AvlTreeViewState> {

    @Override
    public AvlTreeViewState initialState() {
        return AvlTreeViewState.empty();
    }

    @Override
    public Reduction<AvlTreeViewState> reduce(AvlTreeViewState previous, EventEnvelope event) {
        Object payload = event.event();
        if (payload instanceof AvlTreeEvent.Initialized initialized) {
            return changed(snapshot(initialized.root(), null, null, null, Set.of(),
                    AvlTreeViewState.Phase.INITIALIZED, null, null, null, false),
                    EventImportance.CHECKPOINT);
        }

        if (payload instanceof TreeStructureEvent treeEvent) {
            if (previous.phase() == AvlTreeViewState.Phase.IDLE) {
                return Reduction.unchanged(previous, EventImportance.TRANSIENT);
            }
            return reduceStructureEvent(previous, treeEvent);
        }

        if (payload instanceof AvlTreeEvent.CommandCompleted completed) {
            return changed(snapshot(completed.root(), completed.focusId(), completed.focusValue(),
                    parentId(completed.root(), completed.focusId()),
                    ancestors(completed.root(), completed.focusId()),
                    AvlTreeViewState.Phase.COMMAND_COMPLETED, null, null, null, false),
                    EventImportance.CHECKPOINT);
        }

        if (payload instanceof AvlTreeEvent.Completed completed) {
            return changed(snapshot(completed.root(), completed.focusId(), completed.focusValue(),
                    parentId(completed.root(), completed.focusId()),
                    ancestors(completed.root(), completed.focusId()),
                    AvlTreeViewState.Phase.COMPLETED, null, null, null, true),
                    EventImportance.TERMINAL);
        }

        return Reduction.unchanged(previous, EventImportance.TRANSIENT);
    }

    private Reduction<AvlTreeViewState> reduceStructureEvent(
            AvlTreeViewState previous, TreeStructureEvent event) {
        if (event instanceof TreeStructureEvent.Inserted inserted) {
            return changed(copy(previous, inserted.nodeId(), integerValue(inserted.value()),
                    null, null, Set.of(), AvlTreeViewState.Phase.INSERTING,
                    null, null, null, false), EventImportance.STATE_CHANGE);
        }

        if (event instanceof TreeStructureEvent.Removed removed) {
            return changed(copy(previous, removed.nodeId(), integerValue(removed.value()),
                    parentId(previous.root(), removed.nodeId()), null,
                    ancestors(previous.root(), removed.nodeId()), AvlTreeViewState.Phase.REMOVING,
                    null, null, null, false), EventImportance.STATE_CHANGE);
        }

        if (event instanceof TreeStructureEvent.RotatedLeft rotated) {
            return rotation(previous, rotated.rootId(), rotated.replacementId(),
                    AvlTreeEvent.Direction.LEFT);
        }

        if (event instanceof TreeStructureEvent.RotatedRight rotated) {
            return rotation(previous, rotated.rootId(), rotated.replacementId(),
                    AvlTreeEvent.Direction.RIGHT);
        }

        return Reduction.unchanged(previous, EventImportance.TRANSIENT);
    }

    private Reduction<AvlTreeViewState> rotation(
            AvlTreeViewState previous,
            long rootId,
            long replacementId,
            AvlTreeEvent.Direction direction) {
        return changed(copy(previous, rootId, valueOf(previous.root(), rootId),
                parentId(previous.root(), rootId), replacementId,
                ancestors(previous.root(), rootId), AvlTreeViewState.Phase.ROTATING,
                direction, rootId, replacementId, false), EventImportance.CHECKPOINT);
    }

    private static AvlTreeViewState snapshot(
            AvlNodeSnapshot root,
            Long focusId,
            Integer focusValue,
            Long parentId,
            Set<Long> ancestorIds,
            AvlTreeViewState.Phase phase,
            AvlTreeEvent.Direction rotationDirection,
            Long rotationPivotId,
            Long rotationReplacementId,
            boolean completed) {
        return new AvlTreeViewState(root, values(root), focusId, focusValue, parentId, null,
                ancestorIds, phase, rotationDirection, rotationPivotId, rotationReplacementId, completed);
    }

    private static AvlTreeViewState copy(
            AvlTreeViewState previous,
            Long focusId,
            Integer focusValue,
            Long parentId,
            Long childId,
            Set<Long> ancestorIds,
            AvlTreeViewState.Phase phase,
            AvlTreeEvent.Direction rotationDirection,
            Long rotationPivotId,
            Long rotationReplacementId,
            boolean completed) {
        return new AvlTreeViewState(previous.root(), previous.values(), focusId, focusValue,
                parentId, childId, ancestorIds, phase, rotationDirection, rotationPivotId,
                rotationReplacementId, completed);
    }

    private static Integer integerValue(Object value) {
        if (value instanceof Integer integer) {
            return integer;
        }
        return null;
    }

    private static Set<Long> ancestors(AvlNodeSnapshot root, Long targetId) {
        if (root == null || targetId == null) {
            return Set.of();
        }
        LinkedHashSet<Long> path = new LinkedHashSet<>();
        if (!findPath(root, targetId, path)) {
            return Set.of();
        }
        path.remove(targetId);
        return path;
    }

    private static boolean findPath(AvlNodeSnapshot node, long targetId, Set<Long> path) {
        if (node == null) {
            return false;
        }
        path.add(node.id());
        if (node.id() == targetId || findPath(node.left(), targetId, path)
                || findPath(node.right(), targetId, path)) {
            return true;
        }
        path.remove(node.id());
        return false;
    }

    private static Long parentId(AvlNodeSnapshot root, Long targetId) {
        if (root == null || targetId == null || root.id() == targetId) {
            return null;
        }
        return findParent(root, targetId);
    }

    private static Long findParent(AvlNodeSnapshot node, long targetId) {
        if (node == null) {
            return null;
        }
        if ((node.left() != null && node.left().id() == targetId)
                || (node.right() != null && node.right().id() == targetId)) {
            return node.id();
        }
        Long left = findParent(node.left(), targetId);
        if (left != null) {
            return left;
        }
        return findParent(node.right(), targetId);
    }

    private static Integer valueOf(AvlNodeSnapshot node, Long targetId) {
        if (node == null || targetId == null) {
            return null;
        }
        if (node.id() == targetId) {
            return node.value();
        }
        Integer left = valueOf(node.left(), targetId);
        if (left != null) {
            return left;
        }
        return valueOf(node.right(), targetId);
    }

    private static List<Integer> values(AvlNodeSnapshot root) {
        List<Integer> result = new ArrayList<>();
        traverse(root, result);
        return result;
    }

    private static void traverse(AvlNodeSnapshot node, List<Integer> values) {
        if (node == null) {
            return;
        }
        traverse(node.left(), values);
        values.add(node.value());
        traverse(node.right(), values);
    }

    private static Reduction<AvlTreeViewState> changed(
            AvlTreeViewState state, EventImportance importance) {
        return Reduction.changed(state, importance, true);
    }
}
