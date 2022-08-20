package Basic.Structure.Node;

import java.util.List;

public class TRnode<T> {
    private T data;
    private int level;
    private int childCount;
    private TRnode<T> parent;
    private List<TRnode<T>> child;
    private List<TRnode<T>> company;

    /**
     * @param data   数据
     * @param level  层级
     * @param parent 父节点
     * @param child  子节点
     */
    public TRnode(T data, int level, TRnode<T> parent, List<TRnode<T>> child) {
        this.data = data;
        this.level = level;
        this.parent = parent;
        this.child = child;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */

    /**
     * @return the data
     */
    public T getData() {
        return data;
    }

    /**
     * @param data the data to set
     */
    public void setData(T data) {
        this.data = data;
    }

    /**
     * @return the level
     */
    public int getLevel() {
        return level;
    }

    /**
     * @param level the level to set
     */
    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * @return the childCount
     */
    public int getChildCount() {
        return childCount;
    }

    /**
     * @param childCount the childCount to set
     */
    public void setChildCount(int childCount) {
        this.childCount = childCount;
    }

    /**
     * @return the parent
     */
    public TRnode<T> getParent() {
        return parent;
    }

    /**
     * @param parent the parent to set
     */
    public void setParent(TRnode<T> parent) {
        this.parent = parent;
    }

    /**
     * @return the child
     */
    public List<TRnode<T>> getChild() {
        return child;
    }

    /**
     * @param child the child to set
     */
    public void setChild(List<TRnode<T>> child) {
        this.child = child;
    }

    /**
     * @return the company
     */
    public List<TRnode<T>> getCompany() {
        return company;
    }

    /**
     * @param company the company to set
     */
    public void setCompany(List<TRnode<T>> company) {
        this.company = company;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    
    @Override
    public String toString() {
        return "TRnode [child=" + child + ", childCount=" + childCount + ", company=" + company + ", data=" + data
                + ", level=" + level + ", parent=" + parent + "]";
    }
    
}
