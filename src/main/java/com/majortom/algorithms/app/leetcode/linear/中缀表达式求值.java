package com.majortom.algorithms.app.leetcode.linear;

import com.majortom.algorithms.core.basic.LinkedList;
import com.majortom.algorithms.core.interfaces.Stack;

public class 中缀表达式求值 {
    public static void main(String[] args) {
        int i = 0;
        Double result = 0.0;
        Double temp = 0.0;
        String statement = "(1+((2*3)+(4*5)))";
        char[] state = statement.toCharArray();
        Character cr = ' ';
        Stack<Character> ops = new LinkedList<>();
        Stack<Double> vals = new LinkedList<>();
        while (i != state.length) {
            if (state[i] == '(') {
                i++;
                continue;
            } else if (state[i] == ')') {
                cr = (Character) ops.pop();
                if (cr == '+') {
                    result = (Double) vals.pop();
                    temp = (Double) vals.pop();
                    result += temp;
                    vals.push(result);
                } else if (cr == '-') {
                    result = (Double) vals.pop();
                    temp = (Double) vals.pop();
                    result -= temp;
                    vals.push(result);
                } else if (cr == '*') {
                    result = (Double) vals.pop();
                    temp = (Double) vals.pop();
                    result *= temp;
                    vals.push(result);
                } else if (cr == '/') {
                    result = (Double) vals.pop();
                    temp = (Double) vals.pop();
                    result /= temp;
                    vals.push(result);
                }
                i++;
                continue;
            }
            if (state[i] == '+' || state[i] == '-' || state[i] == '*' || state[i] == '/') {
                ops.push((char) state[i]);
            } else {
                vals.push(Double.parseDouble(Character.toString(state[i])));
            }
            i++;
        }
        System.out.println(statement + " 这句表达式计算结果是:" + vals.pop());
    }
}