public class Test {
    public static void main(String[] args) {
        Test_2();
    }

    public void Test_1() {
        int f = 0;
        int g = 1;
        for (int i = 0; i <= 15; i++) {
            System.out.println("当i等于:" + i + "时");
            System.out.println("运算前f为:" + f);
            f = f + g;
            g = f - g;
            System.out.println("运算后f为:" + f);
            System.out.println("运算后g为:" + g);
            System.out.println("\n");
        }
    }

    public static void Test_2() {
        int sum=0;
        for(int i=1;i<1000;i++){
            for(int j=0;j<i;j++){
                sum++;
            }
        }
        System.out.println(sum);
    }
}
