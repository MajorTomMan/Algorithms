package com.majortom.algorithms.algorithm.tree.impl;

import com.majortom.algorithms.algorithm.tree.AvlTreeAlgorithm;
import com.majortom.algorithms.core.annotation.Algorithm;
import com.majortom.algorithms.structure.tree.AvlTreeStructure;

@Algorithm(id = "avl-tree-test", name = "二叉排序树测试", module = "tree", type = Integer.class, structure = AvlTreeStructure.class)
public class AvlTreeTest implements AvlTreeAlgorithm<Integer> {

    @Override
    public void execute(AvlTreeStructure<Integer> tree) {
    }

}
