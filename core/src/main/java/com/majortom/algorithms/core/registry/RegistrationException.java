package com.majortom.algorithms.core.registry;

public final class RegistrationException extends IllegalStateException {

    public RegistrationException(String message) {
        super(message);
    }

    public RegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
