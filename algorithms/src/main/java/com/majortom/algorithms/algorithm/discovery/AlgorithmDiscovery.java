package com.majortom.algorithms.algorithm.discovery;

import com.majortom.algorithms.core.annotation.Algorithm;
import com.majortom.algorithms.core.annotation.AlgorithmEntry;
import com.majortom.algorithms.core.annotation.AlgorithmInitialize;
import com.majortom.algorithms.core.annotation.Structure;
import com.majortom.algorithms.core.metadata.ComponentNames;
import com.majortom.algorithms.core.registry.AlgorithmDescriptor;
import com.majortom.algorithms.core.registry.AlgorithmTypeSignature;
import com.majortom.algorithms.core.registry.FrameworkClassScanner;
import com.majortom.algorithms.core.registry.RegistrationException;
import com.majortom.algorithms.core.registry.RegistrationValidator;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * @Algorithm 发现与注册元数据构造入口，不承担运行时执行。
 *
 * <p>扫描 algorithms 包中的实现，读取 @Algorithm 声明，
 * 定位唯一 @AlgorithmEntry 和可选的唯一 @AlgorithmInitialize；
 * 注册时验证结构契约与有序泛型类型签名。
 *
 * <p>最终返回 AlgorithmDescriptor，由 ComponentRegistry 统一索引。
 * 这里仅缓存 Method 描述，不预先执行数据初始化，也不创建工作线程。
 * 实际运行时由 AlgorithmDescriptor.invoke() 新建算法实例、初始化并执行。
 *
 * <p>如果源码未编译、未在扫描根包、没有 @Algorithm，或当前工作台
 * 的顶点类型与 @Algorithm.types 不一致，就不会出现在对应算法列表。
 */
public final class AlgorithmDiscovery {
  public static final String ROOT_PACKAGE = "com.majortom.algorithms.algorithm";

  private final FrameworkClassScanner scanner;

  public AlgorithmDiscovery() {
    this(new FrameworkClassScanner());
  }

  AlgorithmDiscovery(FrameworkClassScanner scanner) {
    this.scanner = Objects.requireNonNull(scanner, "scanner");
  }

  public List<AlgorithmDescriptor> discover(ClassLoader classLoader) {
    Objects.requireNonNull(classLoader, "classLoader");
    List<AlgorithmDescriptor> descriptors = new ArrayList<>();
    for (Class<?> implementation : scanner.scan(ROOT_PACKAGE, classLoader)) {
      Algorithm annotation = implementation.getAnnotation(Algorithm.class);
      if (annotation == null) {
        continue;
      }

      Class<?> structureContract = annotation.structure();
      if (structureContract.getAnnotation(Structure.class) == null) {
        throw new RegistrationException(
            "Algorithm structure contract is missing @Structure metadata: "
                + structureContract.getName());
      }

      Method entryPoint = findEntryPoint(implementation);
      Method initializer = findInitializer(implementation);
      AlgorithmDescriptor descriptor = new AlgorithmDescriptor(
          annotation.id(),
          ComponentNames.resolve(annotation.name(), implementation),
          AlgorithmTypeSignature.of(annotation.types()),
          structureContract,
          implementation,
          entryPoint,
          initializer);
      validateTypeSignature(descriptor);
      descriptors.add(descriptor);
    }

    descriptors.sort(
        Comparator.comparing((AlgorithmDescriptor descriptor) -> descriptor.module().id())
            .thenComparing(descriptor -> descriptor.structureContract().getName())
            .thenComparing(descriptor -> descriptor.typeSignature().stableName())
            .thenComparing(AlgorithmDescriptor::id));
    RegistrationValidator.validateUniqueAlgorithmKeys(descriptors);
    return List.copyOf(descriptors);
  }

  void validateTypeSignature(AlgorithmDescriptor descriptor) {
    if (descriptor.structureContract().getTypeParameters().length == 0) {
      return;
    }

    List<Class<?>> entryTypes = StructureTypeSignatureResolver.resolve(
        descriptor.entryPoint().getGenericParameterTypes()[0],
        descriptor.structureContract());
    if (!entryTypes.equals(descriptor.typeSignature().types())) {
      throw new RegistrationException(
          "Algorithm annotation types " + descriptor.typeSignature().types()
              + " do not match ordered entry Structure types " + entryTypes
              + " for " + descriptor.implementation().getName());
    }
  }

  /** An initializer is optional but must be unique; validate non-public declarations early. */
  private Method findInitializer(Class<?> implementation) {
    for (Method method : implementation.getDeclaredMethods()) {
      if (method.isAnnotationPresent(AlgorithmInitialize.class)
          && !Modifier.isPublic(method.getModifiers())) {
        throw new RegistrationException(
            "Algorithm @AlgorithmInitialize method must be public: " + method);
      }
    }

    List<Method> initializers = new ArrayList<>();
    for (Method method : implementation.getMethods()) {
      if (method.isAnnotationPresent(AlgorithmInitialize.class))
        initializers.add(method);
    }
    if (initializers.size() > 1) {
      throw new RegistrationException("Algorithm " + implementation.getName()
          + " must expose at most one @AlgorithmInitialize method, found "
          + initializers.size());
    }
    return initializers.isEmpty() ? null : initializers.getFirst();
  }

  private Method findEntryPoint(Class<?> implementation) {
    List<Method> entries = new ArrayList<>();
    for (Method method : implementation.getMethods()) {
      if (method.getAnnotation(AlgorithmEntry.class) != null) {
        entries.add(method);
      }
    }
    if (entries.size() != 1) {
      throw new RegistrationException("Algorithm " + implementation.getName()
          + " must expose exactly one @AlgorithmEntry method, found " + entries.size());
    }
    return entries.getFirst();
  }
}
