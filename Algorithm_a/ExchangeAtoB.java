
import java.util.Random;


public class ExchangeAtoB {
    public static void main(String[] args) {
        int[][] ary1=new int[3][3];
        int[][] ary2=new int[3][3];
        Add(ary1,ary2,3,3);
        System.out.println("这是交换前的矩阵A和B");
        Show(ary1,ary2,3,3);
        System.out.println("开始进行交换");
        Exchange(ary1,ary2,3,3);
        System.out.println("这是交换后的矩阵A和B");
        Show(ary1,ary2,3,3);
    }
    public static void Exchange(int[][] ary1,int[][] ary2,int i,int j){
        int[] temp1=new int[i];
        int[] temp2=new int[i];
        for(int z=0;z<i;z++){
            for(int k=0;k<i;k++){
                temp1[k]=ary1[z][k];
            }
            for(int k=0;k<i;k++){
                temp2[k]=ary2[k][z];
            }
            for(int k=0;k<i;k++){
                ary1[z][k]=temp2[k];
                ary2[k][z]=temp1[k];
            }
        }
    }
    public static void Show(int[][] ary1,int[][] ary2,int i,int j){
        for(int z=0;z<i;z++){
            for(int k=0;k<j;k++){
                System.out.print(ary1[z][k]+" ");
            }
            System.out.print("|||||||||");
            for(int k=0;k<j;k++){
                System.out.print(ary2[z][k]+" ");
            }
            System.out.println();
        }
    }
    public static void Add(int[][] ary1,int[][] ary2,int i,int j){
        Random random=new Random(1);
        int temp;
        for(int z=0;z<i;z++){
            for(int k=0;k<j;k++){
                temp=random.nextInt(100);
                ary1[z][k]=temp;
                temp=random.nextInt(100);
                ary2[z][k]=temp;
            }
        }
    }
}