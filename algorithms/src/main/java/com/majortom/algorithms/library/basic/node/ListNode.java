package com.majortom.algorithms.library.basic.node;

import com.majortom.algorithms.core.event.structure.LinkedStructureEvent;
import com.majortom.algorithms.core.runtime.ExecutionEvents;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class ListNode<T> {
    private static final AtomicLong IDS = new AtomicLong(1L);

    private final long id;
    private T value;
    private ListNode<T> next;
    private ListNode<T> previous;

    public ListNode() {
        this(null);
    }

    public ListNode(T value) {
        this(IDS.getAndIncrement(), value, null, null);
    }

    public ListNode(T value, ListNode<T> next, ListNode<T> previous) {
        this(IDS.getAndIncrement(), value, next, previous);
    }

    public ListNode(long id, T value, ListNode<T> next, ListNode<T> previous) {
        if (id <= 0) {
            throw new IllegalArgumentException("node id must be positive");
        }
        this.id = id;
        this.value = value;
        this.next = next;
        this.previous = previous;
    }

    public long getId() {
        return id;
    }

    public T getValue() {
        return value;
    }

    public ListNode<T> getNext() {
        return next;
    }

    public ListNode<T> getPrevious() {
        return previous;
    }

    public void setValue(T value) {
        if (Objects.equals(this.value, value)) {
            return;
        }
        T previousValue = this.value;
        this.value = value;
        ExecutionEvents.emit(new LinkedStructureEvent.ValueChanged(id, previousValue, value));
    }

    public void setNext(ListNode<T> next) {
        if (this.next == next) {
            return;
        }
        Long previousNextId = id(this.next);
        this.next = next;
        ExecutionEvents.emit(new LinkedStructureEvent.NextChanged(id, previousNextId, id(next)));
    }

    public void setPrevious(ListNode<T> previous) {
        if (this.previous == previous) {
            return;
        }
        Long previousPreviousId = id(this.previous);
        this.previous = previous;
        ExecutionEvents.emit(new LinkedStructureEvent.PreviousChanged(id, previousPreviousId, id(previous)));
    }

    @Override
    public java.lang.String toString() {
        return "ListNode{id=" + id + ", value=" + value + "}";
    }

    private static Long id(ListNode<?> node) {
        return node == null ? null : node.id;
    }
}
