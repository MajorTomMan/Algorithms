package com.majortom.algorithms.structure.tree;

import com.majortom.algorithms.core.annotation.Structure;
import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.core.metadata.StructureModule;
import com.majortom.algorithms.core.snapshot.BinaryTreeSnapshot;

/** Explicit structure contract for AVL-balanced search trees. */
@Structure(id = StructureIds.AVL_TREE, name = "AVL Tree", module = StructureModule.TREE,
    implementation = AVLTree.class)
public interface AvlTreeStructure<T extends Comparable<? super T>> extends SearchTreeStructure<T> {
  /** Restores a validated BST/AVL topology, retaining node IDs. */
  void initialize(BinaryTreeSnapshot<T> snapshot);

  @Override AVLTreeNode<T> root();
}
