package com.majortom.algorithms.core.registry;
import java.util.*;
public final class ModuleRegistry {
 private final Map<String,Class<?>> implementations; ModuleRegistry(Map<String,Class<?>> implementations){this.implementations=Map.copyOf(new LinkedHashMap<>(implementations));}
 public boolean contains(String key){return implementations.containsKey(requireKey(key));} public Optional<Class<?>> find(String key){return Optional.ofNullable(implementations.get(requireKey(key)));}
 public Class<?> require(String key){String k=requireKey(key);Class<?> c=implementations.get(k);if(c==null)throw new IllegalArgumentException("No implementation registered for: "+k);return c;}
 public <T> Class<? extends T> require(String key,Class<T> contract){Objects.requireNonNull(contract);Class<?> c=require(key);if(!contract.isAssignableFrom(c))throw new IllegalStateException("Registered implementation "+c.getName()+" does not implement "+contract.getName()+" for key "+key);return c.asSubclass(contract);}
 public <T>T create(String key,Class<T> contract){Class<? extends T> c=require(key,contract);try{return c.getDeclaredConstructor().newInstance();}catch(ReflectiveOperationException e){throw new IllegalStateException("Registered implementation requires an accessible no-arg constructor: "+c.getName(),e);}}
 public List<String> keys(String prefix){Objects.requireNonNull(prefix);return implementations.keySet().stream().filter(k->k.startsWith(prefix)).sorted().toList();} public Map<String,Class<?>> entries(){return implementations;}
 private static String requireKey(String k){Objects.requireNonNull(k);if(k.isBlank())throw new IllegalArgumentException("key must not be blank");return k;}
}
