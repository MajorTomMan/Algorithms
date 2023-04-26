package func.drawing;

public class 函数值{
    public static void main(String[] args) {
        int N=100;
        double j=3.1415926;
        StdDraw.setXscale(0,N);
        StdDraw.setYscale(0,N*N);
        StdDraw.setPenRadius(.01);
        for(int i=-2;i<=N;i++){
           // StdDraw.point(i, i);
             StdDraw.point(i, i*i);
           //  StdDraw.point(i, i*Math.log(i));
            StdDraw.point(i,i*Math.tanh(j/3));
        }
    }
}
