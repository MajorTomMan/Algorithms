package NonLinear;

import character.triest.TST;
import character.triest.TrieST;

public class 单词查找树测试 {
    public static void main(String[] args) {
        int i=0;
        TrieST<Integer> trieST=new TrieST<>();
        TST<Integer> tST=new TST<>();
        String str="it was the best of times it was the worst of times";
        String strr="it was the age of wisdom it was the age of foolishness";
        String[] temp=str.split(" ");
        String[] temp_1=strr.split(" ");
        while(i!=temp.length){
            trieST.put(temp[i],i);
            tST.put(temp_1[i],i);
            i++;
        }
        for (String key :trieST.keys()) {
            System.out.print(key+" ");
        }
        System.out.println();
        System.out.println("-----------------------");
        for(String data:trieST.keysThatMatch("**")){ //通配符测试 通配符:*|.
            System.out.println(data);
        }
    }
}
