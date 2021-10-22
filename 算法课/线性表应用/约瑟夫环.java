package 线性表应用;

import 基本.Structure.Cyclelist;

public class 约瑟夫环{
    public static void main(String[] args) {
        int i=0;
        Cyclelist<Integer> cyclelist=new Cyclelist<>();
        while(i!=6){
            cyclelist.Insert(i);
            i++;
        }
        while(cyclelist.getHead().next!=cyclelist.getHead()){
            cyclelist.Delete(3);
            cyclelist.Show(cyclelist.getHead());
        }
        System.out.println("约瑟夫环只剩一个元素");
    }
}

