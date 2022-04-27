package Basic.Structure.Node;

public class Node<T>{
    public Data<T> data;
    public Node<T> next;
    public Node(Data<T> data, Node<T> next) {
        this.data = data;
        this.next = next;
    }
    @Override
    public String toString() {
        return "Node [data=" + data + ", next=" + next + "]";
    }
}
