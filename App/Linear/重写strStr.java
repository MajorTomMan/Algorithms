package linear;


public class 重写strStr {
    public static void main(String[] args) {
    }
    public static int strStr(String haystack, String needle) {
        if(needle.length()==0){
            return 0;
        }
        int m=haystack.length(),n=needle.length();
        for (int i=0;i+n<m;i++) {
            Boolean flag=true;
            for (int j = 0; j < m; j++) {
                if (haystack.charAt(i + j) != needle.charAt(j)) {
                    flag = false;
                    break;
                }
            }
            if(flag){
                return i;
            }
        }
        return -1;
    }
}
