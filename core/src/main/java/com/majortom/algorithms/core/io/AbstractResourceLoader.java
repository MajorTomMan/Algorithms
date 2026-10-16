package com.majortom.algorithms.core.io;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

/**
 * 跨模块共享的 classpath JSON/CSV 资源读取模板，不包含业务对象关联规则。
 *
 * <p>AlgorithmResourceLoader 等模块门面提供 Class 锚点；
 * 本类负责路径校验、打开 classpath InputStream、按请求类型调用编解码器并关闭流。
 * JSON 的 TypeReference 表示完整目标（例如 List&lt;City&gt;），
 * CSV 的 TypeReference 表示单行类型，最终返回 List。
 *
 * <p><b>安全边界：</b>拒绝 ..、.、重复斜杠和反斜杠等无效资源路径；
 * 只读取打包资源而非任意主机文件。加载的对象如何组成 Region、Graph、
 * 如何通过业务 ID 关联实体，属于调用方而不是通用数据层。
 */
public abstract class AbstractResourceLoader implements ResourceLoader {
  private final Class<?> resourceAnchor;

  protected AbstractResourceLoader(Class<?> resourceAnchor) {
    this.resourceAnchor = Objects.requireNonNull(resourceAnchor, "resourceAnchor");
  }

  @Override
  public final <T> T load(String resourcePath, Class<T> type) {
    return read(resourcePath, "JSON",
        new JsonDataCodec<>(Objects.requireNonNull(type, "type")));
  }

  @Override
  public final <T> T load(String resourcePath, TypeReference<T> type) {
    return read(resourcePath, "JSON",
        new JsonDataCodec<>(Objects.requireNonNull(type, "type")));
  }

  @Override
  public final <T> List<T> loadCsv(String resourcePath, Class<T> rowType) {
    return read(resourcePath, "CSV",
        new CsvDataReader<>(Objects.requireNonNull(rowType, "rowType")));
  }

  @Override
  public final <T> List<T> loadCsv(String resourcePath, TypeReference<T> rowType) {
    return read(resourcePath, "CSV",
        new CsvDataReader<>(Objects.requireNonNull(rowType, "rowType")));
  }

  private <T> T read(String resourcePath, String format, DataReader<T> reader) {
    String path = normalizedPath(resourcePath);
    try (InputStream input = resourceAnchor.getResourceAsStream("/" + path)) {
      if (input == null)
        throw new IllegalArgumentException(format + " resource not found: " + path);
      T value = reader.read(input);
      if (value == null)
        throw new IllegalArgumentException(format + " resource must not be null: " + path);
      return value;
    } catch (IOException exception) {
      throw new IllegalArgumentException("Cannot read " + format + " resource: " + path, exception);
    }
  }

  private static String normalizedPath(String resourcePath) {
    Objects.requireNonNull(resourcePath, "resourcePath");
    String path = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
    if (path.isBlank() || path.indexOf('\\') >= 0)
      throw new IllegalArgumentException("Invalid classpath resource path: " + resourcePath);
    for (String part : path.split("/", -1)) {
      if (part.isEmpty() || part.equals(".") || part.equals(".."))
        throw new IllegalArgumentException("Invalid classpath resource path: " + resourcePath);
    }
    return path;
  }
}
