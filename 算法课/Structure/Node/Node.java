package Structure.Node;

public class Node<T>{
    public Data<T> data;
    public Node<T> next;
    public Node(Data<T> data, Node<T> next) {
        this.data = data;
        this.next = next;
    }
}
