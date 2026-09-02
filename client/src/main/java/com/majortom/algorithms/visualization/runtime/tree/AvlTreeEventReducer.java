package com.majortom.algorithms.visualization.runtime.tree;

import com.majortom.algorithms.visualization.runtime.EventImportance;
import com.majortom.algorithms.visualization.runtime.EventReducer;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.visualization.runtime.Reduction;
import com.majortom.algorithms.library.tree.AvlNodeSnapshot;
import com.majortom.algorithms.library.tree.AvlTreeEvent;
import com.majortom.algorithms.library.tree.TreeStepEvent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Stateless reducer for generic binary-tree steps plus AVL balancing events. */
public final class AvlTreeEventReducer implements EventReducer<AvlTreeViewState> {

    @Override
    public AvlTreeViewState initialState() {
        return AvlTreeViewState.empty();
    }

    @Override
    public Reduction<AvlTreeViewState> reduce(AvlTreeViewState previous, EventEnvelope event) {
        Object payload = event.event();
        if (payload instanceof AvlTreeEvent.Initialized initialized) {
            return changed(snapshot(previous, initialized.root(), null, null, null, Set.of(),
                    AvlTreeViewState.Phase.INITIALIZED, null, null, null, false), EventImportance.CHECKPOINT);
        }
        if (payload instanceof TreeStepEvent.NodeVisited visited) {
            Set<Long> ancestors = nextAncestors(previous, visited.nodeId(), visited.parentId());
            return changed(copy(previous, visited.nodeId(), visited.value(), visited.parentId(), null,
                    ancestors, AvlTreeViewState.Phase.VISITING, null, null, null, false),
                    EventImportance.TRANSIENT);
        }
        if (payload instanceof TreeStepEvent.NodeCompared compared) {
            Long parentId = parentId(previous.root(), compared.nodeId());
            return changed(copy(previous, compared.nodeId(), compared.nodeValue(), parentId, null,
                    ancestors(previous.root(), compared.nodeId()), AvlTreeViewState.Phase.COMPARING,
                    null, null, null, false), EventImportance.TRANSIENT);
        }
        if (payload instanceof TreeStepEvent.ChildSelected selected) {
            Set<Long> ancestors = ancestors(previous.root(), selected.parentId());
            return changed(copy(previous, selected.parentId(), selected.parentValue(), null,
                    selected.childId(), ancestors, AvlTreeViewState.Phase.CHILD_SELECTED,
                    null, null, null, false), EventImportance.TRANSIENT);
        }
        if (payload instanceof TreeStepEvent.NodeInserted inserted) {
            Set<Long> ancestors = ancestors(previous.root(), inserted.parentId());
            return changed(copy(previous, inserted.nodeId(), inserted.value(), inserted.parentId(), null,
                    ancestors, AvlTreeViewState.Phase.INSERTING, null, null, null, false),
                    EventImportance.STATE_CHANGE);
        }
        if (payload instanceof TreeStepEvent.NodeRemoved removed) {
            return changed(copy(previous, removed.nodeId(), removed.value(), removed.parentId(), null,
                    ancestors(previous.root(), removed.parentId()), AvlTreeViewState.Phase.REMOVING,
                    null, null, null, false), EventImportance.STATE_CHANGE);
        }
        if (payload instanceof TreeStepEvent.NodeValueReplaced replaced) {
            Long parentId = parentId(previous.root(), replaced.nodeId());
            return changed(copy(previous, replaced.nodeId(), replaced.newValue(), parentId, null,
                    ancestors(previous.root(), replaced.nodeId()), AvlTreeViewState.Phase.REPLACING,
                    null, null, null, false), EventImportance.STATE_CHANGE);
        }
        if (payload instanceof TreeStepEvent.LinkChanged link) {
            return changed(copy(previous, link.parentId(), valueOf(previous.root(), link.parentId()), null,
                    link.childId(), ancestors(previous.root(), link.parentId()),
                    AvlTreeViewState.Phase.LINKING, null, null, null, false),
                    EventImportance.STATE_CHANGE);
        }
        if (payload instanceof AvlTreeEvent.BalanceChecked checked) {
            return changed(copy(previous, checked.nodeId(), checked.value(),
                    parentId(previous.root(), checked.nodeId()), null,
                    ancestors(previous.root(), checked.nodeId()), AvlTreeViewState.Phase.BALANCE_CHECK,
                    null, null, null, false), EventImportance.TRANSIENT);
        }
        if (payload instanceof AvlTreeEvent.HeightUpdated updated) {
            return changed(copy(previous, updated.nodeId(), updated.value(),
                    parentId(previous.root(), updated.nodeId()), null,
                    ancestors(previous.root(), updated.nodeId()), AvlTreeViewState.Phase.HEIGHT_UPDATE,
                    null, null, null, false), EventImportance.TRANSIENT);
        }
        if (payload instanceof AvlTreeEvent.Rotated rotated) {
            return changed(copy(previous, rotated.pivotId(), rotated.pivotValue(),
                    parentId(previous.root(), rotated.pivotId()), rotated.replacementId(),
                    ancestors(previous.root(), rotated.pivotId()), AvlTreeViewState.Phase.ROTATING,
                    rotated.direction(), rotated.pivotId(), rotated.replacementId(), false),
                    EventImportance.CHECKPOINT);
        }
        if (payload instanceof AvlTreeEvent.StructureChanged changed) {
            AvlTreeViewState.Phase phase = structurePhase(changed.phase());
            AvlTreeEvent.Direction direction = null;
            Long pivotId = null;
            Long replacementId = null;
            if (changed.phase() == AvlTreeEvent.StructurePhase.ROTATED) {
                direction = previous.rotationDirection();
                pivotId = previous.rotationPivotId();
                replacementId = previous.rotationReplacementId();
            }
            Integer focusValue = valueOf(changed.root(), changed.focusId());
            return changed(snapshot(previous, changed.root(), changed.focusId(), focusValue,
                    parentId(changed.root(), changed.focusId()),
                    ancestors(changed.root(), changed.focusId()), phase, direction, pivotId,
                    replacementId, false), EventImportance.CHECKPOINT);
        }
        if (payload instanceof AvlTreeEvent.CommandCompleted completed) {
            return changed(snapshot(previous, completed.root(), completed.focusId(), completed.focusValue(),
                    parentId(completed.root(), completed.focusId()),
                    ancestors(completed.root(), completed.focusId()),
                    AvlTreeViewState.Phase.COMMAND_COMPLETED, null, null, null, false),
                    EventImportance.CHECKPOINT);
        }
        if (payload instanceof AvlTreeEvent.Completed completed) {
            return changed(snapshot(previous, completed.root(), completed.focusId(), completed.focusValue(),
                    parentId(completed.root(), completed.focusId()),
                    ancestors(completed.root(), completed.focusId()), AvlTreeViewState.Phase.COMPLETED,
                    null, null, null, true), EventImportance.TERMINAL);
        }
        return Reduction.unchanged(previous, EventImportance.TRANSIENT);
    }

    private static AvlTreeViewState snapshot(
            AvlTreeViewState previous,
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

    private static Set<Long> nextAncestors(AvlTreeViewState previous, long nodeId, Long parentId) {
        Set<Long> result = new LinkedHashSet<>(ancestors(previous.root(), nodeId));
        if (result.isEmpty()) {
            result.addAll(previous.ancestorIds());
            if (parentId != null) {
                result.add(parentId);
            }
        }
        result.remove(nodeId);
        return result;
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

    private static AvlTreeViewState.Phase structurePhase(AvlTreeEvent.StructurePhase phase) {
        if (phase == AvlTreeEvent.StructurePhase.INSERTED) {
            return AvlTreeViewState.Phase.INSERTING;
        }
        if (phase == AvlTreeEvent.StructurePhase.REMOVED) {
            return AvlTreeViewState.Phase.REMOVING;
        }
        if (phase == AvlTreeEvent.StructurePhase.VALUE_REPLACED) {
            return AvlTreeViewState.Phase.REPLACING;
        }
        if (phase == AvlTreeEvent.StructurePhase.LINK_CHANGED) {
            return AvlTreeViewState.Phase.LINKING;
        }
        return AvlTreeViewState.Phase.ROTATING;
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
