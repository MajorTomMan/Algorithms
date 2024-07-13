
package basic.structure;

import java.util.Iterator;

import basic.structure.node.ListNode;

/**
 * Bag
 */
public class Bag<T> implements Iterable<T> {
    private ListNode<T> top;
    private int size;

    public void add(T data) {
        ListNode<T> node = new ListNode<>(data);
        if (isEmpty()) {
            top = node;
            size++;
            return;
        }
        node.next = top;
        top = node;
        size++;
    }

    public boolean isEmpty() {
        // TODO Auto-generated method stub
        if (top == null) {
            return true;
        } else {
            return false;
        }
    }

    public int size() {
        // TODO Auto-generated method stub
        return size;
    }

    public Iterator<T> iterator() {
        // TODO Auto-generated method stub
        return new ListIterator();
    }

    private class ListIterator implements Iterator<T> {
        private ListNode<T> current = top;

        public boolean hasNext() {
            // TODO Auto-generated method stub
            return current != null;
        }

        public T next() {
            // TODO Auto-generated method stub
            T data = current.data;
            current = current.next;
            return data;
        }
    }
}