package linear;

import basic.structure.LinkedList;
import basic.structure.interfaces.Queue;
import utils.AlgorithmsUtils;

public class 队列 {
    public static void main(String[] args) {
        Queue<Integer> queue = new LinkedList<>();
        Integer[] sortedArray = AlgorithmsUtils.sortedArray(29, 28);
        queue.add(sortedArray);
        queue.foreach((v) -> {
            System.out.println(v);
        });
        System.out.println("-------------------------------------");
        queue.poll();
        queue.foreach((v) -> {
            System.out.println(v);
        });
    }
}
