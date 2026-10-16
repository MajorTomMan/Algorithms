package com.majortom.algorithms.algorithm.discovery;

import com.majortom.algorithms.core.registry.ComponentRegistry;
import com.majortom.algorithms.structure.discovery.StructureDiscovery;
import java.util.Objects;

/**
 * 将结构发现与算法发现组合为工作台可消费的注册目录。
 *
 * <p>默认 discover() 采用懒加载 Holder，在当前类加载器下只构造一次注册表，
 * 避免每次选择算法时扫描 classpath。带 ClassLoader 的重载可构建独立目录，
 * 用于明确的运行环境隔离。
 *
 * <p>结构发现定义“有哪些可执行的数据结构能力”，算法发现定义
 * “哪些算法支持哪种结构和类型”；最终只依赖 core 的 ComponentRegistry
 * 承担查询，不把 JavaFX 引入算法模块。
 */
public final class ComponentDiscovery {
  private ComponentDiscovery() {}

  public static ComponentRegistry discover() {
    return Holder.DEFAULT;
  }

  public static ComponentRegistry discover(ClassLoader classLoader) {
    Objects.requireNonNull(classLoader, "classLoader");
    return new ComponentRegistry(new StructureDiscovery().discover(classLoader),
        new AlgorithmDiscovery().discover(classLoader));
  }

  private static ClassLoader defaultClassLoader() {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    if (classLoader == null) {
      classLoader = ComponentDiscovery.class.getClassLoader();
    }
    return classLoader;
  }

  private static final class Holder {
    private static final ComponentRegistry DEFAULT = discover(defaultClassLoader());
  }
}
