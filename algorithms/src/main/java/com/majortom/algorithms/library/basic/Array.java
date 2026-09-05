package com.majortom.algorithms.library.basic;

import com.majortom.algorithms.core.runtime.StructureEvents;
import com.majortom.algorithms.library.structure.ArrayStructure;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

public final class Array<T> implements ArrayStructure<T> {
    private static final int DEFAULT_CAPACITY = 10;

    private Object[] elements;
    private int size;

    public Array() {
        elements = new Object[DEFAULT_CAPACITY];
    }

    public Array(Collection<? extends T> values) {
        Objects.requireNonNull(values, "values");
        elements = new Object[Math.max(DEFAULT_CAPACITY, values.size())];
        for (T value : values) {
            elements[size++] = value;
        }
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public T get(int index) {
        checkIndex(index);
        return element(index);
    }

    @Override
    public T set(int index, T value) {
        checkIndex(index);
        T previous = element(index);
        if (Objects.equals(previous, value)) {
            return previous;
        }
        elements[index] = value;
        StructureEvents.arrayUpdated(index, previous, value);
        return previous;
    }

    @Override
    public void insert(int index, T value) {
        checkInsertIndex(index);
        ensureCapacity(size + 1);
        System.arraycopy(elements, index, elements, index + 1, size - index);
        elements[index] = value;
        size++;
        StructureEvents.arrayInserted(index, value);
    }

    @Override
    public T remove(int index) {
        checkIndex(index);
        T removed = element(index);
        int moved = size - index - 1;
        if (moved > 0) {
            System.arraycopy(elements, index + 1, elements, index, moved);
        }
        elements[--size] = null;
        StructureEvents.arrayRemoved(index, removed);
        return removed;
    }

    @Override
    public void swap(int leftIndex, int rightIndex) {
        checkIndex(leftIndex);
        checkIndex(rightIndex);
        if (leftIndex == rightIndex) {
            return;
        }
        T left = element(leftIndex);
        T right = element(rightIndex);
        elements[leftIndex] = right;
        elements[rightIndex] = left;
        StructureEvents.arraySwapped(leftIndex, rightIndex, right, left);
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private int index;

            @Override
            public boolean hasNext() {
                return index < size;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return element(index++);
            }
        };
    }

    private void ensureCapacity(int required) {
        if (required <= elements.length) {
            return;
        }
        int capacity = Math.max(required, elements.length + (elements.length >> 1) + 1);
        Object[] expanded = new Object[capacity];
        System.arraycopy(elements, 0, expanded, 0, size);
        elements = expanded;
    }

    @SuppressWarnings("unchecked")
    private T element(int index) {
        return (T) elements[index];
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
