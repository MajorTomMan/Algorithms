package com.majortom.algorithms.visualization.impl.controller;
/** The Graph family owns both algorithms and implementations; never a separate family. */
public enum GraphImplementation {
    SIMPLE("label.graph.implementation.simple"),
    MULTI("label.graph.implementation.multi");

    private final String labelKey;
    GraphImplementation(String labelKey) { this.labelKey = labelKey; }
    public String labelKey() { return labelKey; }
}
