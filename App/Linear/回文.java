package linear;

import java.util.Scanner;

import basic.structure.LinkedList;
import basic.structure.interfaces.Queue;
import basic.structure.interfaces.Stack;

public class 回文 {
    public static void main(String[] args) {
        int i = 0;
        boolean flag = true;
        Stack<String> stack = new LinkedList<>();
        Queue<String> queue = new LinkedList<>();
        System.out.print("请输入数据:");
        Scanner scanner = new Scanner(System.in);
        String context = scanner.nextLine();
        scanner.close();
        while (i != context.length()) {
            String ch = "" + context.charAt(i);
            stack.push(ch);
            queue.add(ch);
            i++;
        }
        while (flag && stack.isEmpty()) {
            if (!stack.pop().equals(queue.poll())) {
                flag = false;
            }
        }
        if (flag) {
            System.out.println(context + ":是回文");
        } else {
            System.out.println(context + ":不是回文");
        }
    }
}