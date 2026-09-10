package com.majortom.algorithms.structure.tree;

import com.majortom.algorithms.structure.tree.BinaryTreeNode;

public interface SearchTreeStructure<T> extends BinaryTreeStructure<T> {
    BinaryTreeNode<T> find(T value);
    void insert(T value);
    boolean remove(T value);
}
