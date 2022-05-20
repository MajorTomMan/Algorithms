package Basic.Structure;

import Basic.Structure.Interface.ICyclelist;
import Basic.Structure.Node.Node;

public class Cyclelist<T> implements ICyclelist<T> {
    private Node<T> head;
    private int size;
    @Override
    public void Initial(T data) {
        // TODO Auto-generated method stub
        Node<T> node=new Node<>(data,null);
        node.data=data;
        head=node;
        node.next=head;
    }

    @Override
    public void Delete(int index) {
        // TODO Auto-generated method stub
        int i=0;
        if(head==null){
            return;
        }
        Node<T> temp=head;
        Node<T> pre=new Node<>(null,null);
        while(i!=index){
            pre=temp;
            temp=temp.next;
            i++;
        }
        head=temp.next;
        pre.next=head;
        System.out.println("被删除的节点是:"+temp.data);
    }

    @Override
    public void Insert(T data) {
        // TODO Auto-generated method stub
        if(head==null){
            Initial(data);
            return;
        }
        Node<T> temp=head;
        Node<T> node=new Node<>(data,null);
        while(temp.next!=head){
            temp=temp.next;
        }
        node.next=head;
        temp.next=node;
    }
    @Override
    public int Size() {
        // TODO Auto-generated method stub
        return size;
    }
    public void Show() {
        // TODO Auto-generated method stub
        Show(head);
    }
    private void Show(Node<T> node) {
        // TODO Auto-generated method stub
        if(node.next==head){
            return;
        }
        Show(node.next);
        System.out.println(node.data);
    }
    public Node<T> getHead() {
        return head;
    }

    public void setHead(Node<T> head) {
        this.head = head;
    }

    @Override
    public boolean isEmpty() {
        // TODO Auto-generated method stub
        return head==null;
    }
}
