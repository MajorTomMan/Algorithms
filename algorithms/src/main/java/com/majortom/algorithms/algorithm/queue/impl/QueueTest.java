package com.majortom.algorithms.algorithm.queue.impl;

import com.majortom.algorithms.algorithm.queue.QueueAlgorithm;
import com.majortom.algorithms.core.annotation.Algorithm;
import com.majortom.algorithms.structure.linked.QueueStructure;

@Algorithm(id = "queue-test", name = "队列测试", module = "queue", type = Integer.class, structure = QueueStructure.class)
public class QueueTest implements QueueAlgorithm<Integer> {

    @Override
    public void execute(QueueStructure<Integer> queue) {

    }

}
