package com.majortom.algorithms.core.runtime;

import com.majortom.algorithms.core.event.structure.ArrayStructureEvent;
import com.majortom.algorithms.core.event.structure.GraphStructureEvent;
import com.majortom.algorithms.core.event.structure.LinkedStructureEvent;
import com.majortom.algorithms.core.event.structure.StringStructureEvent;
import com.majortom.algorithms.core.event.structure.TreeStructureEvent;

/**
 * Thin domain-oriented publishing helpers for factual StructureEvent instances.
 *
 * <p>These methods never mutate or validate Structures. Callers perform the real mutation first,
 * then publish the fact through this convenience layer.</p>
 */
public final class StructureEvents {
    private StructureEvents() {
    }

    public static void arrayInserted(int index, Object value) {
        ExecutionEvents.emit(new ArrayStructureEvent.Inserted(index, value));
    }

    public static void arrayRemoved(int index, Object value) {
        ExecutionEvents.emit(new ArrayStructureEvent.Removed(index, value));
    }

    public static void arrayUpdated(int index, Object previousValue, Object value) {
        ExecutionEvents.emit(new ArrayStructureEvent.Updated(index, previousValue, value));
    }

    public static void arraySwapped(int leftIndex, int rightIndex, Object leftValue, Object rightValue) {
        ExecutionEvents.emit(new ArrayStructureEvent.Swapped(leftIndex, rightIndex, leftValue, rightValue));
    }

    public static void graphVertexAdded(long vertexId, Object value) {
        ExecutionEvents.emit(new GraphStructureEvent.VertexAdded(vertexId, value));
    }

    public static void graphVertexRemoved(long vertexId, Object value) {
        ExecutionEvents.emit(new GraphStructureEvent.VertexRemoved(vertexId, value));
    }

    public static void graphEdgeAdded(long edgeId, long fromId, long toId) {
        ExecutionEvents.emit(new GraphStructureEvent.EdgeAdded(edgeId, fromId, toId));
    }

    public static void graphEdgeRemoved(long edgeId, long fromId, long toId) {
        ExecutionEvents.emit(new GraphStructureEvent.EdgeRemoved(edgeId, fromId, toId));
    }

    public static void linkedNodeInserted(long nodeId, Object value) {
        ExecutionEvents.emit(new LinkedStructureEvent.NodeInserted(nodeId, value));
    }

    public static void linkedNodeRemoved(long nodeId, Object value) {
        ExecutionEvents.emit(new LinkedStructureEvent.NodeRemoved(nodeId, value));
    }

    public static void linkedValueChanged(long nodeId, Object previousValue, Object value) {
        ExecutionEvents.emit(new LinkedStructureEvent.ValueChanged(nodeId, previousValue, value));
    }

    public static void linkedNextChanged(long nodeId, Long previousNextNodeId, Long nextNodeId) {
        ExecutionEvents.emit(new LinkedStructureEvent.NextChanged(nodeId, previousNextNodeId, nextNodeId));
    }

    public static void linkedPreviousChanged(long nodeId, Long previousPreviousNodeId, Long previousNodeId) {
        ExecutionEvents.emit(new LinkedStructureEvent.PreviousChanged(nodeId, previousPreviousNodeId, previousNodeId));
    }

    public static void stringReplaced(int index, java.lang.String previousValue, java.lang.String value) {
        ExecutionEvents.emit(new StringStructureEvent.Replaced(index, previousValue, value));
    }

    public static void stringInserted(int index, java.lang.String value) {
        ExecutionEvents.emit(new StringStructureEvent.Inserted(index, value));
    }

    public static void stringRemoved(int index, java.lang.String value) {
        ExecutionEvents.emit(new StringStructureEvent.Removed(index, value));
    }

    public static void stringUpdated(int index, char previousValue, char value) {
        ExecutionEvents.emit(new StringStructureEvent.Updated(index, previousValue, value));
    }

    public static void treeNodeInserted(long nodeId, Object value) {
        ExecutionEvents.emit(new TreeStructureEvent.NodeInserted(nodeId, value));
    }

    public static void treeNodeRemoved(long nodeId, Object value) {
        ExecutionEvents.emit(new TreeStructureEvent.NodeRemoved(nodeId, value));
    }

    public static void treeValueChanged(long nodeId, Object previousValue, Object value) {
        ExecutionEvents.emit(new TreeStructureEvent.ValueChanged(nodeId, previousValue, value));
    }

    public static void treeLeftChanged(long nodeId, Long previousChildId, Long childId) {
        ExecutionEvents.emit(new TreeStructureEvent.LeftChanged(nodeId, previousChildId, childId));
    }

    public static void treeRightChanged(long nodeId, Long previousChildId, Long childId) {
        ExecutionEvents.emit(new TreeStructureEvent.RightChanged(nodeId, previousChildId, childId));
    }

    public static void treeRootChanged(Long previousRootId, Long rootId) {
        ExecutionEvents.emit(new TreeStructureEvent.RootChanged(previousRootId, rootId));
    }

    public static void treeChildInserted(long parentId, int index, long childId, Object value) {
        ExecutionEvents.emit(new TreeStructureEvent.ChildInserted(parentId, index, childId, value));
    }

    public static void treeChildRemoved(long parentId, int index, long childId, Object value) {
        ExecutionEvents.emit(new TreeStructureEvent.ChildRemoved(parentId, index, childId, value));
    }
}
