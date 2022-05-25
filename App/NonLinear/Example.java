package NonLinear;

import java.util.Random;

import Basic.Structure.BRTree;
import Basic.Structure.Node.Treenode;

public abstract class Example {
    private static Treenode<Integer> root;

    public static Treenode<Integer> buildTree(Integer[] nums) {
        root = buildTree(nums, 1);
        return root;
    }

    private static Treenode<Integer> buildTree(Integer[] nums, int index) {
        if (index > nums.length) {
            return null;
        }
        Integer value = nums[index - 1];
        if (value == null) {
            return null;
        }
        Treenode<Integer> node = new Treenode<Integer>(value);
        node.Left = buildTree(nums, index * 2);
        node.Right = buildTree(nums, index * 2 + 1);
        return node;
    }

    public static BRTree<Integer> buildTreeByRandom(int times) {
        int i = 0;
        BRTree<Integer> tree = new BRTree<Integer>();
        Random random = new Random();
        System.out.println("-----------------------raw data--------------------");
        while (i != times) {
            int ran = random.nextInt(100);
            tree.put(ran);
            System.out.print(ran + ",");
            i++;
        }
        root = tree.getRoot();
        return tree;
    }

    public static void printTree(Treenode<Integer> root) {
        System.out.println();
        System.out.println();
        System.out.println();
        if (root == null)
            System.out.println("EMPTY!");
        // 得到树的深度
        int treeDepth = getTreeDepth(root);

        // 最后一行的宽度为2的（n - 1）次方乘3，再加1
        // 作为整个二维数组的宽度
        int arrayHeight = treeDepth * 2 - 1;
        int arrayWidth = (2 << (treeDepth - 2)) * 3 + 1;
        // 用一个字符串数组来存储每个位置应显示的元素
        String[][] res = new String[arrayHeight][arrayWidth];
        // 对数组进行初始化，默认为一个空格
        for (int i = 0; i < arrayHeight; i++) {
            for (int j = 0; j < arrayWidth; j++) {
                res[i][j] = " ";
            }
        }

        // 从根节点开始，递归处理整个树
        // res[0][(arrayWidth + 1)/ 2] = (char)(root.val + '0');
        writeArray(root, 0, arrayWidth / 2, res, treeDepth);

        // 此时，已经将所有需要显示的元素储存到了二维数组中，将其拼接并打印即可
        for (String[] line : res) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < line.length; i++) {
                sb.append(line[i]);
                if (line[i].length() > 1 && i <= line.length - 1) {
                    i += line[i].length() > 4 ? 2 : line[i].length() - 1;
                }
            }
            System.out.println(sb.toString());
        }
    }


    public static void printTree() {
        System.out.println();
        System.out.println();
        System.out.println();
        if (root == null)
            System.out.println("EMPTY!");
        // 得到树的深度
        int treeDepth = getTreeDepth(root);

        // 最后一行的宽度为2的（n - 1）次方乘3，再加1
        // 作为整个二维数组的宽度
        int arrayHeight = treeDepth * 2 - 1;
        int arrayWidth = (2 << (treeDepth - 2)) * 3 + 1;
        // 用一个字符串数组来存储每个位置应显示的元素
        String[][] res = new String[arrayHeight][arrayWidth];
        // 对数组进行初始化，默认为一个空格
        for (int i = 0; i < arrayHeight; i++) {
            for (int j = 0; j < arrayWidth; j++) {
                res[i][j] = " ";
            }
        }

        // 从根节点开始，递归处理整个树
        // res[0][(arrayWidth + 1)/ 2] = (char)(root.val + '0');
        writeArray(root, 0, arrayWidth / 2, res, treeDepth);

        // 此时，已经将所有需要显示的元素储存到了二维数组中，将其拼接并打印即可
        for (String[] line : res) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < line.length; i++) {
                sb.append(line[i]);
                if (line[i].length() > 1 && i <= line.length - 1) {
                    i += line[i].length() > 4 ? 2 : line[i].length() - 1;
                }
            }
            System.out.println(sb.toString());
        }
    }

    // 用于获得树的层数
    private static int getTreeDepth(Treenode<Integer> root) {
        return root == null ? 0 : (1 + Math.max(getTreeDepth(root.Left), getTreeDepth(root.Right)));
    }

    private static void writeArray(Treenode<Integer> currNode, int rowIndex, int columnIndex, String[][] res,
            int treeDepth) {
        // 保证输入的树不为空
        if (currNode == null)
            return;
        // 先将当前节点保存到二维数组中
        res[rowIndex][columnIndex] = String.valueOf(currNode.data);

        // 计算当前位于树的第几层
        int currLevel = ((rowIndex + 1) / 2);
        // 若到了最后一层，则返回
        if (currLevel == treeDepth)
            return;
        // 计算当前行到下一行，每个元素之间的间隔（下一行的列索引与当前元素的列索引之间的间隔）
        int gap = treeDepth - currLevel - 1;

        // 对左儿子进行判断，若有左儿子，则记录相应的"/"与左儿子的值
        if (currNode.Left != null) {
            res[rowIndex + 1][columnIndex - gap] = "/";
            writeArray(currNode.Left, rowIndex + 2, columnIndex - gap * 2, res, treeDepth);
        }

        // 对右儿子进行判断，若有右儿子，则记录相应的"\"与右儿子的值
        if (currNode.Right != null) {
            res[rowIndex + 1][columnIndex + gap] = "\\";
            writeArray(currNode.Right, rowIndex + 2, columnIndex + gap * 2, res, treeDepth);
        }
    }
}
