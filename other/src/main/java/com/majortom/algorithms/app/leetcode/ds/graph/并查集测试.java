package com.majortom.algorithms.app.leetcode.ds.graph;

import com.majortom.algorithms.library.basic.UnionFind;

public class 并查集测试 {
    public static void main(String[] args) {
        Integer[][] edges = {
                { 0, 4 },
                { 1, 1 }, { 1, 5 },
                { 2, 2 }, { 2, 3 }, { 2, 5 },
                { 3, 2 }, { 3, 3 },
                { 4, 0 }, { 4, 4 },
                { 5, 1 }, { 5, 2 }, { 5, 5 },
        };
        UnionFind unionFind = new UnionFind(6);
        boolean hasCycle = false;
        for (Integer[] edge : edges) {
            int from = edge[0];
            int to = edge[1];
            if (unionFind.connected(from, to)) {
                hasCycle = true;
            } else {
                unionFind.union(from, to);
            }
        }
        System.out.println(hasCycle);
        System.out.println(unionFind.connected(1, 5));
    }
}
