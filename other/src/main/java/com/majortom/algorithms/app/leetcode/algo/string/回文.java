package com.majortom.algorithms.app.leetcode.algo.string;

import java.util.Scanner;

import com.majortom.algorithms.library.basic.LinkedList;
import com.majortom.algorithms.library.structure.QueueStructure;
import com.majortom.algorithms.library.structure.StackStructure;

public class 回文 {
    public static void main(String[] args) {
        int i = 0;
        boolean flag = true;
        StackStructure<String> stack = new LinkedList<>();
        QueueStructure<String> queue = new LinkedList<>();
        System.out.print("请输入数据:");
        Scanner scanner = new Scanner(System.in);
        String context = scanner.nextLine();
        scanner.close();
        while (i != context.length()) {
            String ch = "" + context.charAt(i);
            stack.push(ch);
            queue.enqueue(ch);
            i++;
        }
        while (flag && stack.isEmpty()) {
            if (!stack.pop().equals(queue.dequeue())) {
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