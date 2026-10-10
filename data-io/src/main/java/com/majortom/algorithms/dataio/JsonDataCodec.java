package com.majortom.algorithms.dataio;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * JSON reader/writer for ordinary records, classes, and parameterized data models.
 * Input and output streams remain owned by the caller.
 */
public final class JsonDataCodec<T> implements DataCodec<T> {
  private final ObjectMapper mapper;
  private final JavaType dataType;

  public JsonDataCodec(Class<T> type) {
    Objects.requireNonNull(type, "type");
    mapper = newMapper();
    dataType = mapper.constructType(type);
  }

  public JsonDataCodec(TypeReference<T> type) {
    Objects.requireNonNull(type, "type");
    mapper = newMapper();
    dataType = mapper.getTypeFactory().constructType(type);
  }

  @Override
  public T read(InputStream input) throws IOException {
    Objects.requireNonNull(input, "input");
    return mapper.readValue(input, dataType);
  }

  @Override
  public void write(T value, OutputStream output) throws IOException {
    Objects.requireNonNull(value, "value");
    Objects.requireNonNull(output, "output");
    mapper.writerWithDefaultPrettyPrinter().writeValue(output, value);
  }

  private static ObjectMapper newMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    mapper.getFactory().disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
    mapper.getFactory().disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
    return mapper;
  }
}
