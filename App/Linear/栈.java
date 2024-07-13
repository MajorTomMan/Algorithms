package linear;


import basic.structure.LinkedList;
import basic.structure.interfaces.Stack;
public class 栈 {
    public static void main(String[] args) {
        int i=0;
        Stack<Integer> stack=new LinkedList<>();
        while(i!=6){
            stack.push(i);
            i++;
        }
        i=0;
        for (Integer data : stack) {
            System.out.println(data);
        }
    }
}