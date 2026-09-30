package com.majortom.algorithms.algorithm.discovery;

import com.majortom.algorithms.core.registry.RegistrationException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Resolves ordered generic arguments of an Algorithm entry's declared Structure contract. */
final class StructureTypeSignatureResolver {
  private StructureTypeSignatureResolver() {}

  static List<Class<?>> resolve(Type entryParameter, Class<?> structureContract) {
    if (structureContract.getTypeParameters().length == 0) {
      return List.of();
    }
    List<Class<?>> result =
        resolve(entryParameter, structureContract, new HashMap<>());
    if (result == null) {
      throw new RegistrationException(
          "Unable to resolve generic Structure signature for "
              + entryParameter.getTypeName() + " against " + structureContract.getName());
    }
    return result;
  }

  private static List<Class<?>> resolve(
      Type current,
      Class<?> target,
      Map<TypeVariable<?>, Type> bindings) {
    Type resolvedCurrent = resolveType(current, bindings);
    Class<?> rawType;
    Map<TypeVariable<?>, Type> nested = new HashMap<>(bindings);

    if (resolvedCurrent instanceof ParameterizedType parameterizedType) {
      if (!(parameterizedType.getRawType() instanceof Class<?> raw)) {
        return null;
      }
      rawType = raw;
      TypeVariable<?>[] variables = rawType.getTypeParameters();
      Type[] arguments = parameterizedType.getActualTypeArguments();
      for (int index = 0; index < variables.length; index++) {
        nested.put(variables[index], resolveType(arguments[index], bindings));
      }
    } else if (resolvedCurrent instanceof Class<?> raw) {
      rawType = raw;
    } else {
      return null;
    }

    if (rawType.equals(target)) {
      List<Class<?>> result = new ArrayList<>(target.getTypeParameters().length);
      for (TypeVariable<?> variable : target.getTypeParameters()) {
        Type resolved = resolveType(nested.getOrDefault(variable, variable), nested);
        if (!(resolved instanceof Class<?> concrete)) {
          throw new RegistrationException(
              "Algorithm entry Structure generic parameter remains unresolved: "
                  + variable.getName() + " in " + current.getTypeName());
        }
        result.add(concrete);
      }
      return List.copyOf(result);
    }

    for (Type interfaceType : rawType.getGenericInterfaces()) {
      List<Class<?>> result = resolve(interfaceType, target, nested);
      if (result != null) return result;
    }

    Type superclass = rawType.getGenericSuperclass();
    if (superclass != null && !Object.class.equals(superclass)) {
      List<Class<?>> result = resolve(superclass, target, nested);
      if (result != null) return result;
    }
    return null;
  }

  private static Type resolveType(Type type, Map<TypeVariable<?>, Type> bindings) {
    Type resolved = type;
    while (resolved instanceof TypeVariable<?> variable && bindings.containsKey(variable)) {
      Type next = bindings.get(variable);
      if (next.equals(resolved)) break;
      resolved = next;
    }
    return resolved;
  }
}
