
package NonLinear;

import java.util.Random;

import basic.structure.BinaryTree;

public class 二叉排序树测试 extends Common{
    public static void main(String[] args) {
        BinaryTree<Integer> tree=new BinaryTree<>();
        Random random=new Random();
        for (int i = 0; i < 10; i++) {
            tree.put(random.nextInt(100)+1);
        }
        tree.put(-1);
        tree.put(35);
        tree.show();
        tree.delete(35);
        tree.show();
        tree.delete(-1);
        tree.show();
    }
}
