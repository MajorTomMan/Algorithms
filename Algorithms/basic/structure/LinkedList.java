package basic.structure;

import java.util.Iterator;

import basic.structure.iface.IList;
import basic.structure.node.ListNode;

public class LinkedList<T extends Comparable<T>> implements IList<T>,Iterable<T>{
    private ListNode<T> head;
    private int size;
    public LinkedList(ListNode<T> head) {
        this.head = head;
        size++;
    }
    public LinkedList(T data){
        this.head=new ListNode<>(data,null);
        size++;
    }
    public LinkedList(){
        
    }
    @Override
    public boolean isEmpty() {
        // TODO Auto-generated method stub
        return head==null;
    }
    public void Show() {
        // TODO Auto-generated method stub
        ListNode<T> temp=head;
        Show(temp);
    }
    public void Delete(int index) {
        int i=0;
        ListNode<T> temp=head;
        ListNode<T> pre=new ListNode<>(null,null);
        while(i!=index&&index!=0){
            pre=temp;
            temp=temp.next;
            i++;
        }
        pre.next=temp.next;
    }
    public void Delete(T data){
        ListNode<T> temp=head;
        ListNode<T> pre=new ListNode<T>(null,null);
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
            head=new ListNode<>(data,null);
            return;
        }
        InsertTail(data,head);
    }
    private ListNode<T> InsertTail(T data,ListNode<T> node) {
        if(node==null){
            ++size;
            return new ListNode<>(data,null);
        }
        node.next=InsertTail(data,node.next);
        return node;
    }

    public void InsertMiddle(int index,T data) {
        if(isEmpty()){
            head=new ListNode<>(data,null);
            return;
        }
        ListNode<T> temp=head;
        ListNode<T> node=new ListNode<>(data,null);
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
            head=new ListNode<>(data,null);
            return;
        }
        ListNode<T> node=new ListNode<>(data,null);
        node.next=head.next;
        head.next=node;
        size++;
    }
    public void Reverse(){
        ListNode<T> beg=null,mid=head,end=head.next;
        while(end!=null){
            mid.next=beg;
            beg=mid;
            mid=end;
            end=end.next;
        }
        mid.next=beg;
        head=mid;
    }
    public void Show(ListNode<T> node){
        if(node==null){
            return;
        }
        Show(node.next);
        System.out.print(node.data+" ");
    }
    public boolean contains(T target) {
        for(ListNode<T> data=head;data!=null;data=data.next){
            if(data.data==target){
                return true;
            }
        }
        return false;
    }
    public void Sort() {
        ListNode<T> temp=head;
        Sort(temp);
    }
    private void Sort(ListNode<T> node){
        for(ListNode<T> i=node;i!=null;i=i.next){
            for(ListNode<T> j=i.next;j!=null;j=j.next){
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
            ListNode<T> temp=head;
            size=Size(temp);
        }
        return size;
    }
    private int Size(ListNode<T> node){
        if(node==null){
            return 0;
        }
        Size(node.next);
        return size++;
    }
    public ListNode<T> getHead() {
        return head;
    }
    public void setHead(ListNode<T> head) {
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
        private ListNode<T> current=head;
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
