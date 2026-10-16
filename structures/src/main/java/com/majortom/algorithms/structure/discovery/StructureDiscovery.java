package com.majortom.algorithms.structure.discovery;

import com.majortom.algorithms.core.annotation.Structure;
import com.majortom.algorithms.core.metadata.ComponentNames;
import com.majortom.algorithms.core.registry.FrameworkClassScanner;
import com.majortom.algorithms.core.registry.RegistrationException;
import com.majortom.algorithms.core.registry.RegistrationValidator;
import com.majortom.algorithms.core.registry.StructureDescriptor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * @Structure 的发现入口，和 AlgorithmDiscovery 共用 FrameworkClassScanner。
 *
 * <p>只扫描结构能力接口：声明了稳定 ID 和实现类的契约成为实际注册项，
 * 只有 module 元数据的家族接口只负责类型分类，不单独实例化。
 * 二者必须成对声明，否则视为注册错误。
 *
 * <p>结果经 RegistrationValidator 校验并排序，
 * ComponentRegistry 才把它们与算法注册表组合。
 * 结构模块不引用 client，确保模型在命令行/无 UI 环境也能构造。
 */
public final class StructureDiscovery {
  public static final String ROOT_PACKAGE = "com.majortom.algorithms.structure";

  private final FrameworkClassScanner scanner;

  public StructureDiscovery() {
    this(new FrameworkClassScanner());
  }

  StructureDiscovery(FrameworkClassScanner scanner) {
    this.scanner = Objects.requireNonNull(scanner, "scanner");
  }

  public List<StructureDescriptor> discover(ClassLoader classLoader) {
    Objects.requireNonNull(classLoader, "classLoader");
    List<StructureDescriptor> descriptors = new ArrayList<>();
    for (Class<?> contract : scanner.scan(ROOT_PACKAGE, classLoader)) {
      Structure annotation = contract.getAnnotation(Structure.class);
      if (annotation == null) {
        continue;
      }
      if (!contract.isInterface()) {
        throw new RegistrationException(
            "@Structure must be declared on a capability interface: " + contract.getName());
      }
      boolean hasId = !annotation.id().isBlank();
      boolean hasImplementation = annotation.implementation() != Void.class;
      if (!hasId && !hasImplementation) {
        continue;
      }
      if (hasId != hasImplementation) {
        throw new RegistrationException(
            "Concrete @Structure registration requires both id and implementation: "
            + contract.getName());
      }
      Class<?> implementation = annotation.implementation();
      StructureDescriptor descriptor = new StructureDescriptor(annotation.id(),
          ComponentNames.resolve(annotation.name(), implementation), annotation.module(), contract,
          implementation);
      descriptors.add(descriptor);
    }
    descriptors.sort(Comparator.comparing(StructureDescriptor::id));
    RegistrationValidator.validateUniqueStructureIds(descriptors);
    return List.copyOf(descriptors);
  }
}
