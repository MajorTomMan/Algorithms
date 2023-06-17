package Linear;


public class 颠倒二进制位 {
    public static void main(String[] args) {

        System.out.println(reverseBits(43261596));
    }
    public static int reverseBits(int n) {
        int res=0;
        for(int i=0;i<32;i++){
            res = (res << 1) | (n & 1);
            n>>=1;
        }
        return res;
    }
}
