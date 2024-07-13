package basic.structure;

import basic.structure.interfaces.Tree;
import basic.structure.node.TreeNode;

public class BinarySearchTree<Key extends Comparable<Key>, Value extends Comparable<Value>>
        implements Tree<Key, Value> {
    private TreeNode<Key, Value> root;
    private Integer size;

    @Override
    public boolean isEmpty() {
        // TODO Auto-generated method stub
        return root == null;
    }

    @Override
    public int size() {
        // TODO Auto-generated method stub
        return size;
    }

    /*
     * 根据键的字典序来决定子节点插入
     */
    @Override
    public void put(Key key, Value value) {
        // TODO Auto-generated method stub
        if (isEmpty()) {
            root = new TreeNode<Key, Value>(key, value);
            return;
        }
        root = doPut(key, value, root);
    }

    /*
     * 二叉搜索树,左小右大
     */
    private TreeNode<Key, Value> doPut(Key key, Value value, TreeNode<Key, Value> node) {
        if (node == null) {
            return new TreeNode<Key, Value>(key, value, null, null);
        }
        if (node.key.compareTo(key) < 0) {
            node.left = doPut(key, value, node.left);
        } else if (node.key.compareTo(key) > 0) {
            node.right = doPut(key, value, node.right);
        }
        return doPut(key, value, node);
    }

    @Override
    public Value get(Key key) {
        // TODO Auto-generated method stub
        if (isEmpty()) {
            return null;
        }
        return doGet(key, root);
    }

    private Value doGet(Key key, TreeNode<Key, Value> node) {
        if (node == null) {
            return null;
        }
        if (node.key.compareTo(key) == 0) {
            return node.value;
        } else if (node.key.compareTo(key) < 0) {
            return doGet(key, node.left);
        } else if (node.key.compareTo(key) > 0) {
            return doGet(key, node.right);
        }
        return null;
    }

    /*
     * 只删除值不删除键
     */
    @Override
    public void remove(Key key) {
        // TODO Auto-generated method stub
        if (isEmpty()) {
            return;
        }
        if (root.key.compareTo(key) == 0) {
            root = null;
        }
    }

    /*
     * 从根节点开始，根据二叉搜索树的性质，比较要删除的节点值和当前节点值的大小关系，
     * 向左子树或右子树递归查找要删除的节点。
     * 如果找到了要删除的节点，分为以下几种情况处理：
     * 节点没有子节点（叶子节点）：直接删除。
     * 节点有一个子节点：将父节点指向其子节点。
     * 节点有两个子节点：找到其右子树中的最小节点（后继节点），
     * 用后继节点的值替换要删除节点的值，然后删除后继节点。
     * 
     */
    private TreeNode<Key, Value> doRemove(Key key, TreeNode<Key, Value> node) {
        if (node == null) {
            return node;
        }
        if (node.key.compareTo(key) == 0) {
            return doRemove(node);
        } else if (node.key.compareTo(key) > 0) {
            node.right = doRemove(key, node.right);
        } else if (node.key.compareTo(key) < 0) {
            node.left = doRemove(key, node.left);
        }
        return node;
    }

    private TreeNode<Key, Value> doRemove(TreeNode<Key, Value> node) {
        // TODO Auto-generated method stub
        if (node == null || (node.left == null && node.right == null)) {
            return null;
        }
        if (node.left == null) {
            return node.right;
        } else if (node.right == null) {
            return node.left;
        }
        Value minValue = findMinNode(node.left);
        node.value = minValue;
        return node;
    }

    /*
     * 找到右子树中最小的节点的值
     */
    private Value findMinNode(TreeNode<Key, Value> node) {
        // TODO Auto-generated method stub
        if (node.left == null) {
            Value value = node.value;
            node = null;
            return value;
        }
        return findMinNode(node.left);
    }

    @Override
    public void replace(Key key, Value value) {
        // TODO Auto-generated method stub
        if (isEmpty()) {
            return;
        }
        doReplace(key, value, root);
    }

    public void doReplace(Key key, Value value, TreeNode<Key, Value> node) {
        if (node == null) {
            return;
        }
        if (node.key.compareTo(key) == 0) {
            node.key = key;
            node.value = value;
            return;
        } else if (node.key.compareTo(key) > 0) {
            doRemove(key, node.right);
        } else if (node.key.compareTo(key) < 0) {
            doRemove(key, node.left);
        }
    }

    @Override
    public void foreach() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'foreach'");
    }
}