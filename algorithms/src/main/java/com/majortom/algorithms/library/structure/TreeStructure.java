package com.majortom.algorithms.library.structure;

import com.majortom.algorithms.library.basic.tree.TreeNode;

public interface TreeStructure<T> {
    int size();
    boolean isEmpty();
    TreeNode<T> find(T value);
    void insert(T value);
    boolean remove(T value);
    TreeNode<T> raw();
}
