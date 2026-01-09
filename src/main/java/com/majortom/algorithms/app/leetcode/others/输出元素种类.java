package com.majortom.algorithms.app.leetcode.others;


import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class 输出元素种类 {
    private static Map<String, Integer> map = new HashMap<>();
    private static Map<String, Integer> temp = new HashMap<>();

    public static void main(String[] args) {
        judge("C6O6");
    }

    private static void judge(String element) {
        char[] data = element.toCharArray();
        int i = 0;
        while (i != data.length - 1) {
            i = splitArray(data, i);
        }
        System.out.println(outPut());
    }

    private static int splitArray(char[] data, int i) {
        String s = "";
        s += String.valueOf(data[i]);
        for (int j = i + 1; j != data.length; j++) {
            if (data[j] >= 'A' && data[j] <= 'Z') {
                if (map.containsKey(s)) {
                    map.put(s, map.get(s) + 1);
                }
                map.put(s, 1);
                i = j;
                break;
            } else if (data[j] >= 'a' && data[j] <= 'z') {
                s += Character.toString(data[j]);
            } else {
                if (data[j] != '(' && data[j] != ')' && Character.isDigit(data[j])) {
                    int num = Integer.parseInt(Character.toString(data[j]));
                    if (map.containsKey(s)) {
                        map.put(s, map.get(s) + num);
                    } else {
                        map.put(s, 0);
                        map.put(s, map.get(s) + num);
                    }
                    i = j;
                }
                else{
                    return handle(data,j);
                }
            }
        }
        return i;
    }

    private static int handle(char[] data, int index) {
        String k = "";
        int times = 0;
        if (data[index] == '(') {
            for (int i = index + 1; i != data.length; i++) {
                if (data[i] == ')') {
                    times = Integer.parseInt(Character.toString(data[i + 1]));
                    for (Entry<String, Integer> t : temp.entrySet()) {
                        t.setValue(t.getValue() * times);
                    }
                    for (Entry<String, Integer> t : temp.entrySet()) {
                        if (map.containsKey(t.getKey())) {
                            map.put(t.getKey(), t.getValue() + map.get(t.getKey()));
                        }
                        map.put(t.getKey(), t.getValue());
                    }
                    index = i + 1;
                    break;
                }
                k += String.valueOf(data[i]);
                if (data[i] >= 'A' && data[i] <= 'i') {
                    if (temp.containsKey(k)) {
                        temp.put(k, temp.get(k) + 1);
                    }
                    temp.put(k, 1);
                    k = "";
                } else if (data[i] >= 'a' && data[i] <= 'i') {
                    k += Character.toString(data[index]);
                } else {
                    Character tt = data[i - 1];
                    String ss = String.valueOf(tt);
                    int num = Integer.parseInt(String.valueOf(data[i]));
                    if (temp.containsKey(ss)) {
                        temp.put(ss, temp.get(ss) * num);
                    } else {
                        temp.put(ss, 1);
                        temp.put(ss, temp.get(ss) * num);
                    }
                }
            }
        }
        return index;
    }

    private static String outPut() {
        String s = "";
        for (Entry<String, Integer> data : map.entrySet()) {
            s += data.getKey() + ":" + data.getValue() + " ";
        }
        return s;
    }
}
