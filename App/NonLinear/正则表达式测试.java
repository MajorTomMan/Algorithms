package NonLinear;


import Character.RegularExpressions.NFA;

public class 正则表达式测试 {
    public static void main(String[] args){
        String regexp ="(a|(bc)*d)*";
        String txt="abcbcd";
        NFA nfa=new NFA(regexp);
        System.out.println(nfa.recognizes(txt));
    }
}
