package com.majortom.algorithms.structure.unionfind;

/*
 * 并查集（Disjoint Set Union）用于处理集合合并和连通性查询。
 * 每个元素初始属于独立的集合，通过 find() 查询根节点，
 * 通过 union() 合并两个集合。
 *
 * 两种优化：
 * 1. 路径压缩：缩短查询过程中经过的路径。
 * 2. 按秩合并：让秩较小的树挂到秩较大的树下，减小树高。
 */
public final class UnionFind {
  /*
   * 通过数组下标表示集合中的元素。
   * 例如 parent = [0, 1, 2, 3, 4, 5, 6]，
   * 初始时每个元素的父节点都是自身，因此每个元素都是一个单独集合的根节点。
   *
   * parent[i] 保存元素 i 的父节点下标。
   * rank[i] 表示以 i 为根的树的秩（树高的一个上界），不是集合元素数量。
   */
  private final int[] parent;
  private final int[] rank;

  public UnionFind(int size) {
    if (size < 0) {
      throw new IllegalArgumentException("size must not be negative");
    }
    parent = new int[size];
    rank = new int[size];
    /* 先根据给定的大小，将每个元素初始化为只包含自身的独立集合。 */
    for (int index = 0; index < size; index++) {
      parent[index] = index;
    }
  }

  /*
   * 实现并查集中的查询操作：
   * 在初始化时，每个集合只有一个根节点，根节点的父节点就是自身。
   * 如果 parent[value] != value，说明当前元素不是根节点，
   * 需要继续沿着父节点向上查找，直到找到集合的根。
   *
   * 找到根节点后，再沿原路径遍历一次，把经过的节点直接指向根节点。
   * 这就是路径压缩，可以减少后续查询所需的遍历次数。
   */
  public int find(int value) {
    int root = value;
    while (parent[root] != root) {
      root = parent[root];
    }
    /* 使用路径压缩来减少后续查找的时间复杂度。 */
    while (parent[value] != value) {
      int next = parent[value];
      parent[value] = root;
      value = next;
    }
    return root;
  }

  /*
   * 对集合节点进行合并，需要先分别找到两个元素所在集合的根节点。
   * 如果根节点相同，说明两个元素已经属于同一个集合，不需要再次合并。
   *
   * 如果根节点不同，则采用按秩合并：
   * - 左边的秩较小，就把左边的根挂到右边的根下面；
   * - 右边的秩较小，就把右边的根挂到左边的根下面；
   * - 两边的秩相同，可以任选一个作为新根，此时才需要将新根的秩加一。
   *
   * 注意：rank 表示树高的上界，并不表示集合的元素个数。
   *
   * @return 本次操作是否真正合并了两个原本不同的集合
   */
  public boolean union(int left, int right) {
    int leftRoot = find(left);
    int rightRoot = find(right);
    /* 先判断两个节点是否已经属于同一个集合。 */
    if (leftRoot == rightRoot) {
      return false;
    }
    if (rank[leftRoot] < rank[rightRoot]) {
      parent[leftRoot] = rightRoot;
    } else if (rank[leftRoot] > rank[rightRoot]) {
      parent[rightRoot] = leftRoot;
    } else {
      parent[rightRoot] = leftRoot;
      /* 只有两棵树的秩相同时，合并后的新根才需要增加秩。 */
      rank[leftRoot]++;
    }
    return true;
  }

  /**
   * 判断两个节点是否连通（是否属于同一个集合）。
   *
   * @param left 左侧节点
   * @param right 右侧节点
   * @return 连通返回 true，否则返回 false
   */
  public boolean connected(int left, int right) {
    return find(left) == find(right);
  }
}
