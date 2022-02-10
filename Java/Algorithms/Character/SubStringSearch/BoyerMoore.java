package Character.SubStringSearch;

public class BoyerMoore { //利用跳跃表来查找的BoyerMoore算法
    
    private int[] right;
    private String pat;
    public BoyerMoore(String pat) {
        this.pat=pat;
        int m=pat.length();
        int R=256;
        right=new int[R];
        for(int c=0;c<R;c++){
            right[c]=-1; //不包含在模式字符串中的字符的值为-1
        }
        for(int j=0;j<m;j++){ //包含在模式字符串中的字符表的值为
            right[pat.charAt(j)]=j; //它在其中出现的最右位置
        }
    }
    public int search(String txt){
        int n=txt.length();
        int m=pat.length();
        int skip;
        for (int i = 0; i < n-m; i+=skip) {
            skip=0;
            for (int j = m-1; j>=0; j--) {
                if(pat.charAt(j)!=txt.charAt(i+j)){
                    skip=j-right[txt.charAt(i+j)];
                    if(skip<1){
                        skip=1;
                    }
                    break;
                }
            }
            if(skip==0){
                return i;
            }
        }
        return n;
    }
}
