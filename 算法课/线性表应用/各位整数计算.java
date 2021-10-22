package 线性表应用;

import 基本.Structure.Queue;

public class 各位整数计算 {
    public static void main(String[] args) {
        int data=7864;
        int counter=0;
        Queue<Integer> queue=new Queue<>();
        for(int result=0;data!=0;data=data/10){
            result=data%10;
            queue.enqueue(result);
            counter++;
        }
        for (int result:queue) {
            System.out.print(result);
        }
        System.out.println();
        System.out.println("该数为"+counter+"位数");
    }
}
