package com.majortom.algorithms.structure.tree;

import com.majortom.algorithms.core.annotation.Structure;
import com.majortom.algorithms.core.metadata.StructureModule;

@Structure(module = StructureModule.TREE)
public interface SearchTreeStructure<T> extends BinaryTreeStructure<T> {
    BinaryTreeNode<T> find(T value);

    void insert(T value);

    boolean remove(T value);
}
