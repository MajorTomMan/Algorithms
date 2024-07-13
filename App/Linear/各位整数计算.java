package linear;



import basic.structure.LinkedList;
import basic.structure.interfaces.Queue;

public class 各位整数计算 {
    public static void main(String[] args) {
        int data=7864;
        int counter=0;
        Queue<Integer> queue=new LinkedList<>();
        for(int result=0;data!=0;data=data/10){
            result=data%10;
            queue.add(result);
            counter++;
        }
        for (int result:queue) {
            System.out.print(result);
        }
        System.out.println();
        System.out.println("该数为"+counter+"位数");
    }
}
