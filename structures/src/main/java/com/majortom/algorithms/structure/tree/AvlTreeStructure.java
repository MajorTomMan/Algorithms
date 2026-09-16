package com.majortom.algorithms.structure.tree;

import com.majortom.algorithms.core.annotation.Structure;
import com.majortom.algorithms.core.metadata.StructureModule;

import java.util.List;

/** Explicit structure contract for AVL-balanced search trees. */
@Structure(
        id = "avl-tree",
        name = "AVL Tree",
        module = StructureModule.TREE,
        implementation = AVLTree.class)
public interface AvlTreeStructure<T extends Comparable<? super T>> extends SearchTreeStructure<T> {
    /** Trusted bulk-load path. Values must already be strictly sorted and unique. */
    void initializeSorted(List<? extends T> sortedUniqueValues);

    @Override
    AVLTreeNode<T> root();
}
