package com.majortom.algorithms.core.registry;

import com.majortom.algorithms.core.annotation.Structure;
import com.majortom.algorithms.core.metadata.StructureModule;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * 单次算法注册的不可变描述符，也是注解元数据与实际调用之间的桥梁。
 *
 * <p>发现器在启动时解析 @AlgorithmEntry 与可选的 @AlgorithmInitialize；
 * 注册表保存 Method 与类型签名，而不是共享一个可变算法实例。
 * {@link #invoke(Object)} 每次新建实例，按“初始化 → 执行入口”顺序调用，
 * 且两步共用相同的结构对象；初始化异常会阻止入口执行。
 *
 * <p>反射错误保留业务异常原始 cause，供 ExecutionRuntime 转换为失败结果。
 * 此类不负责线程、控制许可、图拓扑或 UI。只有在运行上下文内调用 invoke，
 * 领域事件才会进入可回放的权威事件流。
 */
public record AlgorithmDescriptor(String id, String name, AlgorithmTypeSignature typeSignature,
    Class<?> structureContract, Class<?> implementation, Method entryPoint,
    Method initializer) {
  public AlgorithmDescriptor {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(name, "name");
    if (name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    Objects.requireNonNull(typeSignature, "typeSignature");
    Objects.requireNonNull(structureContract, "structureContract");
    Objects.requireNonNull(implementation, "implementation");
    Objects.requireNonNull(entryPoint, "entryPoint");
  }

  /** Preserve the constructor used by algorithms with no preparation hook. */
  public AlgorithmDescriptor(String id, String name, AlgorithmTypeSignature typeSignature,
      Class<?> structureContract, Class<?> implementation, Method entryPoint) {
    this(id, name, typeSignature, structureContract, implementation, entryPoint, null);
  }

  /** Whether this algorithm prepares its data before running the entry method. */
  public boolean hasInitializer() {
    return initializer != null;
  }

  public AlgorithmKey key() {
    return AlgorithmKey.of(this);
  }

  public StructureModule module() {
    Structure metadata = structureContract.getAnnotation(Structure.class);
    if (metadata == null) {
      throw new RegistrationException(
          "Algorithm structure contract is missing @Structure metadata: "
          + structureContract.getName());
    }
    return metadata.module();
  }

  public Object invoke(Object structure) {
    Objects.requireNonNull(structure, "structure");
    if (!structureContract.isInstance(structure)) {
      throw new IllegalArgumentException("Algorithm " + id + " requires structure "
          + structureContract.getName() + " but received " + structure.getClass().getName());
    }
    Object algorithm = newInstance();
    // Both lifecycle methods share the same instance and Structure object.
    // This happens inside the caller's execution context, so changes and events
    // emitted by initialization participate in normal execution/playback.
    if (initializer != null)
      invokeMethod(initializer, algorithm, structure, "initialization");
    return invokeMethod(entryPoint, algorithm, structure, "entry");
  }

  private Object invokeMethod(Method method, Object instance, Object structure, String phase) {
    try {
      return method.invoke(instance, structure);
    } catch (IllegalAccessException exception) {
      throw new RegistrationException("Unable to access Algorithm " + phase + " "
          + implementation.getName() + "#" + method.getName(), exception);
    } catch (InvocationTargetException exception) {
      Throwable cause = exception.getCause();
      if (cause instanceof RuntimeException runtime) throw runtime;
      if (cause instanceof Error error) throw error;
      throw new RegistrationException("Algorithm " + phase + " failed: "
          + implementation.getName() + "#" + method.getName(), cause);
    }
  }

  private Object newInstance() {
    try {
      return implementation.getDeclaredConstructor().newInstance();
    } catch (ReflectiveOperationException exception) {
      throw new RegistrationException(
          "Unable to instantiate Algorithm " + id + " using " + implementation.getName(),
          exception);
    }
  }
}
