package linear;

import java.util.Stack;

public class 判断括号 {
    public static void main(String[] args) {
        System.out.println(isValid("(())))"));
    }

    public static boolean isValid(String s) {
        if(s.length()==0||s.length()==1){
            return false;
        }
        Stack<Character> stack= new Stack<>();
        for (int i = 0;i<s.length();i++) {
            if (s.charAt(i) == ')') {
                if(stack.isEmpty()){
                    return false;
                }
                if (stack.pop() != '(') {
                    return false;
                }
                continue;
            }
            if (s.charAt(i) == ']') {
                if(stack.isEmpty()){
                    return false;
                }
                if (stack.pop() != '[') {
                    return false;
                }
                continue;
            }
            if (s.charAt(i) == '}') {
                if(stack.isEmpty()){
                    return false;
                }
                if (stack.pop() != '{') {
                    return false;
                }
                continue;
            }
            stack.push(s.charAt(i));
        }
        if(stack.isEmpty()){
            return true;
        }
        else{
            return false;
        }
    }
}
