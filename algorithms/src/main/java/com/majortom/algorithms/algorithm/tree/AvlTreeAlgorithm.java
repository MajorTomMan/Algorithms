package com.majortom.algorithms.algorithm.tree;

import com.majortom.algorithms.structure.tree.AvlTreeStructure;

/** Algorithms that specifically require AVL-tree semantics. */
public interface AvlTreeAlgorithm<T> extends TreeFamilyAlgorithm<T> {
    public void execute(AvlTreeStructure<Integer> tree);
    
}
