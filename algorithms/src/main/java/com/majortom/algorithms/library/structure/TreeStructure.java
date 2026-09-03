package com.majortom.algorithms.library.structure;

import com.majortom.algorithms.library.basic.tree.TreeNode;

public interface TreeStructure<T> {
    int size();

    default boolean isEmpty() {
        return size() == 0;
    }

    TreeNode<T> root();
}
