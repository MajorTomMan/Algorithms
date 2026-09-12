package com.majortom.algorithms.leetcode.algo.string;

import com.majortom.algorithms.algorithm.string.KmpSearch;

public class KMP算法 {
    public static void main(String[] args) {
        com.majortom.algorithms.structure.string.String target =
                new com.majortom.algorithms.structure.string.String("AAAABAABAAAABAAABAAAA");
        System.out.println(new KmpSearch().search(target, "AB"));
    }
}
