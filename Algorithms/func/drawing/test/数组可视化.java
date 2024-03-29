package func.drawing.test;


import java.util.Arrays;

import func.drawing.StdDraw;
import func.random.StdRandom;

public class 数组可视化 {
    public static void main(String[] args) {
        int N=50;
        double a[]=new double[N];
        for (int index = 0; index < N; index++) {
            double data;
            data=StdRandom.random();
            a[index]=data;
        }
        Arrays.sort(a);
        for (int index = 0; index < N; index++) {
            double x=1.0*index/N;
            double y=a[index]/2.0;
            double rw=0.5/N;
            double rh=a[index]/2.0;
            StdDraw.filledRectangle(x, y, rw,rh);
        }
    }
}
