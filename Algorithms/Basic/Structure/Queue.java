package Basic.Structure;

import java.util.Iterator;

import Basic.Structure.Interface.IQueue;
import Basic.Structure.Node.Node;

public class Queue<T> implements IQueue<T>,Iterable<T>{
    private Node<T> front; // 删除
    private Node<T> rear; // 插入
    private int size;
    public T dequeue() {
        T data = front.data;
        front = front.next;
        if (isEmpty()) {
            rear = null;
        }
        size--;
        return data;
    }

    public void enqueue(T data) {
        Node<T> oldRear=rear;
        Node<T> node = new Node<>(data,null);
        rear=node;
        if(isEmpty()){
            front=node;
        }
        else{
            oldRear.next=node;
        }
        size++;
    }
    @Override
    public boolean isEmpty() {
        if (front == null) {
            return true;
        }
        return false;
    }
    @Override
    public int Size() {
        return size;
    }
    @Override
    public void Show() {
        // TODO Auto-generated method stub
        Show(front);
    }
    private Node<T> Show(Node<T> node){
        if(node==null){
            return node;
        }
        Show(node.next);
        System.out.print(node.data+" ");
        return node;
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
            T data=current.data;
            current=current.next;
            return data;
        }
    }
}