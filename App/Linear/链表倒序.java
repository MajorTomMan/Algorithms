package linear;


import java.util.ArrayList;

import basic.structure.LinkedList;
import basic.structure.interfaces.List;
import basic.structure.node.ListNode;
/**
 * 链表倒序
 */
public class 链表倒序 {
    public static void main(String[] args) {
        List<Integer> list=new LinkedList<>();
        for(int i=0;i<10;i++){
            list.add(i);
        }
    }
    private static ArrayList<Integer> reverse(ListNode<Integer> node){
        ArrayList<ListNode<Integer>> List = new ArrayList<>();
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