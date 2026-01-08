package com.majortom.algorithms.app.leetcode.linear;

import com.majortom.algorithms.core.basic.LinkedList;
import com.majortom.algorithms.core.interfaces.Queue;

public class 二进制计算 {
    public static void main(String[] args) {
        int data = 65;
        int Result = 0;
        int temp;
        int i = 0;
        int j = 0;
        String str = "";
        Queue<Integer> queue = new LinkedList<>();
        while (data != 0) {
            queue.add(data % 2);
            data /= 2;
        }
        i = queue.size();
        while (!queue.isEmpty()) {
            temp = queue.poll();
            if (temp == 1) {
                temp *= Math.pow(2, j);
            }
            Result += temp;
            j++;
            i--;
        }
        str = Character.toString((char) Result);
        System.out.println("data:" + Result);
        System.out.println(str);
    }
}
