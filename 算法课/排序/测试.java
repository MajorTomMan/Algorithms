package 排序;

import java.util.Scanner;

import Func.Input_Output.StdOut;
import Func.SortFunction.SortCompare;
import 排序.Structure.Heap;
import 排序.Structure.Insertion;
import 排序.Structure.Merge;
import 排序.Structure.Quick;
import 排序.Structure.Selection;
import 排序.Structure.Shell;


public class 测试{
    public static void main(String[] args) {
        String str="E A S Y Q U E S T I O N";
        String[] result=str.split(" ");
        new Selection().sort(result);
        show(result);
        new Insertion().sort(result);
        show(result);
        new Shell().sort(result);
        show(result);
        new Merge().sort(result);
        show(result);
        new Quick().sort(result);
        show(result);
        new Heap().sort(result);
        show(result);
        Comparable_a();
    }
    private static void Comparable_a() {
        System.out.println();
        System.out.print("请输入要比较的算法和次数(两两分割):");
        SortCompare sort=new SortCompare();
        Scanner scanner=new Scanner(System.in);
        String[] args=scanner.nextLine().split(" ");
        scanner.close();
        String alg1=args[0];
        String alg2=args[1];
        int N=Integer.parseInt(args[2]);
        int T=Integer.parseInt(args[3]);
        double t1=sort.timeRandomInput(alg1, N, T);
        double t2=sort.timeRandomInput(alg2, N, T);
        StdOut.printf("For %d random Doubles\n   %s is",N,alg1);
        StdOut.printf(" %.1f times faster than %s\n ",t2/t1,alg2);
    }
    private static void show(Comparable[] a){
        for(int i=0;i<a.length;i++){
            System.out.print(a[i]+" ");
        }
        System.out.println();
    }
}
