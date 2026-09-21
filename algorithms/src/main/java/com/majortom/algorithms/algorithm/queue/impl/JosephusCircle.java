package com.majortom.algorithms.algorithm.queue.impl;

import java.util.List;

import com.majortom.algorithms.algorithm.support.AlgorithmsUtils;
import com.majortom.algorithms.core.annotation.Algorithm;
import com.majortom.algorithms.core.annotation.AlgorithmEntry;
import com.majortom.algorithms.core.logging.Log;
import com.majortom.algorithms.structure.linked.QueueStructure;

@Algorithm(id = "josephus-circle", name = "约瑟夫环", type = Integer.class, structure = QueueStructure.class)
public class JosephusCircle {
  private int k = 3;

  @AlgorithmEntry
  public void execute(QueueStructure<Integer> queue) {
    if (queue.isEmpty() || queue.size() == 1) {
      return;
    }

    int i = 0;

    Log.d("开始，队列人数: " + queue.size() + "，K: " + k);

    while (queue.size() != 1) {
      Integer unforture_person = queue.dequeue();

      Log.d("出队: " + unforture_person
          + "，i: " + i
          + "，i % k: " + (i % k)
          + "，剩余人数: " + queue.size());

      if (++i % k == 0) {
        Log.d("淘汰: " + unforture_person);
        Log.d("计数重置: i = " + i);
      } else {
        queue.enqueue(unforture_person);
        Log.d("重新入队: " + unforture_person);
      }
    }

    Log.d("执行结束，队列剩余人数: " + queue.size());
  }
}
