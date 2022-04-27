package Basic.Structure;

import java.util.Iterator;

import Basic.Structure.Interface.ILinkedlist;
import Basic.Structure.Node.Data;
import Basic.Structure.Node.Node;

public class Linkedlist<T> implements ILinkedlist<T>,Iterable<T>{
    private Node<T> head;
    private int size;
    public Linkedlist(Node<T> head) {
        this.head = head;
    }
    public Linkedlist(T var){
        this.head=new Node<>(new Data<T>(var),null);
    }
    public void Delete(int index) {
        int i=0;
        Node<T> temp=head;
        Node<T> pre=new Node<>(null,null);
        while(i!=index&&index!=0){
            pre=temp;
            temp=temp.next;
            i++;
        }
        pre.next=temp.next;
    }
    public void Delete(T var){
        Node<T> temp=head;
        Node<T> pre=new Node<T>(null,null);
        boolean flag=false;
        while(temp.data.saveData!=var&&temp!=null){
            if(temp.data.saveData.equals(var)){
                flag=true;
                break;
            }
            pre=temp;
            temp=temp.next;
        }
        if(flag){
            pre.next=temp.next;
            temp=null;
        }
        else{
            System.out.println("未找到数据");
        }
    }

    public void InsertTail(T var) {
        if(head==null){
            head=new Node<>(new Data<T>(var),null);
            return;
        }
        Node<T> temp=head;
        Node<T> node=new Node<>(new Data<T>(var),null);
        while(temp.next!=null){
            temp=temp.next;
        }
        temp.next=node;
    }

    public void InsertMiddle(int index,T var) {
        if(head==null){
            head=new Node<>(new Data<T>(var),null);
            return;
        }
        Node<T> temp=head;
        Node<T> node=new Node<>(new Data<T>(var),null);
        int i=0;
        while(i!=index){
            temp=temp.next;
            i++;
        }
        node.next=temp.next;
        temp.next=node;
    }
    public void Insert(T var) {
        Node<T> node=new Node<>(new Data<T>(var),null);
        node.next=head.next;
        head.next=node;
        size++;
    }
    public void Reverse(){
        Node<T> beg=null,mid=head,end=head.next;
        while(end!=null){
            mid.next=beg;
            beg=mid;
            mid=end;
            end=end.next;
        }
        mid.next=beg;
        head=mid;
    }
    public void Show(Node<T> node){
        if(node==null){
            return;
        }
        Show(node.next);
        System.out.print(node.data.saveData+" ");
    }
    public boolean contains(T target) {
        for(Node<T> data=head;data!=null;data=data.next){
            if(data.data.saveData==target){
                return true;
            }
        }
        return false;
    }
    
    public int Size(){
        if(size==0){
            Node<T> temp=head;
            size=Size(temp);
        }
        return size;
    }
    private int Size(Node<T> node){
        if(node==null){
            return 0;
        }
        Size(node.next);
        return size++;
    }
    public Node<T> getHead() {
        return head;
    }
    public void setHead(Node<T> head) {
        this.head = head;
    }
    public int getSize() {
        return size;
    }
    public void setSize(int size) {
        this.size = size;
    }
    @Override
    public Iterator<T> iterator() {
        // TODO Auto-generated method stub
        return new ListIterator();
    }
    private class ListIterator implements Iterator<T>{
        private Node<T> current=head;
        @Override
        public boolean hasNext() {
            // TODO Auto-generated method stub
            return current!=null;
        }

        @Override
        public T next() {
            // TODO Auto-generated method stub
            Data<T> data=current.data;
            current=current.next;
            return data.saveData;
        }
    }
}
