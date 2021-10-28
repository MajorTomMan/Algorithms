package Linear;


import java.util.Scanner;

import Basic.Structure.Queue;
import Basic.Structure.Stack;

public class 回文 {
    public static void main(String[] args) {
        int i = 0;
        boolean flag=true;
        Stack<String> stack = new Stack<String>();
        Queue<String> queue = new Queue<String>();
        System.out.print("请输入数据:");
        Scanner scanner = new Scanner(System.in);
        String context = scanner.nextLine();
        scanner.close();
        while(i!=context.length()){
            String ch=""+context.charAt(i);
            stack.push(ch);
            queue.enqueue(ch);
            i++;
        }
        while(flag&&stack.isEmpty()){
            if(!stack.pop().equals(queue.dequeue())){
                flag=false;
            }
        }
        if(flag){
            System.out.println(context+":是回文");
        }
        else{
            System.out.println(context+":不是回文");
        }
    }
}