package 线性表应用;
import Structure.Stack;
public class 栈 {
    public static void main(String[] args) {
        int i=0;
        Stack<Integer> stack=new Stack<>();
        while(i!=6){
            stack.push(i);
            i++;
        }
        i=0;
        while(i!=6){
            System.out.println(stack.pop());
            i++;
        }
    }
}