

package basic.structure.interfaces;

public interface Queue<T> extends List<T> {
    public T peak();
    public T poll();
}
