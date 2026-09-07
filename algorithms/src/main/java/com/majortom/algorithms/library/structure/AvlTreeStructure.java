package com.majortom.algorithms.library.structure;

import com.majortom.algorithms.library.basic.tree.AVLTreeNode;

/** Explicit structure contract for AVL-balanced search trees. */
public interface AvlTreeStructure<T extends Comparable<? super T>> extends SearchTreeStructure<T> {
    @Override
    AVLTreeNode<T> root();
}
