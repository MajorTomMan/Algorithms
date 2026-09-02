package com.majortom.algorithms.library.structure;

import com.majortom.algorithms.core.runtime.ExecutionEvents;
import com.majortom.algorithms.library.basic.node.ListNode;
import com.majortom.algorithms.library.structure.event.StackStructureEvent;

import java.util.NoSuchElementException;

public final class LinkedStack<T> implements StackStructure<T> {
    private ListNode<T> head;
    private int size;

    @Override public int size() { return size; }
    @Override public boolean isEmpty() { return size == 0; }

    @Override
    public void push(T value) {
        ListNode<T> node = new ListNode<>(value);
        node.next = head;
        if (head != null) head.pre = node;
        head = node;
        size++;
        ExecutionEvents.emit(new StackStructureEvent.Pushed(value));
    }

    @Override
    public T pop() {
        if (head == null) throw new NoSuchElementException("stack is empty");
        T value = head.data;
        head = head.next;
        if (head != null) head.pre = null;
        size--;
        ExecutionEvents.emit(new StackStructureEvent.Popped(value));
        return value;
    }

    @Override
    public T peek() {
        if (head == null) throw new NoSuchElementException("stack is empty");
        return head.data;
    }

    @Override public ListNode<T> raw() { return head; }
}
