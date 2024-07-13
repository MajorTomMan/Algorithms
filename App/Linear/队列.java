package linear;




import basic.structure.LinkedList;
import basic.structure.interfaces.Queue;

public class 队列 {
    public static void main(String[] args) {
        int i=0;
        Queue<Integer> queue=new LinkedList<>();
        while(i!=6){
            queue.add(i);
            i++;
        }
        i=0;
        for (Integer data : queue) {
            System.out.println(data);
        }
    }
}
