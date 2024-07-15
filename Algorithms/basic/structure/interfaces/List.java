
package basic.structure.interfaces;

import java.util.Comparator;
import java.util.function.Consumer;

public interface List<T> extends Comparator<T>, Iterable<T> {

    public boolean isEmpty();

    public int size();

    public void add(T t);

    public void add(T[] t);

    public T get(int index);

    public void remove(int index);

    public void replace(T t, int index);

    public void foreach(Consumer<T> action);

    public void sort();

    public void reverse();

    public boolean contains(T t);

    @Override
    public default int compare(T arg0, T arg1) {
        // TODO Auto-generated method stub
        if (arg0 instanceof String) {
            String arg_1 = (String) arg0;
            String arg_2 = (String) arg1;
            return arg_1.compareTo(arg_2);
        } else if (arg0 instanceof Integer) {
            Integer arg_1 = (Integer) arg0;
            Integer arg_2 = (Integer) arg1;
            return arg_1.compareTo(arg_2);
        } else if (arg0 instanceof Double) {
            Double arg_1 = (Double) arg0;
            Double arg_2 = (Double) arg1;
            return arg_1.compareTo(arg_2);

        } else if (arg0 instanceof Float) {
            Float arg_1 = (Float) arg0;
            Float arg_2 = (Float) arg1;
            return arg_1.compareTo(arg_2);
        }
        if (arg0 instanceof Object) {
            if (arg0 == arg1) {
                return 0;
            } else {
                return -1;
            }
        }
        throw new IllegalArgumentException("不支持的类型");
    }
}
