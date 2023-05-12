/*
 * @Date: 2023-04-26 16:51:29
 * @LastEditors: hujunhao hujunhao@rtczsz.com
 * @LastEditTime: 2023-05-08 09:01:08
 * @FilePath: /alg/App/Linear/RabinKarp测试.java
 */


import character.substringsearch.RabinKarp;

public class RabinKarp测试 {
    public static void main(String[] args) {
        String pat="AACAA";
        String txt="AABRAACADABRAACAADABRA";
        RabinKarp kmp=new RabinKarp(pat);
        System.out.println("text: "+txt);
        int offset=kmp.search(txt);
        System.out.print("patt: ");
        for (int i = 0; i < offset; i++) {
            System.out.print(" ");
        }
        System.out.println(pat);
    }
}
