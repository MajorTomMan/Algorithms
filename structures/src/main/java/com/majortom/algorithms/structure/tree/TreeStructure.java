package com.majortom.algorithms.structure.tree;

import com.majortom.algorithms.structure.tree.TreeNode;

public interface TreeStructure<T> {
    int size();

    default boolean isEmpty() {
        return size() == 0;
    }

    TreeNode<T> root();
}
