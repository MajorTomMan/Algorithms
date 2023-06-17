package Linear;

import java.util.ArrayList;
import java.util.List;

public class 杨辉三角 {
    public static void main(String[] args) {
        System.out.println(generate(6));
        System.out.println(getRow(3));
    }
    public static List<List<Integer>> generate(int numRows) {
        List<List<Integer>> ret = new ArrayList<List<Integer>>();
        for (int i = 0; i < numRows; ++i) {
            List<Integer> row = new ArrayList<Integer>();
            for (int j = 0; j <= i; ++j) {
                if (j == 0 || j == i) {
                    row.add(1);
                } else {
                    row.add(ret.get(i - 1).get(j - 1) + ret.get(i - 1).get(j));
                }
            }
            ret.add(row);
        }
        return ret;
    }
    public static List<Integer> getRow(int rowIndex) {
        List<List<Integer>> result=generate(rowIndex+1);
        return result.get(rowIndex);
    }
}
