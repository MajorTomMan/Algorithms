import java.util.HashMap;
import java.util.Map;

public class 罗马数字 {
    private static Map<Character,Integer> map=new HashMap<>();
    public static void main(String[] args) {
        System.out.println(romanToInt("MCMXCIV"));
    }
    public static int romanToInt(String s) {
        char[] data=s.toCharArray();
        int result=0;
        for(int i=0,j=i+1;i<data.length-1;i++){
            if(map.get(data[i])<map.get(data[i+1])){
                result-=map.get(data[i]);
            }
            else{
                result+=map.get(data[i]);
            }
        }
        result+=map.get(data[data.length-1]);
        return result;
    }
}
