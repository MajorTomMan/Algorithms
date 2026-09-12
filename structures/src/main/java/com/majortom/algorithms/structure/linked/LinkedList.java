package com.majortom.algorithms.structure.linked;

import com.majortom.algorithms.core.annotation.Structure;
import com.majortom.algorithms.core.runtime.StructureEvents;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

@Structure(id = "linked-list", name = "Linked List", contract = LinkedStructure.class)
@Structure(id = "stack", name = "Stack", contract = StackStructure.class)
@Structure(id = "queue", name = "Queue", contract = QueueStructure.class)
public final class LinkedList<T> implements LinkedStructure<T>, StackStructure<T>, QueueStructure<T> {
    private ListNode<T> head;
    private ListNode<T> tail;
    private int size;

    @Override
    public int size() {
        return size;
    }

    @Override
    public void initialize(Collection<? extends T> values) {
        java.util.Objects.requireNonNull(values, "values");
        ArrayList<ListNode<T>> nodes = new ArrayList<>(values.size());
        for (T value : values) {
            nodes.add(new ListNode<>(value));
        }
        for (int index = 0; index < nodes.size(); index++) {
            ListNode<T> previous = index == 0 ? null : nodes.get(index - 1);
            ListNode<T> next = index + 1 == nodes.size() ? null : nodes.get(index + 1);
            nodes.get(index).initializeLinks(next, previous);
        }
        head = nodes.isEmpty() ? null : nodes.getFirst();
        tail = nodes.isEmpty() ? null : nodes.getLast();
        size = nodes.size();
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public T get(int index) {
        return nodeAt(index).getValue();
    }

    @Override
    public void insert(int index, T value) {
        checkInsertIndex(index);
        ListNode<T> next;
        if (index == size) {
            next = null;
        } else {
            next = nodeAt(index);
        }
        ListNode<T> previous;
        if (next == null) {
            previous = tail;
        } else {
            previous = next.getPrevious();
        }
        ListNode<T> inserted = new ListNode<>(value);
        StructureEvents.linkedNodeInserted(inserted.getId(), value);
        inserted.setPrevious(previous);
        inserted.setNext(next);
        if (previous == null) {
            head = inserted;
        } else {
            previous.setNext(inserted);
        }
        if (next == null) {
            tail = inserted;
        } else {
            next.setPrevious(inserted);
        }
        size++;
    }

    @Override
    public T remove(int index) {
        ListNode<T> node = nodeAt(index);
        ListNode<T> previous = node.getPrevious();
        ListNode<T> next = node.getNext();
        if (previous == null) {
            head = next;
        } else {
            previous.setNext(next);
        }
        if (next == null) {
            tail = previous;
        } else {
            next.setPrevious(previous);
        }
        node.setPrevious(null);
        node.setNext(null);
        size--;
        T value = node.getValue();
        StructureEvents.linkedNodeRemoved(node.getId(), value);
        return value;
    }

    @Override
    public T set(int index, T value) {
        ListNode<T> node = nodeAt(index);
        T previous = node.getValue();
        node.setValue(value);
        return previous;
    }

    @Override
    public ListNode<T> head() {
        return head;
    }

    @Override
    public ListNode<T> tail() {
        return tail;
    }

    @Override
    public void push(T value) {
        insert(0, value);
    }

    @Override
    public T pop() {
        if (isEmpty()) {
            throw new NoSuchElementException("stack is empty");
        }
        return remove(0);
    }

    @Override
    public T peek() {
        if (head == null) {
            throw new NoSuchElementException("stack is empty");
        }
        return head.getValue();
    }

    @Override
    public void enqueue(T value) {
        insert(size, value);
    }

    @Override
    public T dequeue() {
        if (isEmpty()) {
            throw new NoSuchElementException("queue is empty");
        }
        return remove(0);
    }

    @Override
    public T front() {
        if (head == null) {
            throw new NoSuchElementException("queue is empty");
        }
        return head.getValue();
    }

    @Override
    public T rear() {
        if (tail == null) {
            throw new NoSuchElementException("queue is empty");
        }
        return tail.getValue();
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
                T value = current.getValue();
                current = current.getNext();
                return value;
            }
        };
    }

    private ListNode<T> nodeAt(int index) {
        checkIndex(index);
        if (index < size / 2) {
            ListNode<T> current = head;
            for (int i = 0; i < index; i++) {
                current = current.getNext();
            }
            return current;
        }
        ListNode<T> current = tail;
        for (int i = size - 1; i > index; i--) {
            current = current.getPrevious();
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
