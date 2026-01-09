package com.majortom.algorithms.app.leetcode.algo.math;


public class 十六进制转十进制 {
    private static String[] data;
    private static Integer[] hexs;
    public static void main(String[] args) {
        transform("8002c2f2");
    }
    public static void transform(String hex){
        data=new String[hex.length()/2];
        hexs=new Integer[hex.length()/2];
        String str="";
        int k=0;
        for(int i=0,j=1;i<hex.length();i++,j++){
            if(j%2==0){
                data[k]=str;
                k++;
                str="";
            }else{
                str+=hex.charAt(i)+"";
                str+=hex.charAt(j)+"";
            }
        }
        k=0;
        for (String temp : data) {
            Integer t=Integer.parseInt(temp,16);
            hexs[k]=t;
            k++;
        }
        String ip="";
        for(Integer Hex:hexs){
            if(Hex==hexs[hexs.length-1]){
                ip+=String.valueOf(Hex);
            }
            else{
                ip+=String.valueOf(Hex)+".";
            }
        }
        System.out.println(ip);
    }
}
