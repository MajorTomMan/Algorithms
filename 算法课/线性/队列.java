package 线性;

import Structure.Queue;

public class 队列 {
    public static void main(String[] args) {
        int i=0;
        Queue<Integer> queue=new Queue<Integer>();
        while(i!=6){
            queue.push(i);
            i++;
        }
        i=0;
        queue.show(queue.front);
        System.out.println("-------------");
        while(i!=3){
            System.out.println(queue.pop());
            i++;
        }

        while(i!=10){
            queue.push(i);
            i++;
        }
        System.out.println("-------------");
        queue.show(queue.front);
    }
}
