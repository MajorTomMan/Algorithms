package Structure;

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
        Node<T> node = new Node<T>();
        createNode(node);
        node.data.saveData = var;
        if (rear == null) {
            rear = node;
            front.next = rear;
            size++;
        } else {
            Node temp = new Node();
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
        Node<T> node = new Node<T>();
        createNode(node);
        node.data.saveData = var;
        front = node;
        size++;
    }

    public int getSize() {
        return size;
    }

    private void createNode(Node<T> node) {
        node.data = new Data<T>();
    }

    @Override
    public void show(Queue<T> queue) {
        // TODO Auto-generated method stub
        Node front = queue.front;
        while (front != null) {
            System.out.println(front.data.saveData);
            front = front.next;
        }
    }
}