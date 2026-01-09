package com.majortom.algorithms.app.leetcode.nonlinear;

import com.majortom.algorithms.core.tree.impl.AVLTree;
import com.majortom.algorithms.core.visualization.impl.frame.TreeFrame;
import com.majortom.algorithms.utils.AlgorithmsUtils;

public class 树算法测试 {
    public static void main(String[] args) {
        AVLTree<Integer> avl = new AVLTree<>();
        Integer[] data = AlgorithmsUtils.randomArray(10, 10);
        TreeFrame.launch(avl, data);
    }
}