

package basic.structure;

import java.util.Iterator;

import basic.structure.iface.IBag;
import basic.structure.node.ListNode;
/**
 * Bag
 */
public class Bag<T> implements IBag<T>,Iterable<T>{
    private ListNode<T> top;
    private int size;
    
    @Override
    public void add(T data) {
        ListNode<T> node=new ListNode<>(data,null);
        if(isEmpty()){
            top=node;
            size++;
            return;
        }
        node.next=top;
        top=node;
        size++;
    }
    @Override
    public boolean isEmpty() {
        // TODO Auto-generated method stub
        if(top==null){
            return true;
        }
        else{ 
            return false;
        }
    }
    @Override
    public int size() {
        // TODO Auto-generated method stub
        return size;
    }
    @Override
    public Iterator<T> iterator() {
        // TODO Auto-generated method stub
        return new ListIterator();
    }
    private class ListIterator implements Iterator<T>{
        private ListNode<T> current=top;
        @Override
        public boolean hasNext() {
            // TODO Auto-generated method stub
            return current!=null;
        }

        @Override
        public T next() {
            // TODO Auto-generated method stub
            T data=current.data;
            current=current.next;
            return data;
        }
    }
}