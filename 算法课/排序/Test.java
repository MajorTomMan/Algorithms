package 排序;


public class Test{
    public static void main(String[] args) {
        String str="s o r t e x a m p l e";
        new Selection().sort(str.split(" "));
        new Insertion().sort(str.split(" "));
        new Shell().sort(str.split(" "));
        new Merge().sort(str.split(" "));
    }
}
