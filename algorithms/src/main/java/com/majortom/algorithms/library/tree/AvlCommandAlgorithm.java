package com.majortom.algorithms.library.tree;

import com.majortom.algorithms.library.structure.AvlTreeStructure;

import java.util.List;

/** Shared invocation contract for ordered command batches against an AVL structure. */
public interface AvlCommandAlgorithm<T extends Comparable<? super T>> extends AvlTreeAlgorithm<T> {
    void execute(AvlTreeStructure<T> tree, List<AvlCommand> commands);
}
