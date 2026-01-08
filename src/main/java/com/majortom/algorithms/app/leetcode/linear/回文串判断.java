package com.majortom.algorithms.app.leetcode.linear;




public class 回文串判断 {
    public static void main(String[] args) {
        System.out.println(isPalindrome("Marge, let's \"[went].\" I await {news} telegram."));
    }
    public static boolean isPalindrome(String s) {
        String ss=s.replaceAll(" ","");
        String[] temp=ss.split(
                               "\\,|\\:|\\.|\\@|\\!|\\#|\\$|\\%|\\^|\\&|\\*|\\("
                              +"|\\)|\\_|\\=|\\-|\\+|\\`|\\~|\\;|\\'|\"|\\[|\\]"
                              +"|\\{|\\}|\\|\\<|\\>|\\?|\\/"
                              );
        String str=String.join("",temp).toLowerCase();
        for(int i=0,j=str.length()-1;i<str.length()||j>0;i++,j--){
            if(str.charAt(i)!=str.charAt(j)){
                return false;
            }
        }
        return true;
    }
}
