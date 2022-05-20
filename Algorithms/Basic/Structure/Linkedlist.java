package Basic.Structure;

import java.util.Iterator;

import Basic.Structure.Interface.ILinkedlist;
import Basic.Structure.Node.Node;

public class Linkedlist<T extends Comparable<T>> implements ILinkedlist<T>,Iterable<T>{
    private Node<T> head;
    private int size;
    public Linkedlist(Node<T> head) {
        this.head = head;
        size++;
    }
    public Linkedlist(T data){
        this.head=new Node<>(data,null);
        size++;
    }
    public Linkedlist(){
        
    }
    @Override
    public boolean isEmpty() {
        // TODO Auto-generated method stub
        return head==null;
    }
    public void Show() {
        // TODO Auto-generated method stub
        Node<T> temp=head;
        Show(temp);
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
    public void Delete(T data){
        Node<T> temp=head;
        Node<T> pre=new Node<T>(null,null);
        boolean flag=false;
        while(temp.data!=data&&temp!=null){
            if(temp.data.equals(data)){
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

    public void InsertTail(T data){
        if(isEmpty()){
            head=new Node<>(data,null);
            return;
        }
        InsertTail(data,head);
    }
    private Node<T> InsertTail(T data,Node<T> node) {
        if(node==null){
            ++size;
            return new Node<>(data,null);
        }
        node.next=InsertTail(data,node.next);
        return node;
    }

    public void InsertMiddle(int index,T data) {
        if(isEmpty()){
            head=new Node<>(data,null);
            return;
        }
        Node<T> temp=head;
        Node<T> node=new Node<>(data,null);
        int i=0;
        while(i!=index){
            temp=temp.next;
            i++;
        }
        node.next=temp.next;
        temp.next=node;
    }
    public void Insert(T data) {
        if(isEmpty()){
            head=new Node<>(data,null);
            return;
        }
        Node<T> node=new Node<>(data,null);
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
        System.out.print(node.data+" ");
    }
    public boolean contains(T target) {
        for(Node<T> data=head;data!=null;data=data.next){
            if(data.data==target){
                return true;
            }
        }
        return false;
    }
    public void Sort() {
        Node<T> temp=head;
        Sort(temp);
    }
    private void Sort(Node<T> node){
        for(Node<T> i=node;i!=null;i=i.next){
            for(Node<T> j=i.next;j!=null;j=j.next){
                if(i.data.compareTo(j.data)==1){
                    T data=i.data;
                    i.data=j.data;
                    j.data=data;
                }
            }
        }
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
            T data=current.data;
            current=current.next;
            return data;
        }
    }
}
