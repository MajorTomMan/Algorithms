package com.majortom.algorithms;

import com.majortom.algorithms.core.sort.impl.InsertionSort;
import com.majortom.algorithms.utils.AlgorithmLab;
import com.majortom.algorithms.utils.AlgorithmsUtils;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) {
        Integer[] rawData = AlgorithmsUtils.randomArray(20, 100);
        int[] dataForInsertion = AlgorithmsUtils.toPrimitive(rawData);

        // 使用极简 API 启动排序实验室 [cite: 2026-01-10]
        AlgorithmLab.showSort(dataForInsertion, new InsertionSort());
    }
}
