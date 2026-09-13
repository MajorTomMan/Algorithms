package com.majortom.algorithms.algorithm.queue;

import com.majortom.algorithms.structure.linked.QueueStructure;

public interface QueueAlgorithm<T> {
    void execute(QueueStructure<T> queue);
}