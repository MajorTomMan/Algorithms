package linear;


public class 暴力字符串匹配 {
    private static String goal = "aaaabbbbaaabbbbcccabcdasdaasdasdasda";
    private static String target = "da";
    private static int index = 0;
    private static int search(int start,int end){
        int i,j=start;
        StringBuilder temp=new StringBuilder();
        while (start!=end){
            temp.append(goal.charAt(start++));
        }
        for (i = 0; i < target.length(); i++) {
            if(target.charAt(i)!=temp.charAt(i)){
                return -1;
            }
        }
        return j;
    }
    public static void main(String[] args) {
        int j=0,moving=0;
        for (int i = 0; i < goal.length(); i++) {
            if (target.charAt(0) != goal.charAt(i)) {
                continue;
            }
            moving=i+target.length();
            if(moving>goal.length()){
                break;
            }
            j=search(i,moving);
            if(j!=-1&& j!=0){
                index=j;
            }
        }
        System.out.println("index is " + index);
        System.out.println(goal);
        for (int i = 0; i < index; i++) {
            System.out.print(" ");
        }
        System.out.println(target);
    }
}
