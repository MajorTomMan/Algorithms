package linear;


import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;


public class 回文数 {
    public static void main(String[] args) {
        System.out.println(isPalindrome(-121));
    }
    public static boolean isPalindrome(int x) {
        Stack<String> stack =new Stack<>();
        Queue<String> queue=new LinkedList<>();
        if(x<0){
            return false;
        }
        else{
            isPalindrome(x,stack, queue);
        }
        while(!stack.empty()){
            if(!stack.pop().equals(queue.poll())){
                return false;
            }
        }
        return true;
    }
    private static int isPalindrome(int x,Stack<String> stack,Queue<String> queue){
        if(x==0){
            return x;
        }
        else if(x<0){
        }
        stack.push(String.valueOf(x%10));
        queue.add(String.valueOf(x%10));
        isPalindrome(x/10,stack,queue);
        return x;
    }
}
