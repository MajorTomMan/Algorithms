package com.majortom.algorithms.app.concurrency.patterns.pc;

import java.util.Random;


public class Producer implements Runnable {
    private String TAG = "";
    private BufferQueue buffer;
    private Random random = new Random();

    public Producer(BufferQueue buffer, String tag) {
        this.buffer = buffer;
        this.TAG = tag;
    }

    public void product() {
        System.out.println(TAG+":"+"1 goods was produce");
        Goods goods = new Goods();
        goods.setFrom(TAG);
        goods.setName("goods");
        goods.setPrice(random.nextInt(132) + 22);
        buffer.put(goods);
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub
        while ((true)) {
            product();
            try {
                Thread.sleep((random.nextInt(3) + 1) * 1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
