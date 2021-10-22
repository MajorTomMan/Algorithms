package 非线性应用;

import java.util.LinkedList;
import java.util.Scanner;

import 基本.Structure.Tree;
import 基本.Structure.Node.Data;
import 基本.Structure.Node.TRnode;

public class 目录创建系统 {
    static Scanner scanner;
    public static void main(String[] args) {
        Tree<String> tree=new Tree<>();
        String choose=Menu();
        while(Integer.parseInt(choose)!='q'){
            switch(Integer.parseInt(choose)){
                case 1:tree.Show(tree.getRoot());break;
                case 2:System.out.println("请输入要创建的文件夹名称:");
                       String chose=scanner.nextLine();
                       TRnode<String> node=new TRnode<>(new Data<String>(chose),null,new LinkedList<>());
                       tree.Insert(node,tree.getRoot());
                       System.out.println("文件夹创建成功!");
                       break;
                case 3:System.out.println("请输入要进入的目录名:");
                       String dirname=scanner.nextLine();
                       break;

                case 4:System.out.println("请输入要删除的文件夹名称:");
                       String dir=scanner.nextLine();
                       TRnode<String> d_node=new TRnode<>(new Data<String>(dir),null,new LinkedList<>());
                       tree.Delete(d_node,tree.getRoot());
                       break;
            }
            choose=Menu();
        }
        System.out.println("Bye!");
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
