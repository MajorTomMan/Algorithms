package 线性表应用;

import 基本.Structure.Stack;



public class 二进制{
    public static void main(String[] args) {
        int i=16;
        Stack<Integer> stack=new Stack<>();
        while(i!=0){
            stack.push(i%2);
            i=i/2;
        }
        while(!stack.isEmpty()){
            System.out.print(stack.pop());
        }
    }
}
