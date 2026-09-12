package com.majortom.algorithms.structure.tree;

import java.util.List;

/** Explicit structure contract for AVL-balanced search trees. */
public interface AvlTreeStructure<T extends Comparable<? super T>> extends SearchTreeStructure<T> {
    /** Trusted bulk-load path. Values must already be strictly sorted and unique. */
    void initializeSorted(List<? extends T> sortedUniqueValues);

    @Override
    AVLTreeNode<T> root();
}
