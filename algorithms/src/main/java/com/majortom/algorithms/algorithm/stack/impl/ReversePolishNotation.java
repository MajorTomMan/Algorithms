package com.majortom.algorithms.algorithm.stack.impl;

import com.majortom.algorithms.core.annotation.Algorithm;
import com.majortom.algorithms.core.annotation.AlgorithmEntry;
import com.majortom.algorithms.core.logging.Log;
import com.majortom.algorithms.structure.linked.StackStructure;

@Algorithm(id = "reverse-polish-expression", name = "逆波兰式", type = String.class, structure = StackStructure.class)
public class ReversePolishNotation {
  private String expression = "3 4 + 5 *";

  @AlgorithmEntry
  public void evaluateRPN(StackStructure<String> stack) {
    Log.d("stack size:" + stack.size());
    if (stack.isEmpty()) {
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
  }

  private String calcate(String a, String b, String operator) {
    if (operator == "+") {
      return Integer.parseInt(a) + Integer.parseInt(b) + "";
    } else if (operator == "-") {
      return Integer.parseInt(a) - Integer.parseInt(b) + "";
    } else if (operator == "*") {
      return Integer.parseInt(a) * Integer.parseInt(b) + "";
    } else if (operator == "/") {
      return Integer.parseInt(a) / Integer.parseInt(b) + "";
    }
    return "";
  }

}
