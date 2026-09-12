package com.majortom.algorithms.practice.runtime.worker;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

record WorkerInvocation(String className, String methodName, String[] parameterTypeNames, Object[] arguments)
        implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    WorkerInvocation {
        className = Objects.requireNonNull(className, "className");
        methodName = Objects.requireNonNull(methodName, "methodName");
        parameterTypeNames = parameterTypeNames.clone();
        arguments = arguments.clone();
    }

    @Override public String[] parameterTypeNames() { return parameterTypeNames.clone(); }
    @Override public Object[] arguments() { return arguments.clone(); }

    @Override public String toString() {
        return className + "#" + methodName + Arrays.toString(parameterTypeNames);
    }
}
