package basic.structure;

import basic.structure.iface.ITree;
import basic.structure.node.TreeNode;

public class BinaryTree<T extends Comparable<T>> implements ITree<T> {
    private TreeNode<T> root;
    private Integer size;

    @Override
    public boolean isEmpty() {
        // TODO Auto-generated method stub
        return root == null ? true : false;
    }

    @Override
    public int size() {
        // TODO Auto-generated method stub
        return size;
    }

    @Override
    public void put(T data) {
        // TODO Auto-generated method stub
        if (isEmpty()) {
            root = new TreeNode<T>(data);
        }
        root = put(root, data);
    }

    /* 左最小右最大 */
    private TreeNode<T> put(TreeNode<T> node, T data) {
        if (node == null) {
            return new TreeNode<T>(data);
        }
        /* node.data>data */
        if (node.data.compareTo(data) < 0) {
            node.right = put(node.right, data);
            node.subtreenum++;
        }
        if (node.data.compareTo(data) > 0) {
            node.left = put(node.left, data);
            node.subtreenum++;
        }
        return node;
    }

    @Override
    public T get(T key) {
        // TODO Auto-generated method stub
        if (isEmpty()) {
            return null;
        }
        TreeNode<T> node = get(root, key);
        return node.data;
    }

    private TreeNode<T> get(TreeNode<T> node, T key) {
        if (node == null) {
            return node;
        }
        if (node.data.compareTo(key) == 0) {
            return node;
        }
        if (node.data.compareTo(key) < 0) {
            return get(node.right, key);
        }
        if (node.data.compareTo(key) > 0) {
            return get(node.left, key);
        }
        return node;
    }

    @Override
    public void delete(T key) {
        // TODO Auto-generated method stub
        if (isEmpty()) {
            return;
        }
        delete(root, key);
    }

    private TreeNode<T> delete(TreeNode<T> node, T key) {
        if (root == null) {
            return node;
        }
        if (node.data.compareTo(key) == 0) {
            /* 叶子节点 */
            if (node.left == null && node.right == null) {
                return null;
            }
            /* 仅有右子树 */
            if (node.left == null && node.right != null) {
                return node.right;
            }
            /* 仅有左子树 */
            if (node.right == null && node.left != null) {
                return node.left;
            }
            /* 左右子树皆有 */
            node = appendToRight(node.left);
        }
        if (node.data.compareTo(key) < 0) {
            node.right = delete(node.right, key);
        }
        if (node.data.compareTo(key) > 0) {
            node.left = delete(node.left, key);
        }
        node.subtreenum--;
        return node;

    }

    /* 将左子树的最右节点获取并返回 */
    private TreeNode<T> appendToRight(TreeNode<T> left) {
        if (left.right == null) {
            return left;
        }
        return appendToRight(left.right);
    }

    public void show() {
        if (isEmpty()) {
            return;
        }
        show(root);
        System.out.println();
    }

    private TreeNode<T> show(TreeNode<T> node) {
        if (node == null) {
            return node;
        }
        show(node.left);
        System.out.print(node.data + " ");
        show(node.right);
        return node;
    }
}