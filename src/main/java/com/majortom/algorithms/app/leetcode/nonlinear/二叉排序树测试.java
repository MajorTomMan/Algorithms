package com.majortom.algorithms.app.leetcode.nonlinear;

import javax.swing.SwingUtilities;

import com.majortom.algorithms.app.visualization.impl.frame.TreeFrame;
import com.majortom.algorithms.core.tree.impl.AVLTree;
import com.majortom.algorithms.utils.AlgorithmsUtils;

public class 二叉排序树测试 {
    public static void main(String[] args) {
        AVLTree<Integer> avl = new AVLTree<>();
        Integer[] randomArray = AlgorithmsUtils.randomArray(10, 10);
        for (Integer val : randomArray) {
            avl.put(val);
        }
        // 2. 将内核塞进可视化骨架
        SwingUtilities.invokeLater(() -> {
            TreeFrame frame = new TreeFrame(avl);
            frame.setVisible(true);
        });
    }
}