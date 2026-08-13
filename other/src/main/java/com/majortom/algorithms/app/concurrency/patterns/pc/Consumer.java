package com.majortom.algorithms.app.concurrency.patterns.pc;

import com.majortom.algorithms.app.concurrency.patterns.pc.BufferQueue;
import com.majortom.algorithms.app.concurrency.patterns.pc.Goods;

public class Consumer implements Runnable {
    private BufferQueue queue;
    private String TAG;

    public Consumer(BufferQueue queue, String tag) {
        this.queue = queue;
        this.TAG = tag;
    }

    public void consume() {
        Goods goods = queue.take();
        System.err.println(TAG + "->" + "consume 1 goods:" + goods.toString());
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub
        while (true) {
            consume();
        }
    }
}
