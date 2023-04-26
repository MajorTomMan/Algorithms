
import java.util.Random;

import basic.structure.LinkedList;

public class 链表{
    public static void main(String[] args) {
        int i=0;
        LinkedList<Integer> linkedlist=new LinkedList<>(99);
        Random random=new Random();
        while(i!=12){
            int j=random.nextInt(100)+1;
            linkedlist.Insert(j);
            i++;
        }
        System.out.println();
        for (Integer t:linkedlist) {
            System.out.print(t+" ");
        }
        System.out.println();
        linkedlist.Delete(3);
        linkedlist.Reverse();
        for (Integer t:linkedlist) {
            System.out.print(t+" ");
        }
        System.out.println();
        System.out.println("--------------------------");
        System.out.println(linkedlist.Size());
        linkedlist.Sort();
        for (Integer t:linkedlist) {
            System.out.print(t+" ");
        }
    }
}