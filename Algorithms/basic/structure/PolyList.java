/*
 * @Date: 2023-04-26 16:51:29
 * @LastEditors: hujunhao hujunhao@rtczsz.com
 * @LastEditTime: 2023-04-27 11:55:44
 * @FilePath: /alg/Algorithms/basic/structure/PolyList.java
 */
package basic.structure;

import java.util.Iterator;

import basic.structure.iface.IPolyList;
import basic.structure.node.PolyListNode;
/* 一元多项式链表 */
public class PolyList extends IPolyList implements Iterable<PolyListNode>{
    @Override
    public void insert(int power, int exp) {
        // TODO Auto-generated method stub
        if (isEmpty()) {
            head = new PolyListNode(power, exp, null);
            return;
        }
        head = insert(power, exp, head);
    }

    private PolyListNode insert(int power, int exp, PolyListNode node) {
        // TODO Auto-generated method stub
        if (node == null) {
            node = new PolyListNode(power, exp, null);
            return node;
        }
        PolyListNode temp= insert(power, exp, node.getNext());
        node.setNext(temp);
        return node;
    }

    @Override
    public void delete(int power, int exp) {
        // TODO Auto-generated method stub
        if (isEmpty()) {
            return;
        }
        if (head.getPower() == power && head.getExp() == exp) {
            if(head.getNext()==null){
                head=null;
            }
            else{
                head=head.getNext();
            }
            return;
        }
        for (PolyListNode pre = head,current = head.getNext(); current != null; pre = current, current = current.getNext()) {
            if (current.getPower() == power && current.getExp() == exp) {
                pre.setNext(current.getNext());
                current = null;
                return;
            }
        }
    }
    public PolyListNode getHead(){
        return head;
    }
    @Override
    public void show() {
        // TODO Auto-generated method stub
        super.show();
    }

    @Override
    public void clear() {
        // TODO Auto-generated method stub
        super.clear();
    }
    @Override
    public boolean isEmpty() {
        // TODO Auto-generated method stub
        return super.isEmpty();
    }

    @Override
    public Iterator<PolyListNode> iterator() {
        // TODO Auto-generated method stub
        return new ListIterator();
    }
    private class ListIterator implements Iterator<PolyListNode>{
        private PolyListNode current=head;
        @Override
        public boolean hasNext() {
            // TODO Auto-generated method stub
            return current!=null;
        }

        @Override
        public PolyListNode next() {
            // TODO Auto-generated method stub
            PolyListNode data=current;
            current=current.getNext();
            return data;
        }
    }
}
