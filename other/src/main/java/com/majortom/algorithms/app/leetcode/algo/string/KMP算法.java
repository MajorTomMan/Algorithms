package com.majortom.algorithms.app.leetcode.algo.string;

import com.majortom.algorithms.library.string.KmpSearch;

public class KMP算法 {
    public static void main(String[] args) {
        com.majortom.algorithms.library.basic.String target =
                new com.majortom.algorithms.library.basic.String("AAAABAABAAAABAAABAAAA");
        System.out.println(new KmpSearch().search(target, "AB"));
    }
}
