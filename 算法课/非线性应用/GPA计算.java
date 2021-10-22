package 非线性应用;

import java.util.Random;

import 基本.Structure.Stack;
import 查找.Structure.ArrayST;
import 查找.Structure.BinarySearchST;
import 查找.Structure.OrderedSequentialSearchST;

public class GPA计算 {
    public static void main(String[] args) {
        int i=0;
        Double result=0.0;
        Double result_a=0.0;
        Double result_l=0.0;
        String[] rank={"A+","A","A-","B+","B","B-","C+","C","C-","D","F"};
        String[] grade={"4.33","4.00","3.67","3.33","3.00","2.67","2.33","2.00","1.67","1.00","0.00"};
        Stack<Double> stack=new Stack<Double>();
        Stack<Double> stack_a=new Stack<Double>();
        Stack<Double> stack_l=new Stack<Double>();
        BinarySearchST<String,Double> BST=new BinarySearchST<>(11);
        ArrayST<String,Double> AST=new ArrayST<>(11);
        OrderedSequentialSearchST<String,Double> LST=new OrderedSequentialSearchST<>();
        Random random=new Random();
        while(i!=rank.length){
            int temp=random.nextInt(10);
            BST.put(rank[i],Double.parseDouble(grade[i]));
            AST.put(rank[i],Double.parseDouble(grade[i]));
            LST.put(rank[temp],Double.parseDouble(grade[temp]));
            i++;
        }
        i=0;
        while(i!=rank.length){
            stack.push((Double)BST.get(rank[i]));
            stack_a.push((Double)AST.get(rank[i]));
            if(LST.contains(rank[i])){
                stack_l.push((Double)LST.get(rank[i]));
            }
            i++;
        }
        while(!stack.isEmpty()){
            result+=stack.pop();
            result_a+=stack_a.pop();
        }
        while(!stack_l.isEmpty()){
            result_l+=stack_l.pop();
        }
        System.out.printf("GPA为:%.2f\n",result/rank.length);
        System.out.printf("GPA为:%.2f\n",result_a/rank.length);
        System.out.printf("GPA为:%.2f\n",result_l/rank.length);
        for (String str:LST.keys()) {
            System.out.print(str);
        }
        System.out.println();
        System.out.println(LST.ceiling("C"));
        System.out.println(LST.floor("D"));
    }
}
