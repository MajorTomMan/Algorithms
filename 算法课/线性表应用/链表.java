package 线性表应用;
import Structure.Linkedlist;

public class 链表{
    public static void main(String[] args) {
        int i=0;
        Linkedlist<Integer> linkedlist=new Linkedlist<>();
        while(i!=12){
            linkedlist.Insert(i);
            i++;
        }
        linkedlist.Show(linkedlist.getHead());
        linkedlist.Delete(3);
        System.out.println("--------------------------");
        linkedlist.Reverse(linkedlist);
        linkedlist.Show(linkedlist.getHead());
    }
}