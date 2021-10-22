package 基本.Structure;

import java.lang.reflect.Array;

import 基本.Structure.Interface.ILoopqueue;
import 基本.Structure.Node.Clocknode;

public class LoopQueue implements ILoopqueue{

    private Clocknode[] data; //存放数据的数组
    private int front; //队首指针
    private int tail;  //队尾（队列第一个没有元素的位置）指针
    private int size;  //队列内元素个数

    /**
     * 有参构造函数
     * @param capacity
     */
    public LoopQueue(int capacity){
        data =new Clocknode[capacity + 1];  //因为循环队列下会浪费一个元素空间
        front = 0;
        tail = 0;
        size = 0;
    }

    /**
     * 无参构造
     */
    public LoopQueue(){
        this(10);
    }

    public int getCapacity(){
        return data.length - 1;
    }


    @Override
    public int getSize() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return front == tail ;   //判断循环队列是否为空，即判断队尾和队首是否指向同一处
    }

    /**
     * 入队
     * @param e
     */
    @Override
    public void enqueue(Clocknode e) {
        if((tail + 1) % data.length == front){   //队列满的条件
            resize(getCapacity() * 2);  //扩容，容积为当前数据容积的两倍
        }

        data[tail] = e;
        tail = (tail + 1) % data.length;
        size ++;
    }

    private void resize(int newCapacity){ //扩容
        //先新建一个newCapacity的数组
        Clocknode[] newData = new Clocknode[newCapacity + 1 ];

        //将原循环队列中的元素放入新的队列中
        for(int i = 0; i < size; i++){
            //因为是循环队列，队列队首元素有可能不是原本队列的首元素
            newData[i] = data[(front + i) % data.length];
        }

        data = newData;
        front = 0;
        tail = size;  //size是循环队列中元素的个数，扩容操作不会影响size，因为没有元素入队或者出队
    }

    /**
     * 出队
     * @return
     */
    @Override
    public Clocknode dequeue() {
        if(isEmpty()){
            throw new IllegalArgumentException("Cannot not dequeue from empty queue");
        }

        Clocknode ret = data[front];
        data[front] = null;  //因为出队了，将出队后的元素置为Null
        front = (front + 1) % data.length;
        size --;
        //如果出队的元素很多，队列中有很多空间浪费，则进行缩容
        if(size == getCapacity() / 4 && getCapacity() / 2 != 0){
            resize(getCapacity() / 2);
        }
        return ret;
    }

    @Override
    public Clocknode getFront() {
        if(isEmpty()){
            throw new IllegalArgumentException("Queue is empty");
        }
        return data[front];
    }

    @Override
    public String toString(){
        StringBuffer rst = new StringBuffer();
        rst.append(String.format("Queue: size = %d, Array: capacity = %d\n", size, getCapacity()));

        rst.append("front [\n");
        for(int i = front; i != tail; i = (i + 1) % data.length ){
            rst.append("使用位:"+data[i].getUsed()+" "+data[i].getIntruduce());
            if((i + 1) % data.length != tail){
                rst.append(",\n");
            }
        }
        rst.append("\n] tail");
        return rst.toString();
    }
}