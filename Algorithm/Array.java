public class Array {
    public static void main(String[] args) {
        array("*", " ", 3, 3);
    }

    public static void array(String T, String F, int i, int j) {
        String [][]ary=new String[i][j];
        for(int z=0;z<i;z++){
            ary[z][0]=T;
            for(int k=1;k<j;k++){
                ary[z][k]=F;
            }
        }
        for(int z=0;z<i;z++){
            for(int k=0;k<j;k++){
                System.out.print("|| 第"+z+"行第"+k+"个的数据为:"+ary[z][k]);
            }
            System.out.println();
        }    
    } 
}
