package 查找.Structure;

public abstract class OrderSymbolTable<Key extends Comparable<Key>,Value extends Comparable<Value>> extends SymbolTable<Key,Value>{
    protected abstract Key min(); //最小的键
    protected abstract Key max(); //最大的键
    protected abstract Key floor(Key key); //小于等于key的最大值
    protected abstract Key ceiling(Key key); //大于等于key的最小值
    protected abstract int rank(Key key); //小于key的键的数量
    protected abstract Key Select(int k); //排名为K的键
    protected abstract Iterable<Key> keys(Key lo,Key hi); //已排序的以lo到hi这个区间内的数据
    protected abstract int size(); //表中所有键的数量
    protected abstract void delete(Key key);
    @Override
    public Iterable<Key> keys() { //所有表中键的集合
        // TODO Auto-generated method stub
        return keys(min(),max());
    }
    public void deleteMin(){ //删除最小的键
        delete(min());
    }
    public void deleteMax(){ //删除最大的键
        delete(max());
    }
    public int size(Key lo,Key hi){ //lo到hi这个区间内的键的数量
        if(hi.compareTo(lo)<0){
            return 0;
        }
        else if(contains(hi)){
            return rank(hi)-rank(lo)+1;
        }
        else{
            return rank(hi)-rank(lo);
        }
    }
}
