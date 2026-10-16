package com.majortom.algorithms.algorithm.io;

import com.fasterxml.jackson.core.type.TypeReference;
import com.majortom.algorithms.core.io.AbstractResourceLoader;
import com.majortom.algorithms.core.io.ResourceLoader;
import java.util.List;

/**
 * 算法模块面向 classpath 的 JSON/CSV 读取门面。
 *
 * <p>委托 core.io.AbstractResourceLoader 处理路径安全、流及反序列化。
 * JSON 的 TypeReference 描述整个返回对象，CSV 描述单条记录。
 * 这个 API 只转换格式，不理解 Graph、Road 或道路端点的业务 ID。
 *
 * <p>推荐在 @AlgorithmInitialize 阶段准备业务实体：
 * 读取 Cities / Roads → 按业务规则关联端点 → 创建 Region →
 * Graph.from(...) → GraphStructure.initialize(snapshot)。
 * 不要向通用加载器添加与某个算法绑定的图工厂。
 */
public final class AlgorithmResourceLoader {
  private static final ResourceLoader LOADER = new AbstractResourceLoader(AlgorithmResourceLoader.class) {
  };

  private AlgorithmResourceLoader() {
  }

  /** Read JSON into the requested Java type. */
  public static <T> T load(String path, Class<T> type) {
    return LOADER.load(path, type);
  }

  /** Read JSON into a parameterized Java type. */
  public static <T> T load(String path, TypeReference<T> type) {
    return LOADER.load(path, type);
  }

  /** Read CSV with a header row into List<T>. */
  public static <T> List<T> loadCsv(String path, Class<T> rowType) {
    return LOADER.loadCsv(path, rowType);
  }

  /** For generic CSV row types, e.g. Map<String, String>. */
  public static <T> List<T> loadCsv(String path, TypeReference<T> rowType) {
    return LOADER.loadCsv(path, rowType);
  }
}
