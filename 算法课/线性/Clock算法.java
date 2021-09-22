package 线性;

import Structure.LoopQueue;
import Structure.Node.Clocknode;

public class Clock算法{
    public static void main(String[] args) {
        int i=0;
        LoopQueue loopQueue=new LoopQueue(5);
        while(i!=5){
            Clocknode clocknode=new Clocknode("内存"+i,(byte)1);
            loopQueue.enqueue(clocknode);
            i++;
        }
        System.out.println(loopQueue.toString());
        scanle(loopQueue);
        System.out.println(loopQueue.toString());
        scanle(loopQueue);
        System.out.println(loopQueue.toString());
    }
    private static void scanle(LoopQueue lq){
        int i=0;
        Clocknode ck=new Clocknode("",(byte)0);
        while(true){
            ck=lq.dequeue();
            if(ck.getUsed()==(byte)1){
                ck.setUsed((byte)0);
                lq.enqueue(ck);
            }
            else if(ck.getUsed()==(byte)0){
                ck.setIntruduce("内存"+i+"交换至外存");
                ck.setUsed((byte)1);
                lq.enqueue(ck);
                return;
            }
            if(i==lq.getSize()){
                i=0;
            }
            i++;
        }
    }
}
