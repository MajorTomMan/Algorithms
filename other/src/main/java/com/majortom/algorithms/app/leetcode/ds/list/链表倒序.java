package com.majortom.algorithms.app.leetcode.ds.list;


import java.util.ArrayList;

import com.majortom.algorithms.library.basic.LinkedList;
import com.majortom.algorithms.library.basic.node.ListNode;
/**
 * 链表倒序
 */
public class 链表倒序 {
    public static void main(String[] args) {
        LinkedList<Integer> list=new LinkedList<>();
        for(int i=0;i<10;i++){
            list.enqueue(i);
        }
    }
    private static ArrayList<Integer> reverse(ListNode<Integer> node){
        ArrayList<ListNode<Integer>> List = new ArrayList<>();
        ArrayList<Integer> result=new ArrayList<>();
        while(node!=null){
            List.add(node);
            node=node.getNext();
        }
        for (int i = List.size()-1; i>=0;i--) {
            result.add(List.get(i).getValue());
        }
        return result;
    }
}