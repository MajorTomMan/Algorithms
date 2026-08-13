package com.majortom.algorithms.app.leetcode.algo.string;

public class 字符串最长前缀 {
    public static void main(String[] args) {
        System.out.println(lcp("acctgttaac","accgttaa"));
    }
    // 暴力解法 时间复杂度(N^2)
    private static String lcp(String str_1, String str_2) {
        StringBuffer str=new StringBuffer();
        int length=Math.min(str_1.length(),str_2.length());
        for(int i=0;i<length;i++){
            if(str_1.charAt(i)!=str_2.charAt(i)){
                return new String(str);
            }
            str.append(str_1.charAt(i));
        }
        String s=new String(str);
        return s;
    }
}
