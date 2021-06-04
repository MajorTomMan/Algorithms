package Structure;


public class BTree<T extends Comparable<T>>implements IBTree<T>{
    BTRnode<T> Root;
    int depth; 
    @Override
    public void InitializeBTree(BTRnode<T> node) {
        // TODO Auto-generated method stub
        Root=node;
    }
    @Override
    public boolean BTreeIsEmpty() {
        // TODO Auto-generated method stub
        if(Root==null){
            return true;
        }
        return false;
    }
    @Override
    public boolean BTreeIsFull() {
        // TODO Auto-generated method stub
        return false;
    }
    @Override
    public int BTreeItemCount() {
        // TODO Auto-generated method stub
        if(depth==0){
            SearchAll_P(Root);
        }
        return depth;
    }
    @Override
    public boolean EnBRTree(BTRnode<T> node) {
        // TODO Auto-generated method stub
        BTRnode<T> temp=Root;
        if(BTreeIsEmpty()){
            InitializeBTree(node);
            return true;
        }
        while(true){
            if(toLeft(node.item.saveData,temp)&&temp.Left!=null){
                temp=temp.Left;
            }
            else if(toLeft(node.item.saveData,temp)&&temp.Left==null){
                temp.Left=node;
                break;
            }
            else if(toRight(node.item.saveData,temp)&&temp.Right==null){
                temp.Right=node;
                break;
            }
            else if(toRight(node.item.saveData,temp)&&temp.Right!=null){
                temp=temp.Right;
            }
            else if(node.item.saveData.compareTo(temp.item.saveData)==0){
                break;
            }
        }
        return true;

    }
    @Override
    public boolean DeBRTree(T data) {
        // TODO Auto-generated method stub
        BTRnode<T> node=new BTRnode<T>();
        Stack<BTRnode<T>> stack=new Stack<>();
        if(toLeft(data, Root)){
            node=Root.Left;
        }
        else{
            node=Root.Right;
        }
        if(Root.Left.item.saveData==data||Root.Right.item.saveData==data){
            stack.push(Root);
        }
        while(node.item.saveData!=data&&data!=Root.item.saveData){
            stack.push(node);
            if(toLeft(data, node)){
                node=node.Left;
            }
            else if(toRight(data,node)){
                node=node.Right;
            }
            if(node==null){
                break;
            }
        }
        if(node.Left==null&&node.Right==null){
            BTRnode<T> temp=stack.pop();
            ChildIsNull(temp,node);
        }else if(node.Left==null&&node.Right!=null||node.Left!=null&&node.Right==null){
            BTRnode<T> temp=stack.pop();
            ChildHasOne(temp,node);
        }
        else{
            BTRnode<T> temp=stack.pop();
            ChildHasTwo(temp,node);
        }
        return true;
    }
    //@Override
    //public void BTree_Traverse(Method method) {
        // TODO Auto-generated method stub
        
