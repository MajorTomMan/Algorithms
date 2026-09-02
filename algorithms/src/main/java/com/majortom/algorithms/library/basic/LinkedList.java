package com.majortom.algorithms.library.basic;

import com.majortom.algorithms.core.runtime.ExecutionEvents;
import com.majortom.algorithms.library.basic.node.ListNode;
import com.majortom.algorithms.library.structure.LinkedStructure;
import com.majortom.algorithms.library.structure.event.LinkedStructureEvent;

import java.util.Iterator;
import java.util.NoSuchElementException;

/** Direct doubly linked-list implementation used by the V2 structure model. */
public final class LinkedList<T> implements LinkedStructure<T> {
    private ListNode<T> head;
    private ListNode<T> tail;
    private int size;

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public T get(int index) {
        return nodeAt(index).data;
    }

    @Override
    public void insert(int index, T value) {
        checkInsertIndex(index);
        ListNode<T> next = index == size ? null : nodeAt(index);
        ListNode<T> previous = next == null ? tail : next.pre;
        ListNode<T> inserted = new ListNode<>(value, next, previous);
        if (previous == null) {
            head = inserted;
        } else {
            previous.next = inserted;
        }
        if (next == null) {
            tail = inserted;
        } else {
            next.pre = inserted;
        }
        size++;
        ExecutionEvents.emit(new LinkedStructureEvent.Inserted(index, value));
    }

    @Override
    public T remove(int index) {
        ListNode<T> node = nodeAt(index);
        ListNode<T> previous = node.pre;
        ListNode<T> next = node.next;
        if (previous == null) {
            head = next;
        } else {
            previous.next = next;
        }
        if (next == null) {
            tail = previous;
        } else {
            next.pre = previous;
        }
        size--;
        ExecutionEvents.emit(new LinkedStructureEvent.Removed(index, node.data));
        return node.data;
    }

    @Override
    public T update(int index, T value) {
        ListNode<T> node = nodeAt(index);
        T previous = node.data;
        node.data = value;
        ExecutionEvents.emit(new LinkedStructureEvent.Updated(index, previous, value));
        return previous;
    }

    @Override
    public ListNode<T> raw() {
        return head;
    }

    public ListNode<T> tail() {
        return tail;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private ListNode<T> current = head;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public T next() {
                if (current == null) {
                    throw new NoSuchElementException();
                }
                T value = current.data;
                current = current.next;
                return value;
            }
        };
    }

    private ListNode<T> nodeAt(int index) {
        checkIndex(index);
        if (index < size / 2) {
            ListNode<T> current = head;
            for (int i = 0; i < index; i++) {
                current = current.next;
            }
            return current;
        }
        ListNode<T> current = tail;
        for (int i = size - 1; i > index; i--) {
            current = current.pre;
        }
        return current;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index=" + index + ", size=" + size);
        }
    }

    private void checkInsertIndex(int index) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("index=" + index + ", size=" + size);
        }
    }
}
