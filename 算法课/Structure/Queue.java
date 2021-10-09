package Structure;

import Structure.Interface.IQueue;
import Structure.Node.Data;
import Structure.Node.Node;

public class Queue<T> implements IQueue<T> {
    private Node<T> front; // 删除
    private Node<T> rear; // 插入
    private int size;

    public T dequeue() {
        T data = front.data.saveData;
        front = front.next;
        size--;
        if (front == null) {
            rear = null;
        }
        return data;
    }

    public void enqueue(T var) {
        if (isEmpty()) {
            Inital(var);
            return;
        }
        Node<T> node = new Node<>(new Data<T>(var),null);
        if (rear == null) {
            rear = node;
            front.next = rear;
            size++;
        } else {
            rear.next=node;
            rear=node;
            size++;
        }
    }

    public boolean isEmpty() {
        if (front == null) {
            return true;
        }
        return false;
    }

    public void Inital(T var) {
        Node<T> node = new Node<>(new Data<T>(var),null);
        front = node;
        size++;
    }

    public int getSize() {
        return size;
    }
    @Override
    public void show(Node<T> node) {
        // TODO Auto-generated method stub
        if(node==null){
            return;
        }
        show(node.next);
        System.out.println(node.data.saveData);
    }
    public Node<T> getFront() {
        return front;
    }

    public void setFront(Node<T> front) {
        this.front = front;
    }

    public Node<T> getRear() {
        return rear;
    }

    public void setRear(Node<T> rear) {
        this.rear = rear;
    }

    public void setSize(int size) {
        this.size = size;
    }
}