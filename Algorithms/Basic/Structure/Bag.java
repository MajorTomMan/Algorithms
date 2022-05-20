package Basic.Structure;

import Basic.Structure.Node.Node;

import java.util.Iterator;

import Basic.Structure.Interface.IBag;
/**
 * Bag
 */
public class Bag<T> implements IBag<T>,Iterable<T>{
    private Node<T> top;
    private int size;
    @Override
    public void add(T data) {
        Node<T> node=new Node<>(data,null);
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
    public int Size() {
        // TODO Auto-generated method stub
        return size;
    }
    @Override
    public Iterator<T> iterator() {
        // TODO Auto-generated method stub
        return new ListIterator();
    }
    private class ListIterator implements Iterator<T>{
        private Node<T> current=top;
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