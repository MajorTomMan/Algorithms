package 查找;

public abstract class SymbolTable<Key,Value>{
   protected abstract void put(Key key, Value val);//加入键值对
    protected abstract Value get(Key key); //获取键Key对应的值
   protected abstract int size();
   protected abstract Iterable<Key> keys();
   protected void delete(Key key){
      put(key,null);
   }
   protected boolean contains(Key key){
      return get(key)!=null;
   }
   protected boolean isEmpty(){
      return size()==0;
   }
}
