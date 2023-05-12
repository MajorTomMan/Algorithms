

package basic.structure;

import basic.structure.iface.ICycleList;
import basic.structure.node.ListNode;

public class CycleList<T> implements ICycleList<T> {
    private ListNode<T> head;
    private int size;
    @Override
    public void Initial(T data) {
        // TODO Auto-generated method stub
        ListNode<T> node=new ListNode<>(data,null);
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
        ListNode<T> temp=head;
        ListNode<T> pre=new ListNode<>(null,null);
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
        ListNode<T> temp=head;
        ListNode<T> node=new ListNode<>(data,null);
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
    private void Show(ListNode<T> node) {
        // TODO Auto-generated method stub
        if(node.next==head){
            return;
        }
        Show(node.next);
        System.out.println(node.data);
    }
    public ListNode<T> getHead() {
        return head;
    }

    public void setHead(ListNode<T> head) {
        this.head = head;
    }

    @Override
    public boolean isEmpty() {
        // TODO Auto-generated method stub
        return head==null;
    }
}
