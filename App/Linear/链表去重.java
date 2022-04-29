
import java.util.Random;

import Basic.Structure.Linkedlist;
import Basic.Structure.Node.Node;

public class 链表去重 {
    public static void main(String[] args) {
        Linkedlist<Integer> list=new Linkedlist<Integer>();
        int i=0;
        Random random=new Random();
        while(i!=12){
            int j=random.nextInt(100)+1;
            list.Insert(j);
            list.Insert(j);
            list.Insert(j);
            list.Insert(j);
            i++;
        }
        list.Sort();
        System.out.println(deleteDuplicates(list.getHead()));
    }
    public static Node<Integer> deleteDuplicates(Node<Integer> head) {
        if(head==null){
            return null;
        }
        if(head.next==null){
            return head;
        }
        return deleteDuplicates(head.next,head);
    }
    private static Node<Integer> deleteDuplicates(Node<Integer> node,Node<Integer> previous){
        if(node==null){
            return node;
        }
        deleteDuplicates(node.next,node);
        if(node.data==previous.data){ //如果前一个节点数据相同于后一个节点
            previous.next=node.next; //则直接接上后一个节点的链表
        }
        return previous;
    }
    //处理不了乱序时的链表重复
    private static Node<Integer> deleteDuplicatesFor(Node<Integer> head){
        if(head==null){
            return null;
        }
        Node<Integer> temp=head.next;
        Node<Integer> pre=head;
        for(;temp!=null;){
            if(pre.data==temp.data){
                pre.next=temp.next;
                temp=temp.next;
            }
            else{
                pre=temp;
                temp=temp.next;
            }
        }
        return head;
    }
}
