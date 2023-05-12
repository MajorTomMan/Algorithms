
package basic.structure;


import java.util.Iterator;

import basic.structure.iface.IStack;
import basic.structure.node.ListNode;

public class Stack<T> implements IStack<T>,Iterable<T>{
    private ListNode<T> top;
    private int size;
    @Override
    public T pop() {
        // TODO Auto-generated method stub
        T data=top.data;
        top=top.next;
        size--;
        return data;
    }
    @Override
    public void push(T data) {
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
    public int Size() {
        // TODO Auto-generated method stub
        return size;
    }
    public void Show() {
        // TODO Auto-generated method stub
        Show(top);
    }
    private ListNode<T> Show(ListNode<T> node){
        if(node==null){
            return node;
        }
        System.out.print(node.data+" ");
        Show(node.next);
        return node;
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
