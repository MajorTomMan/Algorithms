package 线性;
import Structure.Data;
import Structure.Linkedlist;
import Structure.Node;

public class 链表{
    public static void main(String[] args) {
        int i=0;
        Linkedlist<Integer> linkedlist=new Linkedlist<>();
        while(i!=12){
            Node<Integer> node=new Node<>();
            Data<Integer> data=new Data<>();
            data.saveData=i;
            node.data=data;
            linkedlist.Insert(node);
            i++;
        }
        linkedlist.Show(linkedlist);
        System.out.println("---------------------------");
        i=0;
        while(i!=3){
            Node<Integer> node=new Node<>();
            Data<Integer> data=new Data<>();
            data.saveData=i;
            node.data=data;
            linkedlist.InsertMiddle(i+3,node);
            i++;
        }
        linkedlist.Show(linkedlist);
        System.out.println("---------------------------");
        i=0;
        while(i!=5){
            Node<Integer> node=new Node<>();
            Data<Integer> data=new Data<>();
            data.saveData=i;
            node.data=data;
            linkedlist.InsertHead(node);
            i++;
        }
        linkedlist.Show(linkedlist);
        System.out.println("---------------------------");
        linkedlist.Delete(3);
        linkedlist.Delete(3);
        linkedlist.Delete(3);
        linkedlist.Delete(3);
        linkedlist.Show(linkedlist);
        System.out.println("---------------------------");
    }
}