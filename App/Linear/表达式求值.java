package Linear;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import basic.structure.Stack;

/**
 * 后缀表达式求值
 */
public class 表达式求值 {
    private static Boolean isPost=false;
    public static void main(String[] args) {
        List<String> postFix = new ArrayList<>();
        List<String> preFix = new ArrayList<>();
        String target = "1+(5*6)/(7-4)";
        PostFix(postFix, target);
        PreFix(preFix, target);
        if(isPost){
            Calculate(postFix, target);
        }
        else{
            Calculate(preFix, target);
        }
    }

    /*
     * 从右往左扫描中缀表达式，每当遇到一个操作数，就将其输出到前缀表达式中。
     * 当遇到一个运算符时，将其压入栈中。
     * 如果遇到一个右括号，则将其压入栈中。
     * 如果遇到一个左括号，则将栈顶的运算符弹出并输出到前缀表达式中，直到遇到右括号为止。
     * 重复以上步骤，直到整个中缀表达式扫描完成。
     * 最后将栈中剩余的运算符依次弹出并输出到前缀表达式中，直到栈为空。
     * 将前缀表达式反转即可。
     */
    private static void PreFix(List<String> values, String target) {
        Stack<Character> operators = new Stack<>();
        int i = target.length() - 1;
        while (i >= 0) {
            Character c = target.charAt(i);
            if (Character.isDigit(c)) {
                values.add(c + "");
            } else if (c == ')') {
                operators.push(c);
            } else if (c == '(') {
                while (!operators.isEmpty()) {
                    Character cc = operators.pop();
                    if (cc != ')') {
                        values.add(cc + "");
                    } else {
                        break;
                    }
                }
            } else if (c == '+' || c == '-') {
                if (operators.isEmpty()) {
                    operators.push(c);
                } else {
                    while (!operators.isEmpty()) {
                        Character cc = operators.pop();
                        if (cc == '*' || cc == '/') {
                            values.add(cc + "");
                        } else {
                            break;
                        }
                    }
                    operators.push(c);
                }
            } else if (c == '*' || c == '/') {
                operators.push(c);
            }
            i--;
        }
        while (!operators.isEmpty()) {
            values.add(operators.pop() + "");
        }
        Collections.reverse(values);
        values.stream().forEach(item -> System.out.print(item + " "));
        System.out.println();
    }

    /*
     * 规则:
     * 从左至右遍历表达式，如果当前字符为数字，则直接输出。
     * 如果当前字符为左括号“（”，则将其入栈。
     * 如果当前字符为右括号“）”，则依次弹出栈顶运算符，并输出，直到弹出左括号为止。
     * 如果当前字符为运算符“+、-、*、/”，则进行如下操作：
     * 如果栈为空，则直接将运算符入栈。
     * 如果栈不为空，比较该运算符与栈顶运算符的优先级：
     * 如果该运算符的优先级大于栈顶运算符的优先级，或者两者的优先级相等且该运算符是左结合的，则将该运算符入栈。
     * 否则，将栈顶运算符弹出，并输出，直到遇到一个优先级比该运算符低的运算符或者栈为空为止，然后将该运算符入栈。
     */
    private static void PostFix(List<String> values, String target) {
        Stack<Character> operators = new Stack<>();
        int i = 0;
        while (i < target.length()) {
            Character c = target.charAt(i);
            if (Character.isDigit(c)) {
                values.add(c + "");
            } else if (c == '(') {
                operators.push(c);
            } else if (c == ')') {
                while (!operators.isEmpty()) {
                    Character cc = operators.pop();
                    if (cc != '(') {
                        values.add(cc + "");
                    } else {
                        break;
                    }
                }
            } else if (c == '+' || c == '-') {
                if (operators.isEmpty()) {
                    operators.push(c);
                } else {
                    while (!operators.isEmpty()) {
                        Character cc = operators.pop();
                        if (cc == '*' || cc == '/') {
                            values.add(cc + "");
                        } else {
                            break;
                        }
                    }
                    operators.push(c);
                }
            } else if (c == '*' || c == '/') {
                operators.push(c);
            }
            i++;
        }
        while (!operators.isEmpty()) {
            values.add(operators.pop() + "");
        }
        values.stream().forEach(item -> System.out.print(item + " "));
        System.out.println();
    }

    /*
     * 初始化一个空栈。
     * 从左到右依次遍历后缀表达式中的每个元素，包括操作数和运算符。
     * 如果遇到操作数，将其入栈。
     * 如果遇到运算符，则从栈中弹出相应数量的操作数，根据运算符进行计算，并将计算结果入栈。
     * 重复步骤 2-4，直到遍历完整个后缀表达式。
     * 最终栈中仅剩下一个元素，即为后缀表达式的计算结果。
     */
    private static void Calculate(List<String> values, String target) {
        Stack<Integer> result = new Stack<>();
        String s = "";
        for (String ss : values) {
            s += ss;
        }
        if(isPost){
            int i = 0;
            while (i < s.length()) {
                Calculate(s, result, i);
                i++;
            }
        }
        else{
            int i = s.length()-1;
            while (i>=0) {
                Calculate(s, result, i);
                i--;
            }
        }
        System.out.println(result.pop());
    }
    private static void Calculate(String s,Stack<Integer> result,int i){
        Character c = s.charAt(i);
        if (Character.isDigit(c)) {
            result.push(Integer.parseInt(c + ""));
        } else {
            Integer left=0;
            Integer right=0;
            if(isPost){
                right=result.pop();
                left=result.pop();
            }
            else{
                left=result.pop();
                right=result.pop();
            }
            Integer temp = 0;
            switch (c) {
                case '+':
                    temp = left + right;
                    break;
                case '-':
                    temp = left - right;
                    break;
                case '*':
                    temp = left * right;
                    break;
                case '/':
                    temp = left / right;
                    break;
            }
            result.push(temp);
        }
    }
}