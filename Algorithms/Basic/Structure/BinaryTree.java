package Basic.Structure;

import java.util.LinkedList;
import java.util.Queue;
import Basic.Structure.Interface.IBRTree;
import Basic.Structure.Node.Treenode;

public class BinaryTree<T extends Comparable<T>> implements IBRTree<T> {
    private Treenode<T> Root;
    private int depth;
    private int count;

    public BinaryTree(T data) {
        Root = new Treenode<>(data, null, null);
    }

    public BinaryTree() {
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
        if (isEmpty()) {
            Root = new Treenode<T>(data, null, null);
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
    public void Show() {
        // TODO Auto-generated method stub
        Show(Root);
        System.out.println();
    }

    private void Show(Treenode<T> node) {
        if (node == null) {
            return;
        }
        Show(node.Left);
        System.out.print(node.data + " ");
        Show(node.Right);
    }

    public void printTree() {
        printTree(Root, 0);
    }

    private void printTree(Treenode<T> root, int level) {
        if (root == null) {
            return;
        }
        printTree(root.Right, level + 1);
        if (level != 0) {
            for (int i = 0; i < level - 1; i++) {
                System.out.print("|\t");
            }
            System.out.println("|-------" + root.data);
        } else {
            System.out.println(root.data);
        }
        printTree(root.Left, level + 1);
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