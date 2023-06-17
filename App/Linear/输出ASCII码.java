package Linear;

/**
 * 输出ASCII码
 */
public class 输出ASCII码 {
    public static void main(String[] args) {
        int i=0;
        while(i++<=128){
            System.out.printf("数值:"+i+" ");
            System.out.println(Character.toChars(i));
        }
    }
}