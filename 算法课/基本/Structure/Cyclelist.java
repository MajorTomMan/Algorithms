package 基本.Structure;

import 基本.Structure.Interface.ICyclelist;
import 基本.Structure.Node.Data;
import 基本.Structure.Node.Node;

public class Cyclelist<T> implements ICyclelist<T> {
    private Node<T> head;
    @Override
    public void Initial(T var) {
        // TODO Auto-generated method stub
        Node<T> node=new Node<>(new Data<T>(var),null);
        node.data.saveData=var;
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
        System.out.println("被删除的节点是:"+temp.data.saveData);
    }

    @Override
    public void Insert(T var) {
        // TODO Auto-generated method stub
        if(head==null){
            Initial(var);
            return;
        }
        Node<T> temp=head;
        Node<T> node=new Node<>(new Data<T>(var),null);
        while(temp.next!=head){
            temp=temp.next;
        }
        node.next=head;
        temp.next=node;
    }
    @Override
    public void Show(Node<T> node) {
        // TODO Auto-generated method stub
        if(node.next==head){
            return;
        }
        Show(node.next);
        System.out.println(node.data.saveData);
    }
    public Node<T> getHead() {
        return head;
    }

    public void setHead(Node<T> head) {
        this.head = head;
    }
}
