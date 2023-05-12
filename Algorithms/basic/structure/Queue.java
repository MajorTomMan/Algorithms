
package basic.structure;

import java.util.Iterator;

import basic.structure.iface.IQueue;
import basic.structure.node.ListNode;

public class Queue<T> implements IQueue<T>,Iterable<T>{
    private ListNode<T> front; // 删除
    private ListNode<T> rear; // 插入
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
        ListNode<T> oldRear=rear;
        ListNode<T> node = new ListNode<>(data,null);
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
    public void Show() {
        // TODO Auto-generated method stub
        Show(front);
    }
    private ListNode<T> Show(ListNode<T> node){
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
        private ListNode<T> current=front;
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