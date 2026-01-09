package com.majortom.algorithms.app.concurrency.patterns.pc;


public class ProducerConsumerDemo {
    private static final BufferQueue buffer = new BufferQueue();

    public static void main(String[] args) {
        new Thread(new Producer(buffer, "Producer_1")).start();
        new Thread(new Producer(buffer, "Producer_2")).start();
        new Thread(new Producer(buffer, "Producer_3")).start();
        new Thread(new Producer(buffer, "Producer_4")).start();
        new Thread(new Consumer(buffer, "Consumer_1")).start();
        new Thread(new Consumer(buffer, "Consumer_2")).start();
        new Thread(new Consumer(buffer, "Consumer_3")).start();
        new Thread(new Consumer(buffer, "Consumer_4")).start();
    }
}
