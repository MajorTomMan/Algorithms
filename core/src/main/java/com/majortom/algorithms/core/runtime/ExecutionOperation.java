package com.majortom.algorithms.core.runtime;
@FunctionalInterface
public interface ExecutionOperation<T> { T execute() throws InterruptedException; }
