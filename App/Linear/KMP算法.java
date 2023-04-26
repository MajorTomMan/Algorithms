
import basic.structure.KMP;

public class KMP算法 {
    public static void main(String[] args) {
        KMP kmp=new KMP("AAAABAABAAAABAAABAAAA", "AAAABAAA");
        kmp.useKMP();
    }
}
