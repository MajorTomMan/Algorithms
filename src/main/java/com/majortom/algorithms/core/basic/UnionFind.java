package com.majortom.algorithms.core.basic;

/* 用于处理集合合并和查询 */
public class UnionFind {
    /*
     * 需要事先声明的是
     * 根据数组长度并通过数组索引构建其单例集合时,我们默认其索引中拥有的数据即为其本身的集合
     * 如 nodes =[0,1,2,3,4,5,6]
     * 在初期构造好单例集合后每个索引中的数据即代表着根节点
     */
    private int[] nodes;
    private int[] rank;

    public UnionFind(int size) {
        nodes = new int[size];
        rank = new int[size];
        /* 先根据给定的长度构建所有在集合中的元素的单例集合 */
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = i;
            rank[i] = 1;
        }
    }

    /* 实现并查集中的查询操作 */
    /*
     * 在构建单例集合时可以确保每个集合有且仅有一个根节点
     * 通过对当前索引处数据是否等于传入数据的判断
     * 我们可以唯一确定该节点是否是根节点
     * 若查找未命中,则继续通过该索引处的数据进行递归查找
     * 直到找到根节点为止
     */
    public int find(int x) {
        if (x == nodes[x]) {
            return x;
        }
        /* 使用路径压缩来减少时间复杂度 */
        return nodes[x] = find(nodes[x]);
    }

    /* 对集合节点的合并,需要借助于查找操作来进行合并 */
    /*
     * 先通过查找操作找到两个集合中的各自根节点
     * 然后通过判断根节点是否相同来判断是否是相同集合
     * 再根据并查集根节点指向自己的特点来进行合并
     */
    public void union(int x, int y) {
        /* 先判断是否是相同的集合 */
        if (find(x) != find(y)) {
            if (rank[find(x)] <= rank[find(y)]) {
                /* 将y集合合并到x集合中 */
                nodes[find(x)] = find(y);
            } else {
                nodes[find(y)] = find(x);
            }
            if (rank[find(x)] == rank[find(y)]) {
                /* 更新节点树的大小 */
                rank[find(x)]++;
            }
        }
    }

    /**
     * 判断两个节点是否连通（是否属于同一个集合）
     * 
     * @param p 节点 p
     * @param q 节点 q
     * @return 连通返回 true，否则返回 false
     */
    public boolean connected(int p, int q) {
        return find(p) == find(q);
    }

}
