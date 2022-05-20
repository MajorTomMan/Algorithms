import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import Basic.Structure.Node.Node;
import NonLinear.Example;

public class 两数相加 extends Example{
    public static void main(String[] args) {
        int[] nums_1={1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},nums_2={5,6,4};
        Node<Integer> root_1=buildLinkedList(nums_1),root_2=buildLinkedList(nums_2);
        System.out.println(addTwoNumbers(root_1,root_2));
    }
    public static Node<Integer> addTwoNumbers(Node<Integer> l1,Node<Integer> l2) {
        List<Integer> list_1=new LinkedList<>();
        List<Integer> list_2=new LinkedList<>();
        List<Integer> result_list=new ArrayList<>();
        for(Node<Integer> first=l1,second=l2;;){
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
        Node<Integer> result=buildList(result_list);
        return result;
    }
    public static Node<Integer> buildList(List<Integer> list){
        int i=list.size()-1;
        Node<Integer> root=new Node<Integer>(list.get(i));
        Node<Integer> temp=root;
        i--;
        while(i>=0){
            temp.next=new Node<Integer>(list.get(i));
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
