package linear;

import java.util.HashMap;
import java.util.Map;

/**
 * 表列名称
 */
public class 表列名称 {
    private static Map<Character,Integer> map=new HashMap<>();
    public static void main(String[] args) {
        for(int i=1;i<27;i++){
            map.put((char)('A'+i-1),i);
        }
        System.out.println(convertToTitle("ZY"));
    }
    public static int convertToTitle(String columnTitle) {
        return convertToTitle(columnTitle,0,columnTitle.length()-1);
    }            //   n-1
    // 思路 根据公式    E (a)下标i*26^i 可以转换为递归的思路来求解
                 //   i=0
    private static int convertToTitle(String columnTitle,int i,int j) {
        if(i==columnTitle.length()-1){
            Integer index=map.get(columnTitle.charAt(i));
            int times=(int)Math.pow(26,j);
            int result=times*index;
            return result;
        }
        int result=convertToTitle(columnTitle,i+1,j-1);
        Integer index=map.get(columnTitle.charAt(i));
        int times=(int)Math.pow(26,j);
        result+=index*times;
        return result;
    }
}