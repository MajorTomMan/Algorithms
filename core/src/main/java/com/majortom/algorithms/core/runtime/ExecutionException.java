package com.majortom.algorithms.core.runtime;

public class ExecutionException extends RuntimeException {

    private final String code;

    public ExecutionException(String code, String message) {
        super(message);
        this.code = code;
    }

    public ExecutionException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
