package com.majortom.algorithms.algorithm.discovery;

import com.majortom.algorithms.core.annotation.Algorithm;
import com.majortom.algorithms.core.registry.AlgorithmDescriptor;
import com.majortom.algorithms.core.registry.FrameworkClassScanner;
import com.majortom.algorithms.core.registry.RegistrationException;
import com.majortom.algorithms.core.registry.RegistrationValidator;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class AlgorithmDiscovery {

    public static final String ROOT_PACKAGE = "com.majortom.algorithms.algorithm";

    private final FrameworkClassScanner scanner;

    public AlgorithmDiscovery() {
        this(new FrameworkClassScanner());
    }

    AlgorithmDiscovery(FrameworkClassScanner scanner) {
        this.scanner = Objects.requireNonNull(scanner, "scanner");
    }

    public List<AlgorithmDescriptor> discover(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "classLoader");
        List<AlgorithmDescriptor> descriptors = new ArrayList<>();
        for (Class<?> implementation : scanner.scan(ROOT_PACKAGE, classLoader)) {
            Algorithm annotation = implementation.getAnnotation(Algorithm.class);
            if (annotation == null) {
                continue;
            }
            AlgorithmDescriptor descriptor = new AlgorithmDescriptor(
                    annotation.id(), annotation.type(), annotation.structure(), implementation);
            RegistrationValidator.validate(descriptor);
            validateStructureUsage(descriptor);
            validateValueType(descriptor);
            descriptors.add(descriptor);
        }
        descriptors.sort(Comparator.comparing(AlgorithmDescriptor::id));
        RegistrationValidator.validateUniqueAlgorithmIds(descriptors);
        return List.copyOf(descriptors);
    }

    private void validateStructureUsage(AlgorithmDescriptor descriptor) {
        for (Method method : descriptor.implementation().getMethods()) {
            for (Class<?> parameterType : method.getParameterTypes()) {
                if (descriptor.structureContract().isAssignableFrom(parameterType)) {
                    return;
                }
            }
        }
        throw new RegistrationException("Algorithm " + descriptor.implementation().getName()
                + " does not expose declared structure contract " + descriptor.structureContract().getName());
    }

    private void validateValueType(AlgorithmDescriptor descriptor) {
        Set<Class<?>> resolvedTypes = new HashSet<>();
        collectConcreteTypeArguments(descriptor.implementation(), new HashMap<>(), resolvedTypes);
        if (!resolvedTypes.isEmpty()) {
            if (!resolvedTypes.contains(descriptor.valueType())) {
                throw new RegistrationException("Algorithm annotation type " + descriptor.valueType().getName()
                        + " does not match generic contract of " + descriptor.implementation().getName()
                        + ": " + resolvedTypes.stream().map(Class::getName).sorted().toList());
            }
            return;
        }
        for (Method method : descriptor.implementation().getMethods()) {
            for (Class<?> parameterType : method.getParameterTypes()) {
                if (parameterType.equals(descriptor.valueType())) {
                    return;
                }
            }
        }
        throw new RegistrationException("Unable to verify Algorithm value type " + descriptor.valueType().getName()
                + " for " + descriptor.implementation().getName());
    }

    private void collectConcreteTypeArguments(Type current, Map<TypeVariable<?>, Type> bindings, Set<Class<?>> result) {
        if (current instanceof ParameterizedType parameterizedType) {
            Class<?> rawType = (Class<?>) parameterizedType.getRawType();
            Map<TypeVariable<?>, Type> nested = new HashMap<>(bindings);
            TypeVariable<?>[] variables = rawType.getTypeParameters();
            Type[] arguments = parameterizedType.getActualTypeArguments();
            for (int index = 0; index < variables.length; index++) {
                Type resolved = resolve(arguments[index], bindings);
                nested.put(variables[index], resolved);
                if (resolved instanceof Class<?> type && isValueCandidate(type)) {
                    result.add(type);
                }
            }
            collectHierarchy(rawType, nested, result);
        } else if (current instanceof Class<?> type) {
            collectHierarchy(type, bindings, result);
        }
    }

    private void collectHierarchy(Class<?> type, Map<TypeVariable<?>, Type> bindings, Set<Class<?>> result) {
        for (Type interfaceType : type.getGenericInterfaces()) {
            collectConcreteTypeArguments(interfaceType, bindings, result);
        }
        Type superclass = type.getGenericSuperclass();
        if (superclass != null && !Object.class.equals(superclass)) {
            collectConcreteTypeArguments(superclass, bindings, result);
        }
    }

    private Type resolve(Type type, Map<TypeVariable<?>, Type> bindings) {
        Type resolved = type;
        while (resolved instanceof TypeVariable<?> variable && bindings.containsKey(variable)) {
            resolved = bindings.get(variable);
        }
        return resolved;
    }

    private boolean isValueCandidate(Class<?> type) {
        return type != Object.class && !type.isInterface() && !type.isArray();
    }
}
