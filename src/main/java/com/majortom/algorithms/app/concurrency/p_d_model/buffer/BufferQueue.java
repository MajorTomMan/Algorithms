package com.majortom.algorithms.app.concurrency.p_d_model.buffer;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.majortom.algorithms.app.concurrency.p_d_model.goods.Goods;

public class BufferQueue {
    private static final String TAG = "QUEUE";
    private BlockingQueue<Goods> queue = new ArrayBlockingQueue<>(10);

    public void put(Goods goods) {
        System.out.println(TAG + ":" + "1 goods was put in queue");
        System.out.println(TAG + ": goods info:" + goods);
        try {
            queue.put(goods);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public Goods take() {
        System.out.println(TAG + ":" + "1 goods was taken in queue");
        System.out.println(TAG + ":" + "queue size:" + queue.size());
        try {
            return queue.take();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
}