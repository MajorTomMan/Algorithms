import basic.structure.CycleList;

/*
 * @Date: 2023-04-26 16:51:29
 * @LastEditors: hujunhao hujunhao@rtczsz.com
 * @LastEditTime: 2023-04-26 18:27:19
 * @FilePath: /alg/App/Linear/约瑟夫环.java
 */


public class 约瑟夫环{
    public static void main(String[] args) {
        int i=0;
        CycleList<Integer> cyclelist=new CycleList<>();
        while(i!=6){
            cyclelist.Insert(i);
            i++;
        }
        while(cyclelist.getHead().next!=cyclelist.getHead()){
            cyclelist.Delete(3);
            cyclelist.Show();
        }
        System.out.println("约瑟夫环只剩一个元素");
    }
}

