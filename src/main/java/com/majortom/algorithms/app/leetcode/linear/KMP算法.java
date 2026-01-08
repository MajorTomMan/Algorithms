package com.majortom.algorithms.app.leetcode.linear;


import com.majortom.algorithms.core.basic.KMP;

public class KMP算法 {
    public static void main(String[] args) {
        KMP kmp=new KMP("AAAABAABAAAABAAABAAAA", "AB");
        kmp.useKMP();
    }
}
