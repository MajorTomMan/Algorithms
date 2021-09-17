public class factorial{
    public static void main(String[] args) {
        System.out.println(calculate_factorial(10));
    }

    private static int calculate_factorial(int n){
        if(n==1){
            return n;
        }
        return n*calculate_factorial(n-1);
    }
}
