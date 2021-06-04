package Structure;


public class Stack<T> implements IStack<T>{
    private Node<T> top;
    int size;
    @Override
    public T pop() {
        // TODO Auto-generated method stub
        T data;
        data=top.data.saveData;
        top=top.next;
        size--;
        return data;
    }
    @Override
    public void push(T var) {
        Node<T> node=new Node<T>();
        if(isEmpty()){
            Inital(var);
            return;
        }
        createNode(node);
        node.data.saveData=var;
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
    public void Inital(T var) {
        // TODO Auto-generated method stub
        Node<T> node=new Node<T>();
        createNode(node);
        node.data.saveData=var;
        top=node;
        size++;
    }
    public int getSize(){
        return size;
    }
    public Node<T> getTop() {
        return top;
    }
    public void setTop(Node<T> top) {
        this.top = top;
    }
    private void createNode(Node<T> node){
        node.data=new Data<T>();
    }
}
