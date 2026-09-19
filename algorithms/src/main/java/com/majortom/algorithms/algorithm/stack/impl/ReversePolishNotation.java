package com.majortom.algorithms.algorithm.stack.impl;

import com.majortom.algorithms.core.annotation.Algorithm;
import com.majortom.algorithms.core.annotation.AlgorithmEntry;
import com.majortom.algorithms.core.logging.Log;
import com.majortom.algorithms.structure.linked.StackStructure;

@Algorithm(id = "reverse-polish-expression", name = "逆波兰式", type = String.class, structure = StackStructure.class)
public class ReversePolishNotation {
  private String expression = "((8 + 2) * (9 - 3) / (4 + 2) + 7 * (6 - 2)) * (5 + 3) - (9 * 4 - 6) / 3";

  @AlgorithmEntry
  public void run(StackStructure<String> stack) {
    Log.d("expression:" + expression);
    Log.d("stack size:" + stack.size());
    if (!stack.isEmpty()) {
      stack.clear();
    }
    String rpn = toRPN(stack, expression);
    Log.d("expression:" + rpn);
    evaluateRPN(stack, rpn);
  }

  public String toRPN(StackStructure<String> stack, String expression) {
    StringBuilder result = new StringBuilder();

    for (char c : expression.toCharArray()) {
      if (c == ' ')
        continue;

      if (Character.isDigit(c)) {
        result.append(c).append(' ');
      } else if (c == '(') {
        stack.push("(");
      } else if (c == ')') {
        String op;
        while (!(op = stack.pop()).equals("(")) {
          result.append(op).append(' ');
        }
      } else {
        while (!stack.isEmpty()) {
          String op = stack.pop();
          if (op.equals("(") || priority(op.charAt(0)) < priority(c)) {
            stack.push(op);
            break;
          }
          result.append(op).append(' ');
        }
        stack.push(c + "");
      }
    }

    while (!stack.isEmpty()) {
      result.append(stack.pop()).append(' ');
    }

    return result.toString().trim();
  }

  private int priority(char c) {
    return c == '*' || c == '/' ? 2 : 1;
  }

  public void evaluateRPN(StackStructure<String> stack, String expression) {
    for (char c : expression.toCharArray()) {
      if (c == ' ') {
        continue;
      }
      if (Character.isDigit(c)) {
        stack.push(c + "");
      } else {
        String b = stack.pop();
        String a = stack.pop();
        String result = calcate(a, b, String.valueOf(c));
        if (result != "") {
          stack.push(result);
        }
      }
    }
  }

  private String calcate(String a, String b, String operator) {
    if (operator.equals("+")) {
      return Integer.parseInt(a) + Integer.parseInt(b) + "";
    } else if (operator.equals("-")) {
      return Integer.parseInt(a) - Integer.parseInt(b) + "";
    } else if (operator.equals("*")) {
      return Integer.parseInt(a) * Integer.parseInt(b) + "";
    } else if (operator.equals("/")) {
      return Integer.parseInt(a) / Integer.parseInt(b) + "";
    }
    return "";
  }

}
