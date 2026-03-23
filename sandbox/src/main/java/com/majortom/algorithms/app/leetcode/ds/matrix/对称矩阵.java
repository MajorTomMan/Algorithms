package com.majortom.algorithms.app.leetcode.ds.matrix;




public class 对称矩阵 {
    public static void main(String[] args) {
        Integer[][] data = { 
                        { 5, 3, 8, 6 }, 
						{ 3, 12, 9, 15 }, 
						{ 8, 9, 3, -1 }, 
						{ 6, 15, -1, 2 } 
        };
        int n=data.length;
        Integer[] result_one=new Integer[n * (n + 1) / 2 + 1];
        result_one=zip(data);
        for (int i : result_one) {
            System.out.print(i+" ");
        }
        System.out.println();
        int i=result_one.length;
        int level=result_one[n-1];
        Integer[][] result_two=new Integer[level][i];
        result_two=unzip(result_one);
        for (Integer[] js : result_two) {
            for(int g:js){
                System.out.print(g+" ");
            }
            System.out.println();
        }
    }

    public static Integer[] zip(Integer[][] array) {
        int n = array.length; // 获取对称矩阵的阶数
        Integer[] onearr = new Integer[n * (n + 1) / 2 + 1]; // 一维数组保存下三角元素1+2+3+..+n，再预留一个空间，保留原始数据的阶数
        for (int i = 0; i < n; i++) {
            for (int j = 0; j <= i; j++) {
                onearr[i * (i + 1) / 2 + j] = array[i][j];
            }
        }
        onearr[n * (n + 1) / 2] = n;
        return onearr;
    }
    public static Integer[][] unzip(Integer[] array){
        int len=array.length;
        int level=array[len-1];
        Integer[][] temp=new Integer[level][level];
        for(int i=0;i<level;i++){
            for(int j=0;j<level;j++){
                if(j<=i){
                    temp[i][j] = array[i * (i + 1) / 2 + j];
                }else{
                    temp[i][j] = array[j * (j + 1) / 2 + i];
                }
            }
        }
        return temp;
    }
}