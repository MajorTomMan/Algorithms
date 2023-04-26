/*
 * @Date: 2023-04-26 16:51:29
 * @LastEditors: hujunhao hujunhao@rtczsz.com
 * @LastEditTime: 2023-04-26 17:40:55
 * @FilePath: /alg/Algorithms/search/structure/example/SymbolTable.java
 */
package search.structure.example;

public abstract class SymbolTable<Key,Value>{
   public abstract void put(Key key, Value val);//加入键值对
   public abstract Value get(Key key); //获取键Key对应的值
   public abstract int size();
   public abstract Iterable<Key> keys();
   public void delete(Key key){
      put(key,null);
   }
   public boolean contains(Key key){
      return get(key)!=null;
   }
   public boolean isEmpty(){
      return size()==0;
   }
}
