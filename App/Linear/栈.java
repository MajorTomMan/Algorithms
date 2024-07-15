package linear;

import basic.structure.LinkedList;
import basic.structure.interfaces.Stack;
import utils.AlgorithmsUtils;

public class 栈 {
    public static void main(String[] args) {
        Integer[] sortedArray = AlgorithmsUtils.sortedArray(20, 19);
        Stack<Integer> stack = new LinkedList<>();
        stack.push(sortedArray);
        stack.foreach((v)->{
            System.out.println(v);
        });
        System.out.println(stack.pop());


    }
}