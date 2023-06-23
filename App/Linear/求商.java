package linear;

public class 求商 {
    public static void main(String[] args) {
        System.out.println(divide(-2147483648,-1));
    }
    public static int divide(int a, int b) {
        if(a<0&&b>0){
            return -division(-a, b,0,b);
        }
        else if(b<0&&a>0){
            return -division(a,-b,0,-b);
        }
        else if(a<0&&b<0){
            return division(-a, -b,0,-b);
        }
        else{
            return division(a, b,0,b);
        }
    }
    public static int division(int a,int b,int count,int step){
        if(a<b){
            return count;
        }
        count=division(a, b+step,count,step);
        return ++count;
    }
}
