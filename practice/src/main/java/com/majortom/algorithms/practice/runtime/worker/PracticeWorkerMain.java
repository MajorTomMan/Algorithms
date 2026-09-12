package com.majortom.algorithms.practice.runtime.worker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Child-JVM entry point. It executes only already-resolved metadata supplied by the host. */
public final class PracticeWorkerMain {
    static final String RESULT_PREFIX = "PRACTICE_RESULT:";

    private PracticeWorkerMain() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("worker expects one encoded invocation");
        WorkerInvocation invocation = (WorkerInvocation) WorkerCodec.decode(args[0]);
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Class<?> owner = Class.forName(invocation.className(), true, loader);
        Class<?>[] parameterTypes = new Class<?>[invocation.parameterTypeNames().length];
        for (int i = 0; i < parameterTypes.length; i++) {
            parameterTypes[i] = resolveType(invocation.parameterTypeNames()[i], loader);
        }
        Method method = owner.getDeclaredMethod(invocation.methodName(), parameterTypes);
        method.trySetAccessible();
        Object receiver = Modifier.isStatic(method.getModifiers()) ? null : owner.getDeclaredConstructor().newInstance();
        try {
            Object result = method.invoke(receiver, invocation.arguments());
            System.out.println(RESULT_PREFIX + WorkerCodec.encode(result));
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception checked) throw checked;
            if (cause instanceof Error error) throw error;
            throw exception;
        }
    }

    private static Class<?> resolveType(String name, ClassLoader loader) throws ClassNotFoundException {
        return switch (name) {
            case "boolean" -> boolean.class;
            case "byte" -> byte.class;
            case "short" -> short.class;
            case "int" -> int.class;
            case "long" -> long.class;
            case "float" -> float.class;
            case "double" -> double.class;
            case "char" -> char.class;
            case "void" -> void.class;
            default -> Class.forName(name, false, loader);
        };
    }
}
