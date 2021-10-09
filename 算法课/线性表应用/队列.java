package 线性表应用;

import Structure.Queue;

public class 队列 {
    public static void main(String[] args) {
        int i=0;
        Queue<Integer> queue=new Queue<Integer>();
        while(i!=6){
            queue.enqueue(i);
            i++;
        }
        i=0;
        queue.show(queue.getFront());
        System.out.println("-------------");
        while(i!=3){
            System.out.println(queue.dequeue());
            i++;
        }

        while(i!=10){
            queue.enqueue(i);
            i++;
        }
        System.out.println("-------------");
        queue.show(queue.getFront());
    }
}
