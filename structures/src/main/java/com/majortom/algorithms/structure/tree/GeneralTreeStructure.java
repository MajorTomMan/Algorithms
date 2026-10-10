package com.majortom.algorithms.structure.tree;

import com.majortom.algorithms.core.annotation.Structure;
import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.core.metadata.StructureModule;
import com.majortom.algorithms.core.snapshot.GeneralTreeSnapshot;

@Structure(id = StructureIds.TREE, name = "Tree", module = StructureModule.TREE, implementation = Tree.class)
public interface GeneralTreeStructure<T> extends TreeStructure<T> {
  @Override GeneralTreeNode<T> root();

  /** Replaces the complete ordered tree from an immutable snapshot. */
  void initialize(GeneralTreeSnapshot<T> snapshot);

  GeneralTreeNode<T> addRoot(T value);

  GeneralTreeNode<T> addChild(GeneralTreeNode<T> parent, T value);

  GeneralTreeNode<T> addChild(GeneralTreeNode<T> parent, int index, T value);

  GeneralTreeNode<T> addParent(GeneralTreeNode<T> node, T value);

  T set(GeneralTreeNode<T> node, T value);

  GeneralTreeNode<T> findById(long id);

  GeneralTreeNode<T> findFirstByValue(T value);

  boolean remove(GeneralTreeNode<T> node);

  void move(GeneralTreeNode<T> node, GeneralTreeNode<T> newParent);

  void move(GeneralTreeNode<T> node, GeneralTreeNode<T> newParent, int index);
}
