package com.majortom.algorithms.app.leetcode.linear;

public class 递归算最大公约数 {
    public static void main(String[] args) {
        System.out.println(HCF(91, 49));
    }

    public static int highestCommmonFactor(int x, int y) {
        if (y==0){
            return x;
        }
        if(x<0){
            return highestCommmonFactor(-x, y);
        }
        if(y<0){
            return highestCommmonFactor(x, -y);
        }
          return highestCommmonFactor(y, x%y);
    }

    public static int HCF(int x,int y){
        int temp=0;
        while(true){
            temp=y;
            y=x%y;
            x=temp;
            if(y==0){
                return x;
            }
        }
    }
    /*
    public static int HCF(int x,int y){
        int z=0;
        while(true){
            z=y;
            y=x%y;
            if(y==0){
                x=z;
                return x;
            }
            x=z;
        }
    }
    */
}
