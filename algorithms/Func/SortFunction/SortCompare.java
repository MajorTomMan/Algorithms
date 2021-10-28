package Func.SortFunction;

import Func.Random.StdRandom;
import Sort.Structure.Heap;
import Sort.Structure.Insertion;
import Sort.Structure.Merge;
import Sort.Structure.Quick;
import Sort.Structure.Selection;
import Sort.Structure.Shell;

public class SortCompare{
    public static double time(String alg,Comparable[] a){
        Stopwatch timer =new Stopwatch();
        if(alg.equals("Insertion")){
            new Insertion().sort(a);
        }
        else if(alg.equals("Selection")){
            new Selection().sort(a);
        }
        else if(alg.equals("Shell")){
            new Shell().sort(a);
        }
        else if(alg.equals("Merge")){
            new Merge().sort(a);
        }
        else if(alg.equals("Quick")){
            new Quick().sort(a);
        }
        else if(alg.equals("Heap")){
            new Heap().sort(a);
        }
        return timer.elapsedTime();
    }
    public double timeRandomInput(String alg,int N,int T){
        double total=0.0;
        Double[] a=new Double[N];
        for(int t=0;t<T;t++){
            for(int i=0;i<N;i++){
                a[i]=StdRandom.uniform();
            }
            total+=time(alg,a);
        }
        return total;
    }
    public static void main(String[] args) {
        
        // String alg1=args[0];
        // String alg2=args[1];
        // int N=Integer.parseInt(args[2]);
        // int T=Integer.parseInt(args[3]);
        // double t1=timeRandomInput(alg1, N, T);
        // double t2=timeRandomInput(alg2, N, T);
        // StdOut.printf("For %d random Doubles\n   %s is",N,alg1);
        // StdOut.printf(" %.1f times faster than %s\n ",t2/t1,alg2);
    }
}
