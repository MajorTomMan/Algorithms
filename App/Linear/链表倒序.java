import java.util.ArrayList;

import Basic.Structure.Linkedlist;
import Basic.Structure.Node.Node;

/**
 * 链表倒序
 */
public class 链表倒序 {
    public static void main(String[] args) {
        Linkedlist<Integer> list=new Linkedlist<>(22);
        for(int i=0;i<10;i++){
            list.Insert(i);
        }
        System.out.println(reverse(list.getHead()));
    }
    private static ArrayList<Integer> reverse(Node<Integer> node){
        ArrayList<Node<Integer>> List = new ArrayList<>();
        ArrayList<Integer> result=new ArrayList<>();
        while(node!=null){
            List.add(node);
            node=node.next;
        }
        for (int i = List.size()-1; i>=0;i--) {
            result.add(List.get(i).data);
        }
        return result;
    }
}