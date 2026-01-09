package com.majortom.algorithms.app.leetcode.algo.sort;

import javax.swing.SwingUtilities;

import com.majortom.algorithms.core.sort.impl.InsertionSort;
import com.majortom.algorithms.core.sort.impl.ShellSort;
import com.majortom.algorithms.core.visualization.impl.frame.SortFrame;
import com.majortom.algorithms.utils.AlgorithmsUtils;

public class 排序测试 {

    public static void main(String[] args) {
        Integer[] rawData = AlgorithmsUtils.randomArray(100, 12);
        int[] dataForInsertion = AlgorithmsUtils.toPrimitive(rawData);

        int[] dataForShell = dataForInsertion.clone();
        SortFrame.launch(new InsertionSort(), dataForInsertion);
        //SortFrame.launch(new ShellSort(), dataForShell);
    }
}