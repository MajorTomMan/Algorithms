package com.majortom.algorithms.core.io;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * 独立于结构与算法的 Jackson JSON 编解码器。
 *
 * <p>使用 Class 或 TypeReference 构造目标 JavaType，支持 record 和泛型容器。
 * 每个实例持有自己的 ObjectMapper 配置；InputStream / OutputStream
 * 所有权属于调用方，编解码时不会主动关闭传入流。
 *
 * <p>只执行 JSON ↔ Java 对象转换，不跨文件查找 @ValueId，
 * 也不解释 @GraphNodes / @GraphEdges。因此字符串城市 ID 不能自动成为 City 对象。
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
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    mapper.getFactory().disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
    mapper.getFactory().disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
    return mapper;
  }
}
