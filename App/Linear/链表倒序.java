/*
 * @Date: 2023-04-26 16:51:29
 * @LastEditors: hujunhao hujunhao@rtczsz.com
 * @LastEditTime: 2023-04-26 17:34:34
 * @FilePath: /alg/App/Linear/链表倒序.java
 */
import java.util.ArrayList;

import basic.structure.LinkedList;
import basic.structure.node.ListNode;
/**
 * 链表倒序
 */
public class 链表倒序 {
    public static void main(String[] args) {
        LinkedList<Integer> list=new LinkedList<>(22);
        for(int i=0;i<10;i++){
            list.Insert(i);
        }
        System.out.println(reverse(list.getHead()));
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