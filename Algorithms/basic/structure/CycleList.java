

package basic.structure;

import basic.structure.iface.ICycleList;
import basic.structure.node.ListNode;

public class CycleList<T> implements ICycleList<T> {
    private ListNode<T> head;
    private int size;


    public CycleList(){
    }

    @Override
    public void delete(int index) {
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
    public void insert(T data) {
        // TODO Auto-generated method stub
        if(head==null){
            head=new ListNode<>(data);
            head.next=null;
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
    public int size() {
        // TODO Auto-generated method stub
        return size;
    }
    public void show() {
        // TODO Auto-generated method stub
        show(head);
    }
    private void show(ListNode<T> node) {
        // TODO Auto-generated method stub
        if(node.next==head){
            return;
        }
        show(node.next);
        System.out.println(node.data);
    }

    @Override
    public boolean isEmpty() {
        // TODO Auto-generated method stub
        return head==null;
    }
}
