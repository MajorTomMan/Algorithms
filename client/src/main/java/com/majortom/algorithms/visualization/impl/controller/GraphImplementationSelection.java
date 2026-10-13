package com.majortom.algorithms.visualization.impl.controller;
import java.util.function.Consumer;
/** Notifies the owning workbench when the active Graph implementation changes. */
public interface GraphImplementationSelection {
    void setImplementationChangeListener(Consumer<GraphImplementation> listener);
}
