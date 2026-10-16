package com.majortom.algorithms.core.runtime;

/**
 * Runtime 与 UI 控制层之间的协作式许可协议。
 *
 * <p>RuntimeEventContext 在进入操作、发布领域事实、发布终止事件前分别调用
 * {@link #awaitStartPermission}、{@link #awaitDomainEventPermission}、
 * {@link #awaitCompletionPermission}。分开定义是为了确保
 * “暂停后单步允许一个领域单位”与“允许整次运行完成”不是同一件事。
 *
 * <p>默认 {@link #unrestricted()} 不暂停，可用于 CLI/无交互执行。
 * 不要在领域算法里直接调用 JavaFX 或显式挂起线程。
 */
public interface RunControl extends CancellationToken, ExecutionGate {
  /**
   * Waits until Runtime may enter the operation body.
   * A queued step permit may allow entry while paused, but is preserved for the first domain
   * execution unit.
   */
  default void awaitStartPermission(CancellationToken cancellationToken)
      throws InterruptedException {
    awaitPermission(cancellationToken);
  }

  /** Waits until a Structure/Observation/checkpoint execution unit may proceed. */
  default void awaitDomainEventPermission(CancellationToken cancellationToken)
      throws InterruptedException {
    awaitPermission(cancellationToken);
  }

  /**
   * Waits until Runtime may publish terminal completion.
   * Step permits do not resume a paused run, so completion remains blocked until Resume or
   * cancellation.
   */
  default void awaitCompletionPermission(CancellationToken cancellationToken)
      throws InterruptedException {
    awaitPermission(cancellationToken);
  }

  static RunControl unrestricted() {
    return new RunControl() {
      @Override
      public boolean isCancellationRequested() {
        return false;
      }

      @Override
      public void awaitPermission(CancellationToken cancellationToken) {}
    };
  }
}
