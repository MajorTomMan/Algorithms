package com.majortom.algorithms.structure.unionfind;

/** Disjoint-set union with path compression and union by rank. */
public final class UnionFind {
  private final int[] parent;
  private final int[] rank;

  public UnionFind(int size) {
    if (size < 0) {
      throw new IllegalArgumentException("size must not be negative");
    }
    parent = new int[size];
    rank = new int[size];
    for (int index = 0; index < size; index++) {
      parent[index] = index;
    }
  }

  public int find(int value) {
    int root = value;
    while (parent[root] != root) {
      root = parent[root];
    }
    while (parent[value] != value) {
      int next = parent[value];
      parent[value] = root;
      value = next;
    }
    return root;
  }

  /** Returns true only when the operation merges two previously separate sets. */
  public boolean union(int left, int right) {
    int leftRoot = find(left);
    int rightRoot = find(right);
    if (leftRoot == rightRoot) {
      return false;
    }
    if (rank[leftRoot] < rank[rightRoot]) {
      parent[leftRoot] = rightRoot;
    } else if (rank[leftRoot] > rank[rightRoot]) {
      parent[rightRoot] = leftRoot;
    } else {
      parent[rightRoot] = leftRoot;
      rank[leftRoot]++;
    }
    return true;
  }

  public boolean connected(int left, int right) {
    return find(left) == find(right);
  }
}
