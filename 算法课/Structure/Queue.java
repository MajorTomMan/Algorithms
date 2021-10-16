package Structure;

import java.util.Iterator;
import Structure.Interface.IQueue;
import Structure.Node.Data;
import Structure.Node.Node;

public class Queue<T> implements IQueue<T>,Iterable<T>{
    private Node<T> front; // 删除
    private Node<T> rear; // 插入
    private int size;

    public T dequeue() {
        T data = front.data.saveData;
        front = front.next;
        if (isEmpty()) {
            rear = null;
        }
        size--;
        return data;
    }

    public void enqueue(T var) {
        Node<T> oldRear=rear;
        Node<T> node = new Node<>(new Data<T>(var),null);
        rear=node;
        if(isEmpty()){
            front=node;
        }
        else{
            oldRear.next=node;
        }
    }

    public boolean isEmpty() {
        if (front == null) {
            return true;
        }
        return false;
    }

    public int getSize() {
        return size;
    }
    @Override
    public Iterator<T> iterator() {
        // TODO Auto-generated method stub
        return new ListIterator();
    }
    private class ListIterator implements Iterator<T>{
        private Node<T> current=front;
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