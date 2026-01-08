package com.majortom.algorithms.app.leetcode.nonlinear;

import java.util.Scanner;

import com.majortom.algorithms.core.basic.Tree;

public class 目录创建系统 {
    private static Scanner scanner;
    public static void main(String[] args) {
        String choose;
        while((choose=Menu())!="q"){
            switch(choose){
                case "1": 
                case "2":
                case "3":
                case "4":
                case "5":
            }
        }
    }
    public static String Menu(){
        System.out.println("目录创建系统:\n"
        +"1.显示所在目录\n"
        +"2.创建文件夹\n"
        +"3.进入目录\n"
        +"4.删除文件夹\n"
        +"q.退出\n"
        +"请输入序号:"
        );
        scanner=new Scanner(System.in);
        String choose=scanner.nextLine();
        return choose;
    }
    
}
