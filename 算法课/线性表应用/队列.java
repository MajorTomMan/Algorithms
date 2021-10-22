package 线性表应用;

import 基本.Structure.Queue;

public class 队列 {
    public static void main(String[] args) {
        int i=0;
        Queue<Integer> queue=new Queue<Integer>();
        while(i!=6){
            queue.enqueue(i);
            i++;
        }
        i=0;
        for (Integer data : queue) {
            System.out.println(data);
        }
    }
}
