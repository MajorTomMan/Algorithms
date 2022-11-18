package Character.SubStringSearch;

public class KMP { //利用有限状态自动机的KMP算法
    private final int R;       // the radix
    private final int m;       // length of pattern
    private int[][] dfa;       //有限状态自动机实现
    public KMP(String pat){ //由模式字符串构建dfa
        this.R=256;
        this.m=pat.length();
        int R=256;
        dfa=new int[R][m];
        dfa[pat.charAt(0)][0]=1;
        for (int x = 0,j = 1; j < m; j++) { //计算dfa[][j]
            for (int c = 0; c < R; c++) {
                dfa[c][j]=dfa[c][x]; //复制匹配失败情况下的值
            }
            dfa[pat.charAt(j)][j]=j+1; //设置匹配成功情况下的值
            x=dfa[pat.charAt(j)][x]; //更新重启状态
        }
    }
    public int search(String txt){
        int n = txt.length();
        int i, j;
        for (i = 0, j = 0; i < n && j < m; i++) {
            j = dfa[txt.charAt(i)][j];
        }
        if (j == m) return i - m;    //找到
        return n;                    //没找到
    }
}


