package Structure;

import Structure.Interface.IQueue;
import Structure.Node.Data;
import Structure.Node.Node;

public class Queue<T> implements IQueue<T> {
    public Node<T> front; // 删除
    public Node<T> rear; // 插入
    public int size;

    public T pop() {
        T data = front.data.saveData;
        front = front.next;
        size--;
        if (front == null) {
            rear = null;
        }
        return data;
    }

    public void push(T var) {
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
}