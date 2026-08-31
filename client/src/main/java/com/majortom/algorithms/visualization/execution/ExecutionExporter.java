package com.majortom.algorithms.visualization.execution;

import com.majortom.algorithms.core.runtime.ExecutionSummary;

import java.io.IOException;
import java.nio.file.Path;

/** Persists one validated client execution without exposing file-system details to controllers. */
public interface ExecutionExporter {

    Path export(ClientExecutionRecord record, ExecutionSummary summary) throws IOException;
}
