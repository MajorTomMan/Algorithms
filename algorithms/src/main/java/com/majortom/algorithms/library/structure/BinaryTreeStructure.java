package com.majortom.algorithms.library.structure;

import com.majortom.algorithms.library.basic.tree.BinaryTreeNode;

public interface BinaryTreeStructure<T> extends TreeStructure<T> {
    @Override
    BinaryTreeNode<T> root();
}
