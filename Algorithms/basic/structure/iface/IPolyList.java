
package basic.structure.iface;

import basic.structure.node.PolyListNode;

public abstract class IPolyList {
    protected PolyListNode head;

    public abstract void insert(int power, int exp);

    public abstract void delete(int power, int exp);

    protected void clear() {
        head = clear(head);
    }

    private PolyListNode clear(PolyListNode node) {
        if (node == null) {
            return null;
        }
        PolyListNode temp = clear(node.getNext());
        return temp;
    }

    protected void show() {
        show(head);
    }

    private PolyListNode show(PolyListNode node) {
        if (node != null) {
            System.out.println(node);
        }
        if (node == null) {
            return node;
        }
        return show(node.getNext());
    }

    protected boolean isEmpty() {
        if (head == null) {
            return true;
        }
        return false;
    }
}
