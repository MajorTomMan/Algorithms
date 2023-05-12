
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import basic.structure.node.ListNode;

public class 两数相加 extends Common{
    public static void main(String[] args) {
        int[] nums_1={1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},nums_2={5,6,4};
        ListNode<Integer> root_1=buildLinkedList(nums_1),root_2=buildLinkedList(nums_2);
        System.out.println(addTwoNumbers(root_1,root_2));
    }
    public static ListNode<Integer> addTwoNumbers(ListNode<Integer> l1,ListNode<Integer> l2) {
        List<Integer> list_1=new LinkedList<>();
        List<Integer> list_2=new LinkedList<>();
        List<Integer> result_list=new ArrayList<>();
        for(ListNode<Integer> first=l1,second=l2;;){
            if(first!=null&&second!=null){
                list_1.add(first.data);
                list_2.add(second.data);
                first=first.next;
                second=second.next;
                continue;
            }
            if(first==null&&second!=null){
                list_2.add(second.data);
                second=second.next;
                continue;
            }
            if(first!=null&&second==null){
                list_1.add(first.data);
                first=first.next;
                continue;
            }
            if(first==null&&second==null){
                Collections.reverse(list_1);
                Collections.reverse(list_2);
                break;
            }
        }
        addToList(list_1, list_2,result_list);
        ListNode<Integer> result=buildList(result_list);
        return result;
    }
    public static ListNode<Integer> buildList(List<Integer> list){
        int i=list.size()-1;
        ListNode<Integer> root=new ListNode<Integer>(list.get(i));
        ListNode<Integer> temp=root;
        i--;
        while(i>=0){
            temp.next=new ListNode<Integer>(list.get(i));
            i--;
            temp=temp.next;
        }
        return root;
    }
    public static void addToList(List<Integer> list_1,List<Integer> list_2,List<Integer> result){
        StringBuffer s1=new StringBuffer();
        StringBuffer s2=new StringBuffer();
        for(int i=0;i<list_1.size();i++){
            s1.append(list_1.get(i));
        }
        for(int i=0;i<list_2.size();i++){
            s2.append(list_2.get(i));
        }
        BigInteger value1=new BigInteger(new String(s1));
        BigInteger value2=new BigInteger(new String(s2));
        BigInteger result_int=value1.add(value2);
        String result_String=""+result_int;
        for (char data: result_String.toCharArray()) {
            result.add(Integer.parseInt(""+data));
        }
    }
}
