package com.majortom.algorithms.practice.runtime.worker;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.majortom.algorithms.telemetry.api.TelemetryCapabilities;
import com.majortom.algorithms.telemetry.api.TelemetryMetricDescriptor;
import com.majortom.algorithms.telemetry.api.TelemetryProfile;
import com.majortom.algorithms.telemetry.api.TelemetrySample;
import com.majortom.algorithms.telemetry.api.TelemetrySessionId;
import com.majortom.algorithms.telemetry.api.TelemetrySessionState;
import com.majortom.algorithms.telemetry.api.TelemetryValue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Host-to-child invocation uses Java serialization of host-owned input only.
 * Child-to-host results and metrics are JSON; the host never deserializes arbitrary
 * object graphs emitted by exercise code.
 */
final class WorkerCodec {
  private static final ObjectMapper JSON = newMapper();
  private static final int MAX_INVOCATION_BYTES = 1_048_576;
  private static final int MAX_JSON_BYTES = 4_194_304;

  private WorkerCodec() {}

  private static ObjectMapper newMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    mapper.getFactory().setStreamReadConstraints(StreamReadConstraints.builder()
        .maxNestingDepth(64).maxStringLength(1_000_000).maxNumberLength(1_000).build());
    return mapper;
  }

  static String encode(Object value) {
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
        output.writeObject(value);
      }
      if (bytes.size() > MAX_INVOCATION_BYTES) {
        throw new IllegalArgumentException("Practice worker invocation exceeds size limit");
      }
      return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
    } catch (IOException exception) {
      throw new IllegalArgumentException("Practice worker arguments must be serializable", exception);
    }
  }

  /** Invoked only by the child to read the host-created WorkerInvocation. */
  static WorkerInvocation decode(String encoded) {
    try {
      byte[] bytes = Base64.getUrlDecoder().decode(encoded);
      if (bytes.length > MAX_INVOCATION_BYTES) {
        throw new IllegalArgumentException("Practice worker invocation exceeds size limit");
      }
      try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
        input.setObjectInputFilter(info -> {
          if (info.depth() > 32 || info.references() > 10_000 || info.arrayLength() > 100_000) {
            return ObjectInputFilter.Status.REJECTED;
          }
          return ObjectInputFilter.Status.UNDECIDED;
        });
        Object value = input.readObject();
        if (!(value instanceof WorkerInvocation invocation)) {
          throw new IllegalArgumentException("Invalid worker invocation");
        }
        return invocation;
      }
    } catch (IOException | ClassNotFoundException exception) {
      throw new IllegalArgumentException("Unable to decode Practice worker invocation", exception);
    }
  }

  static String encodeResult(Object value) {
    return encodeJson(value);
  }

  static Object decodeResult(String encoded) {
    try {
      return JSON.readValue(decodeJson(encoded), Object.class);
    } catch (IOException exception) {
      throw new IllegalArgumentException("Unable to decode practice result JSON", exception);
    }
  }

  static String encodeProfile(TelemetryProfile profile) {
    Map<String, Object> values = new LinkedHashMap<>();
    values.put("sessionId", profile.sessionId());
    values.put("state", profile.state());
    values.put("capabilities", profile.capabilities());
    values.put("timingRepresentative", profile.timingRepresentative());
    values.put("durationNanos", profile.durationNanos());
    values.put("descriptors", profile.descriptors());
    values.put("summary", metricValues(profile.summary()));
    List<Map<String, Object>> samples = new ArrayList<>();
    for (TelemetrySample sample : profile.samples()) {
      samples.add(Map.of("elapsedNanos", sample.elapsedNanos(), "values", metricValues(sample.values())));
    }
    values.put("samples", samples);
    return encodeJson(values);
  }

  static TelemetryProfile decodeProfile(String encoded) {
    try {
      JsonNode json = JSON.readTree(decodeJson(encoded));
      TelemetrySessionId sessionId = JSON.treeToValue(json.required("sessionId"), TelemetrySessionId.class);
      TelemetrySessionState state = TelemetrySessionState.valueOf(json.required("state").asText());
      TelemetryCapabilities capabilities = JSON.treeToValue(
          json.required("capabilities"), TelemetryCapabilities.class);
      Map<String, TelemetryMetricDescriptor> descriptors = new LinkedHashMap<>();
      var fields = json.required("descriptors").fields();
      while (fields.hasNext()) {
        var field = fields.next();
        descriptors.put(field.getKey(), JSON.treeToValue(field.getValue(), TelemetryMetricDescriptor.class));
      }
      List<TelemetrySample> samples = new ArrayList<>();
      for (JsonNode sample : json.required("samples")) {
        samples.add(new TelemetrySample(sample.required("elapsedNanos").asLong(),
            readMetricValues(sample.required("values"))));
      }
      return new TelemetryProfile(sessionId, state, capabilities,
          json.required("timingRepresentative").asBoolean(),
          json.required("durationNanos").asLong(), descriptors,
          readMetricValues(json.required("summary")), samples);
    } catch (IOException | RuntimeException exception) {
      throw new IllegalArgumentException("Unable to decode practice telemetry JSON", exception);
    }
  }

  private static Map<String, Object> metricValues(Map<String, TelemetryValue> source) {
    Map<String, Object> result = new LinkedHashMap<>();
    for (var entry : source.entrySet()) {
      TelemetryValue value = entry.getValue();
      if (value instanceof TelemetryValue.LongValue number) {
        result.put(entry.getKey(), Map.of("kind", "long", "value", number.value()));
      } else if (value instanceof TelemetryValue.DoubleValue number) {
        result.put(entry.getKey(), Map.of("kind", "double", "value", number.value()));
      }
    }
    return result;
  }

  private static Map<String, TelemetryValue> readMetricValues(JsonNode source) {
    Map<String, TelemetryValue> result = new LinkedHashMap<>();
    var fields = source.fields();
    while (fields.hasNext()) {
      var field = fields.next();
      JsonNode value = field.getValue();
      TelemetryValue parsed = switch (value.required("kind").asText()) {
        case "long" -> TelemetryValue.of(value.required("value").asLong());
        case "double" -> TelemetryValue.of(value.required("value").asDouble());
        default -> throw new IllegalArgumentException("Unknown telemetry number kind");
      };
      result.put(field.getKey(), parsed);
    }
    return result;
  }

  private static String encodeJson(Object value) {
    try {
      byte[] bytes = JSON.writeValueAsBytes(value);
      if (bytes.length > MAX_JSON_BYTES) {
        throw new IllegalArgumentException("Practice worker JSON exceeds size limit");
      }
      return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    } catch (IOException exception) {
      throw new IllegalArgumentException("Unable to encode Practice worker JSON", exception);
    }
  }

  private static byte[] decodeJson(String encoded) {
    byte[] bytes = Base64.getUrlDecoder().decode(encoded);
    if (bytes.length > MAX_JSON_BYTES) {
      throw new IllegalArgumentException("Practice worker JSON exceeds size limit");
    }
    return bytes;
  }
}
