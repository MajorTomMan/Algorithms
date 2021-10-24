package Linear;
import Basic.Structure.Stack;
public class 栈 {
    public static void main(String[] args) {
        int i=0;
        Stack<Integer> stack=new Stack<>();
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