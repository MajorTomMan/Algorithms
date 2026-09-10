package com.majortom.algorithms.structure.tree;

import com.majortom.algorithms.structure.tree.AVLTreeNode;

/** Explicit structure contract for AVL-balanced search trees. */
public interface AvlTreeStructure<T extends Comparable<? super T>> extends SearchTreeStructure<T> {
    @Override
    AVLTreeNode<T> root();
}
