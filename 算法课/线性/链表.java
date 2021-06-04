package 线性;
import Structure.Linkedlist;

public class 链表{
    public static void main(String[] args) {
        int i=0;
        Linkedlist<Integer> linkedlist=new Linkedlist<>();
        while(i!=12){
            linkedlist.Insert(i);
            i++;
        }
        linkedlist.Show(linkedlist);
        System.out.println("---------------------------");
        i=0;
        while(i!=3){
            linkedlist.InsertMiddle(i+3,i);
            i++;
        }
        linkedlist.Show(linkedlist);
        System.out.println("---------------------------");
        i=0;
        while(i!=5){
            linkedlist.InsertHead(i);
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