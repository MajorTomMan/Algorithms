package com.majortom.algorithms.algorithm.tree.impl;

import com.majortom.algorithms.core.annotation.Algorithm;
import com.majortom.algorithms.core.annotation.AlgorithmEntry;
import com.majortom.algorithms.core.domain.observation.TreeObservationDomains;
import com.majortom.algorithms.core.logging.Log;
import com.majortom.algorithms.core.runtime.Observations;
import com.majortom.algorithms.structure.tree.AVLTreeNode;
import com.majortom.algorithms.structure.tree.AvlTreeStructure;
import com.majortom.algorithms.structure.tree.BinaryTreeNode;

@Algorithm(id = "avl-tree-dfs", name = "二叉树中序遍历", type = Integer.class, structure = AvlTreeStructure.class)
public class InorderTraversalAvlTree {
  @AlgorithmEntry
  public void execute(AvlTreeStructure<Integer> tree) {
    Log.d("tree root:" + tree.root());
    AVLTreeNode<Integer> root = tree.root();
    dfs(root, null);
  }

  private void dfs(BinaryTreeNode<Integer> root, String parentCallId) {
    if (root == null) {
      return;
    }

    String callId = "tree-node-" + root.getId();
    Observations.callEntered(callId, parentCallId, "inorder(" + root.getValue() + ")");
    try {
      if (root.getLeft() != null) {
        Observations.examined(TreeObservationDomains.NODE, root.getId(), root.getLeft().getId());
        dfs(root.getLeft(), callId);
      }

      Observations.visited(TreeObservationDomains.NODE, root.getId());
      Log.d("root value:" + root.getValue());

      if (root.getRight() != null) {
        Observations.examined(TreeObservationDomains.NODE, root.getId(), root.getRight().getId());
        dfs(root.getRight(), callId);
      }
    } finally {
      Observations.callReturned(callId, "visited");
    }
  }
}
