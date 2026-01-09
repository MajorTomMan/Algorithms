package com.majortom.algorithms.app.leetcode.algo.string;

import java.util.Scanner;

import com.majortom.algorithms.core.basic.LinkedList;
import com.majortom.algorithms.core.interfaces.Queue;
import com.majortom.algorithms.core.interfaces.Stack;

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