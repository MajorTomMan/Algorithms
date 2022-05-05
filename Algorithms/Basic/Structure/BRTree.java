package Basic.Structure;

import Basic.Structure.Interface.IBRTree;
import Basic.Structure.Node.Treenode;

public class BRTree<T extends Comparable<T>> implements IBRTree<T> {
    private Treenode<T> Root;
    private int depth;
    private int count;

    public BRTree(T data) {
        Root = new Treenode<>(data, null, null);
    }

    public BRTree() {
    }

    @Override
    public boolean isEmpty() {
        // TODO Auto-generated method stub
        if (Root == null) {
            return true;
        }
        return false;
    }

    @Override
    public int Size() {
        // TODO Auto-generated method stub
        count = Size(Root);
        return count;
    }

    private int Size(Treenode<T> node) {
        if (node == null) {
            return 0;
        }
        Size(node.Left);
        Size(node.Right);
        return count++;
    }

    @Override
    public void put(T data) {
        // TODO Auto-generated method stub
        if(isEmpty()){
            Root=new Treenode<T>(data,null,null);
            return;
        }
        Treenode<T> node = new Treenode<T>(data, null, null);
        put(Root, node);
    }

    private Treenode<T> put(Treenode<T> node, Treenode<T> target) {
        if (node == null) {
            node = target;
            depth++;
            count++;
            return node;
        }
        if (target.data.compareTo(node.data) < 0) { // target<node
            node.Left = put(node.Left, target);
            node.SubTreeNum++;
        } else if (target.data.compareTo(node.data) > 0) { // target>node
            node.Right = put(node.Right, target);
            node.SubTreeNum++;
        }
        return node;
    }
    @Override
    public T get(T data) {
        // TODO Auto-generated method stub
        return null;
    }
    public void Delete(T data) {
        // TODO Auto-generated method stub
        Treenode<T> node = new Treenode<T>(data, null, null);
        Delete(Root, node);
        Size();
    }

    private Treenode<T> Delete(Treenode<T> node, Treenode<T> target) {
        if (node == null) {
            depth--;
            return node;
        }
        if (node.data.compareTo(target.data) < 0) {
            node.Right = Delete(node.Right, target);
        } else if (node.data.compareTo(target.data) == 0) {
            int flag = Check(node, target);
            if (flag == 1) {
                node = node.Right;
            } else if (flag == 0) {
                node = node.Left;
            } else {
                Treenode<T> temp = node;
                node = Min(temp.Right);
                node.Right = delMin(temp.Right);
                node.Left = temp.Left;
                if (target.data.compareTo(Root.data) == 0) {
                    Root = node;
                }
            }
        } else {
            node.Left = Delete(node.Left, target);
        }
        return node;
    }

    private int Check(Treenode<T> node, Treenode<T> target) {
        int flag = 0;
        if (node.Left == null && node.Right != null) {
            flag = 1;
        } else if (node.Right == null && node.Left != null) {
            flag = 0;
        } else if (node.Left != null && node.Right != null) {
            flag = -1;
        }
        return flag;
    }

    @Override
    public T Max() {
        // TODO Auto-generated method stub
        Treenode<T> node;
        for (node = Root; node.Right != null; node = node.Right)
            ;
        return node.data;
    }

    private Treenode<T> Max(Treenode<T> node) {
        if (node == null) {
            return node;
        }
        return Max(node.Right);
    }

    @Override
    public T Min() {
        // TODO Auto-generated method stub
        Treenode<T> node;
        for (node = Root; node.Left != null; node = node.Left)
            ;
        return node.data;
    }

    private Treenode<T> delMax(Treenode<T> node) {
        if (node.Right == null) {
            node.SubTreeNum--;
            return node.Left;
        }
        node.Right = delMin(node.Right);
        return node;
    }

    private Treenode<T> delMin(Treenode<T> node) {
        if (node.Left == null) {
            node.SubTreeNum--;
            return node.Right;
        }
        node.Left = delMin(node.Left);
        return node;
    }

    private Treenode<T> Min(Treenode<T> node) {
        if (node.Left == null) {
            return node;
        }
        return Min(node.Left);
    }

    /* 中序遍历 递归 */
    @Override
    public void Show() {
        // TODO Auto-generated method stub
        Show(Root);
    }

    private void Show(Treenode<T> node) {
        if (node == null) {
            return;
        }
        Show(node.Left);
        System.out.print(node.data + " ");
        Show(node.Right);
    }
    public void Print(){
        if (Root == null) System.out.println("EMPTY!");
        // 得到树的深度
        int treeDepth = getTreeDepth(Root);

        // 最后一行的宽度为2的（n - 1）次方乘3，再加1
        // 作为整个二维数组的宽度
        int arrayHeight = treeDepth * 2 - 1;
        int arrayWidth = (2 << (treeDepth - 2)) * 3 + 1;
        // 用一个字符串数组来存储每个位置应显示的元素
        String[][] res = new String[arrayHeight][arrayWidth];
        // 对数组进行初始化，默认为一个空格
        for (int i = 0; i < arrayHeight; i ++) {
            for (int j = 0; j < arrayWidth; j ++) {
                res[i][j] = " ";
            }
        }

        // 从根节点开始，递归处理整个树
        // res[0][(arrayWidth + 1)/ 2] = (char)(Root.val + '0');
        writeArray(Root, 0, arrayWidth/ 2, res, treeDepth);

        // 此时，已经将所有需要显示的元素储存到了二维数组中，将其拼接并打印即可
        for (String[] line: res) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < line.length; i ++) {
                sb.append(line[i]);
                if (line[i].length() > 1 && i <= line.length - 1) {
                    i += line[i].length() > 4 ? 2: line[i].length() - 1;
                }
            }
            System.out.println(sb.toString());
        }
    }
    // 用于获得树的层数
    private int getTreeDepth(Treenode<T> root) {
        return root == null ? 0 : (1 + Math.max(getTreeDepth(root.Left), getTreeDepth(root.Right)));
    }


    private void writeArray(Treenode<T> currNode, int rowIndex, int columnIndex, String[][] res, int treeDepth) {
        // 保证输入的树不为空
        if (currNode == null) return;
        // 先将当前节点保存到二维数组中
        res[rowIndex][columnIndex] = String.valueOf(currNode.data);

        // 计算当前位于树的第几层
        int currLevel = ((rowIndex + 1) / 2);
        // 若到了最后一层，则返回
        if (currLevel == treeDepth) return;
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

    public Treenode<T> getRoot() {
        return Root;
    }

    public void setRoot(Treenode<T> root) {
        Root = root;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
    
}