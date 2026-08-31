package com.majortom.algorithms.visualization.execution;

import java.util.List;
import java.util.Optional;

/** Stores bounded local execution records independently from module controllers. */
public interface RunHistoryService {

    void add(ClientExecutionRecord record);

    Optional<ClientExecutionRecord> latest();

    List<ClientExecutionRecord> comparableWith(ClientExecutionRecord record);

    List<ClientExecutionRecord> all();

    void clear();
}
