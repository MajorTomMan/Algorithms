package linear;



import character.substringsearch.KMP;

public class KMP算法测试 {
    public static void main(String[] args) {
        String pat="AACAA";
        String txt="AABRAACADABRAACAADABRA";
        KMP kmp=new KMP(pat);
        System.out.println("text: "+txt);
        int offset=kmp.search(txt);
        System.out.print("patt: ");
        for (int i = 0; i < offset; i++) {
            System.out.print(" ");
        }
        System.out.println(pat);
    }
}
