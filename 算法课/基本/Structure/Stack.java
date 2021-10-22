package 基本.Structure;


import java.util.Iterator;

import 基本.Structure.Interface.IStack;
import 基本.Structure.Node.Data;
import 基本.Structure.Node.Node;

public class Stack<T> implements IStack<T>,Iterable<T>{
    private Node<T> top;
    int size;
    @Override
    public T pop() {
        // TODO Auto-generated method stub
        T data=top.data.saveData;
        top=top.next;
        size--;
        return data;
    }
    @Override
    public void push(T var) {
        Node<T> node=new Node<>(new Data<T>(var),null);
        if(isEmpty()){
            top=node;
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
    public int getSize() {
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
            Data<T> data=current.data;
            current=current.next;
            return data.saveData;
        }
    }
}
