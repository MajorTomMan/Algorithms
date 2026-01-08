package com.majortom.algorithms.app.leetcode.linear;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class 快乐数 {
    private static List<Character> stack = new ArrayList<>();
    private static Set<Integer> isExists = new HashSet<>();

    public static void main(String[] args) {
        System.out.println(isHappy(3));
    }

    public static boolean isHappy(int n) {
        if (n == 1) {
            return true;
        }
        for (char c : String.valueOf(n).toCharArray()) {
            stack.add(c);
        }
        if (stack.size() == 1 && (stack.get(0) - '0') % 2 == 0) {
            return false;
        }
        int temp = calculate();
        stack.clear();
        isExists.add(temp);
        if (isCorrect(temp)) {
            return true;
        }
        return isHappy(temp);
    }

    private static int calculate() {
        int result = 0;
        for (Character c : stack) {
            int t = (c - '0');
            result = result + t * t;
        }
        return result;
    }

    private static boolean isCorrect(int n) {
        boolean isFirst = false, isRight = false;
        for (char c : String.valueOf(n).toCharArray()) {
            if (!isFirst) {
                isFirst = true;
                if (c - '0' == 1) {
                    isRight = true;
                }
            } else {
                if (c - '0' != 0) {
                    isRight = false;
                }
            }
        }
        return isRight;
    }
}
