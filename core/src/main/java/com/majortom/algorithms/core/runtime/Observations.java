package com.majortom.algorithms.core.runtime;

import com.majortom.algorithms.core.event.observation.ObservationEvent;

import java.util.List;

/** Thin construction/publishing helpers for explicit factual algorithm observations. */
public final class Observations {
    private Observations() {
    }

    public static void compared(String leftSource, int leftIndex, String rightSource, int rightIndex) {
        ExecutionEvents.observe(new ObservationEvent.Compared(
                new ObservationEvent.IndexRef(leftSource, leftIndex),
                new ObservationEvent.IndexRef(rightSource, rightIndex)));
    }

    public static void compared(String source, int index, Object value) {
        ExecutionEvents.observe(new ObservationEvent.Compared(
                new ObservationEvent.IndexRef(source, index),
                new ObservationEvent.ValueRef(value)));
    }

    public static void visited(String domain, long id) {
        ExecutionEvents.observe(new ObservationEvent.Visited(new ObservationEvent.EntityRef(domain, id)));
    }

    public static void visited(int row, int column) {
        ExecutionEvents.observe(new ObservationEvent.Visited(new ObservationEvent.CoordinateRef(row, column)));
    }

    public static void examined(String domain, long fromId, long toId) {
        ExecutionEvents.observe(new ObservationEvent.Examined(
                new ObservationEvent.EntityRef(domain, fromId),
                new ObservationEvent.EntityRef(domain, toId)));
    }

    public static void examined(int fromRow, int fromColumn, int toRow, int toColumn) {
        ExecutionEvents.observe(new ObservationEvent.Examined(
                new ObservationEvent.CoordinateRef(fromRow, fromColumn),
                new ObservationEvent.CoordinateRef(toRow, toColumn)));
    }

    public static void matched(int index, int length) {
        ExecutionEvents.observe(new ObservationEvent.Matched(index, length));
    }

    public static void fallback(int fromIndex, int toIndex) {
        ExecutionEvents.observe(new ObservationEvent.Fallback(fromIndex, toIndex));
    }

    public static void backtracked(String domain, long id) {
        ExecutionEvents.observe(new ObservationEvent.Backtracked(new ObservationEvent.EntityRef(domain, id)));
    }

    public static void backtracked(int row, int column) {
        ExecutionEvents.observe(new ObservationEvent.Backtracked(new ObservationEvent.CoordinateRef(row, column)));
    }

    public static void pathFound(List<ObservationEvent.Reference> refs) {
        ExecutionEvents.observe(new ObservationEvent.PathFound(refs));
    }
}
