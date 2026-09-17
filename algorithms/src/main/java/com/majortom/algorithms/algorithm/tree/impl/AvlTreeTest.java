package com.majortom.algorithms.algorithm.tree.impl;

import com.majortom.algorithms.core.annotation.Algorithm;
import com.majortom.algorithms.core.annotation.AlgorithmEntry;
import com.majortom.algorithms.structure.tree.AVLTreeNode;
import com.majortom.algorithms.structure.tree.AvlTreeStructure;

@Algorithm(id = "avl-tree-test", name = "二叉排序树测试", type = Integer.class, structure = AvlTreeStructure.class)
public class AvlTreeTest {
  @AlgorithmEntry
  public void execute(AvlTreeStructure<Integer> tree) {
    AVLTreeNode<Integer> root = tree.root();
    
  }
}
