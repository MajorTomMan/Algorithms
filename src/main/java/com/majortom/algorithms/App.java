package com.majortom.algorithms;

import com.majortom.algorithms.core.graph.BaseGraph;
import com.majortom.algorithms.core.graph.algorithms.BFSAlgorithms;
import com.majortom.algorithms.core.graph.impl.UndirectedGraph;
import com.majortom.algorithms.core.sort.impl.InsertionSort;
import com.majortom.algorithms.core.tree.impl.AVLTree;
import com.majortom.algorithms.utils.AlgorithmLab;
import com.majortom.algorithms.utils.AlgorithmsUtils;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) {
        AVLTree<Integer> avl = new AVLTree<>();
        Integer[] data = AlgorithmsUtils.randomArray(10, 100);

        // 启动树实验室，展示 AVL 平衡过程
        AlgorithmLab.showTree(avl, data);
    }
}
