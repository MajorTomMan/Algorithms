public class Binary {
    public static void main(String[] args) {
        binary(Integer.parseInt("99"));
    }

    public static void binary(int i) {
        int read; 
        read=i%2;
        if (i>=2){
           binary(i/2);
        }
        System.out.print(read+"");
    }
}
