package com.majortom.algorithms.structure.tree;

import com.majortom.algorithms.structure.tree.BinaryTreeNode;

public interface BinaryTreeStructure<T> extends TreeStructure<T> {
    @Override
    BinaryTreeNode<T> root();
}
