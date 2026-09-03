package com.majortom.algorithms.library.structure;

import com.majortom.algorithms.library.basic.tree.BinaryTreeNode;

public interface SearchTreeStructure<T> extends BinaryTreeStructure<T> {
    BinaryTreeNode<T> find(T value);
    void insert(T value);
    boolean remove(T value);
}
