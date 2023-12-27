/*
 * @Author: MajorTomMan 765719516@qq.com
 * @Date: 2023-06-25 21:03:05
 * @LastEditors: MajorTomMan 765719516@qq.com
 * @LastEditTime: 2023-10-08 22:35:49
 * @FilePath: \ALG\app\nonlinear\目录创建系统.java
 * @Description: 这是默认设置,请设置`customMade`, 打开koroFileHeader查看配置 进行设置: https://github.com/OBKoro1/koro1FileHeader/wiki/%E9%85%8D%E7%BD%AE
 */
package nonlinear;

import java.util.Scanner;


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
