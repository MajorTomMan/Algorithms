package com.majortom.algorithms.library.structure;

import com.majortom.algorithms.core.runtime.ExecutionEvents;
import com.majortom.algorithms.library.basic.node.ListNode;
import com.majortom.algorithms.library.structure.event.QueueStructureEvent;

import java.util.NoSuchElementException;

public final class LinkedQueue<T> implements QueueStructure<T> {
    private ListNode<T> head;
    private ListNode<T> tail;
    private int size;

    @Override public int size() { return size; }
    @Override public boolean isEmpty() { return size == 0; }

    @Override
    public void enqueue(T value) {
        ListNode<T> node = new ListNode<>(value, null, tail);
        if (tail == null) head = node;
        else tail.next = node;
        tail = node;
        size++;
        ExecutionEvents.emit(new QueueStructureEvent.Enqueued(value));
    }

    @Override
    public T dequeue() {
        if (head == null) throw new NoSuchElementException("queue is empty");
        T value = head.data;
        head = head.next;
        if (head == null) tail = null;
        else head.pre = null;
        size--;
        ExecutionEvents.emit(new QueueStructureEvent.Dequeued(value));
        return value;
    }

    @Override public T front() {
        if (head == null) throw new NoSuchElementException("queue is empty");
        return head.data;
    }

    @Override public T rear() {
        if (tail == null) throw new NoSuchElementException("queue is empty");
        return tail.data;
    }

    @Override public ListNode<T> raw() { return head; }
}
