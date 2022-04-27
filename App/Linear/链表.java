
import Basic.Structure.Linkedlist;

public class 链表{
    public static void main(String[] args) {
        int i=0;
        Linkedlist<Integer> linkedlist=new Linkedlist<>(99);
        while(i!=12){
            linkedlist.Insert(i);
            i++;
        }
        for (Integer t:linkedlist) {
            System.out.println(t);
        }
        linkedlist.Delete(3);
        linkedlist.Reverse();
        for (Integer t:linkedlist) {
            System.out.println(t);
        }
        System.out.println("--------------------------");
        System.out.println(linkedlist.Size());
    }
}