   // }
    @Override
    public BTRnode<T> FindMax(BTRnode<T> node) {
        // TODO Auto-generated method stub
        if(node==null){
            return null;
        }
        else if(node.Right==null){
            return node;
        }
            return FindMax(node.Right);
    }
    @Override
    public BTRnode<T> FindMin(BTRnode<T> node) {
        // TODO Auto-generated method stub
        if(node==null){
            return null;
        }
        else if(node.Left==null){
            return node;
        }
        else{
            return FindMin(node.Left);
        }
    }
    public BTRnode<T> FindMax_NoFeedback(BTRnode<T> node) {
        // TODO Auto-generated method stub
        BTRnode<T> temp=node;
        if(node==null){
            return null;
        }
        while(temp.Right!=null){
            temp=temp.Right;
        }
        return temp;
    }
    public BTRnode<T> FindMin_NoFeedback(BTRnode<T> node) {
        BTRnode<T> temp=node;
        // TODO Auto-generated method stub
        if(node==null){
            return null;
        }
        while(temp.Left!=null){
            temp=temp.Left;
        }
        return temp;
    }
    /* 前序遍历 递归 */
    @Override
    public void SearchAll_P(BTRnode<T> node) {
        // TODO Auto-generated method stub
        if(node==null){
            return;
        }
        System.out.println(node.item.saveData);
        SearchAll_P(node.Left);
        SearchAll_P(node.Right);
        depth++;
    }
    /* 中序遍历 递归 */
    @Override
    public void SearchAll_M(BTRnode<T> node) {
        // TODO Auto-generated method stub
        if(node==null){
            return;
        }
        SearchAll_M(node.Left);
        System.out.println(node.item.saveData);
        SearchAll_M(node.Right);
    }
    /* 后序遍历 递归 */
    @Override
    public void SearchAll_R(BTRnode<T> node) {
        // TODO Auto-generated method stub
        if(node==null){
            return;
        }
        SearchAll_R(node.Left);
        SearchAll_R(node.Right);
        System.out.println(node.item.saveData);
    }
    /*      前序遍历非递归
    首先申请一个新的栈，记为stack；
    声明一个结点treeNode，让其指向node结点；
    如果treeNode的不为空，将treeNode的值打印，并将treeNode入栈，然后让treeNode指向treeNode的右结点，
    重复步骤3，直到treenode为空；
    然后出栈，让treeNode指向treeNode的右孩子
    重复步骤3，直到stack为空.*/
    public void SearchAll_P_noFeedBack(BTRnode<T> node) {
        // TODO Auto-generated method stub
        Stack<BTRnode<T>> stack=new Stack<>();
        BTRnode<T> temp=node;
        while(temp!=null||!stack.isEmpty()){
            while(temp!=null){
                System.out.println(temp.item.saveData);
                stack.push(temp);
                temp=temp.Left;
            }
            if(!stack.isEmpty()){
                temp=stack.pop();
                temp=temp.Right;
            }
        }
    }
       /*    中序遍历非递归
    申请一个新栈，记为stack，申请一个变量cur，初始时令treeNode为头节点；
    先把treeNode节点压入栈中，对以treeNode节点为头的整棵子树来说，依次把整棵树的左子树压入栈中，即不断令treeNode=treeNode.leftChild，然后重复步骤2；
    不断重复步骤2，直到发现cur为空，此时从stack中弹出一个节点记为treeNode，打印node的值，并让treeNode= treeNode.right，然后继续重复步骤2；
    当stack为空并且cur为空时结束。*/
    public void SearchAll_M_noFeedBack(BTRnode<T> node) {
        // TODO Auto-generated method stub
        Stack<BTRnode<T>> stack=new Stack<>();
        BTRnode<T> temp=node;
        while(temp!=null||!stack.isEmpty()){
            while(temp!=null){
                stack.push(temp);
                temp=temp.Left;
            }
            if(!stack.isEmpty()){
                temp=stack.pop();
                System.out.println(temp.item.saveData);
                temp=temp.Right;
            }
        }
    }
    /*           后序遍历非递归实现
    后序遍历这里较前两者实现复杂一点
    我们需要一个标记位来记忆我们此时节点上一个节点，具体看代码注释 */
    public void SearchAll_R_noFeedBack(BTRnode<T> node) {
        // TODO Auto-generated method stub
        Stack<BTRnode<T>> stack=new Stack<>();
        BTRnode<T> temp=node;
        BTRnode<T> lastVisit=null;
        while(temp!=null||!stack.isEmpty()){
            while(temp!=null){
                stack.push(temp);
                temp=temp.Left;
            }
             //出栈
             temp= stack.pop();
             /**
              * 这块就是判断temp是否有右孩子，
              * 如果没有输出temp.item.data，让lastVisit指向treeNode，并让treeNode为空
              * 如果有右孩子，将当前节点继续入栈，treeNode指向它的右孩子,继续重复循环
              */
             if(temp.Right == null || temp.Right== lastVisit) {
                 System.out.println(temp.item.saveData);
                 lastVisit = temp;
                 temp  = null;
             }else{
                 stack.push(temp);
                 temp = temp.Right;
             }


        }
    }
    /*      层次遍历非递归
    首先申请一个新的队列，记为queue；
    将头结点head压入queue中；
    每次从queue中出队，记为node，然后打印node值，如果node左孩子不为空，则将左孩子入队；如果node的右孩子不为空，则将右孩子入队；
    重复步骤3，直到queue为空。
     */
    public void SearchAll_L_noFeedBack(BTRnode<T> node) {
        // TODO Auto-generated method stub
        Queue<BTRnode<T>> queue=new Queue<>();
        BTRnode<T> temp;
        queue.push(node);
        while(!queue.isEmpty()){
            temp = queue.pop();
            System.out.println(temp.item.saveData);
            if(temp.Left!=null) {
                queue.push(temp.Left);
            }
            if(temp.Right!=null){
                queue.push(temp.Right);
            }
        }
    }
    private boolean toLeft(T saveData,BTRnode<T> node){
        int result=saveData.compareTo(node.item.saveData);
        if(result<0){ //比该节点的数值大 进入右子树比较
            return true;
        }
        return false;
    }
    private boolean toRight(T saveData,BTRnode<T> node){
        int result=saveData.compareTo(node.item.saveData);
        if(result>0){ //比该节点的数值大 进入右子树比较
            return true;
        }
        return false;
    }
    private void ChildIsNull(BTRnode<T> father,BTRnode<T> child){
        if(father.Left==child){
            father.Left=null;
        }
        else{
            father.Right=null;
        }
    }
    private void ChildHasOne(BTRnode<T> father,BTRnode<T> child){
        if(father.Left==child){
            father.Left=child.Left;
        }
        else{
            father.Right=child.Right;
        }
    }
    private void ChildHasTwo(BTRnode<T> father,BTRnode<T> child){
        BTRnode<T> temp=new BTRnode<>();
        temp=deletemin(father.Left);
        if(father.Left.item.saveData==child.item.saveData){
            temp.Left=father.Left.Left;
            temp.Right=father.Left.Right;
            father.Left=temp;
        }
        else{
            temp.Left=father.Right.Left;
            temp.Right=father.Right.Right;
            father.Right=temp;
        }
    }
    private BTRnode<T> deletemin(BTRnode<T> node){
        BTRnode<T> temp=new BTRnode<>();
        if(node==null){
            return null;
        }
        while(node.Right!=null){
            node=node.Right;
        }
        temp=node;
        node=null;
        return temp;
    }
    public BTRnode<T> getRoot() {
        return Root;
    }
    public int getDepth() {
        return depth;
    }
}
