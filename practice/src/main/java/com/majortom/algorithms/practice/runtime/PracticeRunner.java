package com.majortom.algorithms.practice.runtime;

import com.majortom.algorithms.practice.runtime.model.ProblemDescriptor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

public final class PracticeRunner {
    public Object run(ProblemDescriptor descriptor, Object... arguments) {
        Objects.requireNonNull(descriptor, "descriptor");
        Method entry = descriptor.entryPoint();
        Object receiver = null;
        try {
            if (!Modifier.isStatic(entry.getModifiers())) {
                receiver = descriptor.implementation().getDeclaredConstructor().newInstance();
            }
            return entry.invoke(receiver, arguments);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            if (cause instanceof Error error) throw error;
            throw new PracticeExecutionException("Problem entry failed: " + descriptor.stableId(), cause);
        } catch (ReflectiveOperationException exception) {
            throw new PracticeExecutionException("Unable to invoke problem entry: " + descriptor.stableId(), exception);
        }
    }

    public static final class PracticeExecutionException extends RuntimeException {
        public PracticeExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
