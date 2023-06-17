package Linear;

import java.util.Scanner;

public class 数列的前M项和 {
    public static void main(String[] args) {
        Scanner s=new Scanner(System.in);
        Double n=s.nextDouble();
        Integer m=s.nextInt();
        System.out.printf("%.2f",Serials(n,m));
        s.close();
    }
    public static Double Serials(Double n,int m){
        if(m==1){
            return n;
        }
        return n+Serials(Math.sqrt(n),--m);
    }
}
