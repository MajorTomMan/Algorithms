
package basic.structure;

import java.util.Iterator;

import basic.structure.node.PolyListNode;

/* 一元多项式链表 */
public class PolyList implements Iterable<PolyListNode> {
    public PolyListNode head;

    public boolean isEmpty() {
        return head == null;
    }

    public void add(int power, int exp) {
        // TODO Auto-generated method stub
        if (isEmpty()) {
            head = new PolyListNode(power, exp, null);
            return;
        }
        head = add(power, exp, head);
    }

    private PolyListNode add(int power, int exp, PolyListNode node) {
        // TODO Auto-generated method stub
        if (node == null) {
            node = new PolyListNode(power, exp, null);
            return node;
        }
        PolyListNode temp = add(power, exp, node.getNext());
        node.setNext(temp);
        return node;
    }

    public void remove(int power, int exp) {
        // TODO Auto-generated method stub
        if (isEmpty()) {
            return;
        }
        if (head.getPower() == power && head.getExp() == exp) {
            if (head.getNext() == null) {
                head = null;
            } else {
                head = head.getNext();
            }
            return;
        }
        for (PolyListNode pre = head,
                current = head.getNext(); current != null; pre = current, current = current.getNext()) {
            if (current.getPower() == power && current.getExp() == exp) {
                pre.setNext(current.getNext());
                current = null;
                return;
            }
        }
    }

    public void display() {
        forEach((v) -> {
            System.out.println(v);
        });
    }

    public Iterator<PolyListNode> iterator() {
        // TODO Auto-generated method stub
        return new ListIterator();
    }

    private class ListIterator implements Iterator<PolyListNode> {
        private PolyListNode current = head;

        public boolean hasNext() {
            // TODO Auto-generated method stub
            return current != null;
        }

        public PolyListNode next() {
            // TODO Auto-generated method stub
            PolyListNode data = current;
            current = current.getNext();
            return data;
        }
    }
}
