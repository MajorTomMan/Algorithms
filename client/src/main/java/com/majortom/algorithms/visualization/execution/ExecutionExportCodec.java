package com.majortom.algorithms.visualization.execution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.majortom.algorithms.core.event.ExecutionEvent;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.core.runtime.ExecutionFailure;
import com.majortom.algorithms.core.runtime.ExecutionResult;
import com.majortom.algorithms.core.runtime.ExecutionStatistics;
import com.majortom.algorithms.core.runtime.ExecutionSummary;
import com.majortom.algorithms.core.runtime.ExecutionTiming;
import com.majortom.algorithms.core.runtime.ResourceUsage;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;

/** Converts a client record to a JSON-safe, versioned export payload. */
public final class ExecutionExportCodec {

    private static final int EXPORT_SCHEMA_VERSION = 3;
    private final ObjectMapper mapper;

    public ExecutionExportCodec(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    public Map<String, Object> encode(ClientExecutionRecord record) {
        Objects.requireNonNull(record, "record");
        return encode(record, record.recording().summary());
    }

    public Map<String, Object> encode(ClientExecutionRecord record, ExecutionSummary summary) {
        Objects.requireNonNull(record, "record");
        Objects.requireNonNull(summary, "summary");
        if (!record.recording().statistics().equals(summary.statistics())) {
            throw new IllegalArgumentException("Export summary must match the recorded statistics");
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("schemaVersion", EXPORT_SCHEMA_VERSION);
        payload.put("moduleId", record.moduleId());
        payload.put("operationId", record.operationId());
        payload.put("inputFingerprint", record.inputFingerprint());
        payload.put("result", encodeResult(record.result()));
        payload.put("eventStatistics", encodeStatistics(record.recording().statistics()));
        payload.put("visualFrameCount", record.visualFrameCount());
        payload.put("summary", encodeSummary(summary));
        payload.put("events", encodeEvents(record.recording().events()));
        return Map.copyOf(payload);
    }

    private Map<String, Object> encodeResult(ExecutionResult result) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", result.status().name());
        result.output().ifPresent(output -> payload.put("output", encodeValue(output)));
        result.failure().ifPresent(failure -> payload.put("failure", encodeFailure(failure)));
        return payload;
    }

    private Map<String, Object> encodeFailure(ExecutionFailure failure) {
        return Map.of(
                "code", failure.code(),
                "message", failure.message(),
                "exceptionType", failure.exceptionType());
    }

    private Map<String, Object> encodeSummary(ExecutionSummary summary) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (summary.inputSize().isPresent()) {
            payload.put("inputSize", summary.inputSize().getAsLong());
        } else {
            payload.put("inputSize", null);
        }
        payload.put("timing", encodeTiming(summary.timing()));
        payload.put("resources", encodeResources(summary.resources()));
        return payload;
    }

    private Map<String, Object> encodeTiming(ExecutionTiming timing) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventSpan", timing.eventSpan().toString());
        payload.put("totalDuration", optionalDuration(timing.totalDuration().orElse(null)));
        return payload;
    }

    private Map<String, Object> encodeResources(ResourceUsage resources) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("cpuTimeNanos", optionalLong(resources.cpuTimeNanos()));
        payload.put("peakMemoryBytes", optionalLong(resources.peakMemoryBytes()));
        payload.put("outputBytes", optionalLong(resources.outputBytes()));
        return payload;
    }

    private Map<String, Object> encodeStatistics(ExecutionStatistics statistics) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("totalEventCount", statistics.totalEventCount());
        payload.put("domainEventCount", statistics.domainEventCount());
        payload.put("lifecycleEventCount", statistics.lifecycleEventCount());
        payload.put("startedAt", optionalInstant(statistics.startedAt().orElse(null)));
        payload.put("endedAt", optionalInstant(statistics.endedAt().orElse(null)));
        payload.put("duration", statistics.duration().toString());
        payload.put("metrics", statistics.metrics());
        return payload;
    }

    private List<Map<String, Object>> encodeEvents(List<EventEnvelope> events) {
        List<Map<String, Object>> result = new ArrayList<>(events.size());
        for (EventEnvelope event : events) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("runId", event.runId());
            item.put("operationId", event.operationId());
            item.put("sequence", event.sequence());
            item.put("timestamp", event.timestamp().toString());
            item.put("source", event.source());
            item.put("eventType", stableEventType(event.operationId(), event.event()));
            item.put("payload", encodeValue(event.event()));
            result.add(item);
        }
        return List.copyOf(result);
    }

    private Object encodeValue(Object value) {
        try {
            JsonNode tree = mapper.valueToTree(value);
            return tree;
        } catch (IllegalArgumentException exception) {
            return String.valueOf(value);
        }
    }

    private String stableEventType(String operationId, ExecutionEvent event) {
        String name = event.getClass().getSimpleName();
        if (name.endsWith("Event")) {
            name = name.substring(0, name.length() - "Event".length());
        }
        if (name.isBlank()) {
            return operationId + ".event";
        }
        StringBuilder result = new StringBuilder(operationId).append('.');
        for (int index = 0; index < name.length(); index++) {
            char character = name.charAt(index);
            if (Character.isUpperCase(character) && index > 0) {
                result.append('-');
            }
            result.append(Character.toLowerCase(character));
        }
        return result.toString();
    }

    private String optionalInstant(Instant value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    private String optionalDuration(Duration value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    private Long optionalLong(OptionalLong value) {
        if (value.isEmpty()) {
            return null;
        }
        return value.getAsLong();
    }
}
