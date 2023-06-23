package linear;

public class 十进制转十六进制 {
    public static void main(String[] args) {
        transform("205.188.146.23");
    }
    public static void transform(String dec){
        String ip="";
        String[] decs=dec.split("\\.");
        for(String decimal:decs){
            Integer decimal_num=Integer.parseInt(decimal);
            if(decimal_num<16){
                ip+="0"+Integer.toHexString(decimal_num);
            }else{
                ip+=Integer.toHexString(decimal_num);
            }
        }
        System.out.println("0x"+ip);
    }
}
