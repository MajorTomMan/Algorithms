package com.majortom.algorithms.core.runtime;

import com.majortom.algorithms.core.domain.execution.ExecutionLifecycleEvent;
import com.majortom.algorithms.core.domain.execution.RunPausedEvent;
import com.majortom.algorithms.core.domain.execution.RunResumedEvent;
import java.util.Objects;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * 单次运行的协作式暂停/继续/单步/取消控制器。
 *
 * <p>工作线程在许可点等待；UI 线程只更新控制状态，不直接改变算法执行栈。
 * ReentrantLock + Condition 保证跨线程可见性。暂停/恢复的生命周期事件
 * 在释放控制锁之后投递，避免事件接收器回调时死锁。
 *
 * <p>step 消耗一个领域许可，不是“播放一帧”：某个事实事件可以不产生可见帧。
 * 开始许可不会提前消耗第一步；完成许可也不会因为单步就越过暂停。
 * 取消会唤醒等待者，但领域代码必须继续到检查点才会观察到取消。
 */
public final class DefaultExecutionControl implements RunControl {
  private final ReentrantLock lock = new ReentrantLock();
  private final Condition stateChanged = lock.newCondition();
  private boolean paused;
  private boolean lifecycleTransitionInProgress;
  private int stepPermits;
  private volatile boolean cancelled;
  private Consumer<ExecutionLifecycleEvent> lifecycleSink;

  public void pause() {
    Consumer<ExecutionLifecycleEvent> sink;
    lock.lock();
    try {
      awaitLifecycleTransition();
      if (cancelled || paused) {
        return;
      }
      lifecycleTransitionInProgress = true;
      paused = true;
      stepPermits = 0;
      sink = lifecycleSink;
    } finally {
      lock.unlock();
    }

    RuntimeException failure = emitLifecycle(sink, new RunPausedEvent());

    lock.lock();
    try {
      if (failure != null && !cancelled) {
        paused = false;
      }
      lifecycleTransitionInProgress = false;
      stateChanged.signalAll();
    } finally {
      lock.unlock();
    }
    if (failure != null) {
      throw failure;
    }
  }

  public void resume() {
    Consumer<ExecutionLifecycleEvent> sink;
    lock.lock();
    try {
      awaitLifecycleTransition();
      if (cancelled || !paused) {
        return;
      }
      lifecycleTransitionInProgress = true;
      sink = lifecycleSink;
    } finally {
      lock.unlock();
    }

    RuntimeException failure = emitLifecycle(sink, new RunResumedEvent());

    lock.lock();
    try {
      if (failure == null && !cancelled) {
        paused = false;
        stepPermits = 0;
      }
      lifecycleTransitionInProgress = false;
      stateChanged.signalAll();
    } finally {
      lock.unlock();
    }
    if (failure != null) {
      throw failure;
    }
  }

  /** Allows exactly one subsequent execution gate through while remaining paused. */
  public void step() {
    lock.lock();
    try {
      awaitLifecycleTransition();
      if (cancelled || !paused) {
        return;
      }
      stepPermits++;
      stateChanged.signalAll();
    } finally {
      lock.unlock();
    }
  }

  public void cancel() {
    lock.lock();
    try {
      awaitLifecycleTransition();
      cancelled = true;
      paused = false;
      stepPermits = 0;
      stateChanged.signalAll();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public boolean isCancellationRequested() {
    return cancelled;
  }

  @Override
  public void awaitPermission(CancellationToken cancellationToken) throws InterruptedException {
    lock.lockInterruptibly();
    try {
      while ((paused || lifecycleTransitionInProgress) && stepPermits == 0 && !cancelled
          && !cancellationToken.isCancellationRequested()) {
        stateChanged.await();
      }
      if (paused && !lifecycleTransitionInProgress && stepPermits > 0) {
        stepPermits--;
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void awaitStartPermission(CancellationToken cancellationToken)
      throws InterruptedException {
    lock.lockInterruptibly();
    try {
      while ((lifecycleTransitionInProgress || (paused && stepPermits == 0)) && !cancelled
          && !cancellationToken.isCancellationRequested()) {
        stateChanged.await();
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void awaitDomainEventPermission(CancellationToken cancellationToken)
      throws InterruptedException {
    awaitPermission(cancellationToken);
  }

  @Override
  public void awaitCompletionPermission(CancellationToken cancellationToken)
      throws InterruptedException {
    lock.lockInterruptibly();
    try {
      while ((paused || lifecycleTransitionInProgress) && !cancelled
          && !cancellationToken.isCancellationRequested()) {
        stateChanged.await();
      }
    } finally {
      lock.unlock();
    }
  }

  void bindLifecycle(Consumer<ExecutionLifecycleEvent> lifecycleSink) {
    lock.lock();
    try {
      this.lifecycleSink = Objects.requireNonNull(lifecycleSink, "lifecycleSink");
    } finally {
      lock.unlock();
    }
  }

  void unbindLifecycle() {
    lock.lock();
    try {
      lifecycleSink = null;
    } finally {
      lock.unlock();
    }
  }

  private void awaitLifecycleTransition() {
    while (lifecycleTransitionInProgress) {
      stateChanged.awaitUninterruptibly();
    }
  }

  private static RuntimeException emitLifecycle(
      Consumer<ExecutionLifecycleEvent> sink, ExecutionLifecycleEvent event) {
    if (sink == null) {
      return null;
    }
    try {
      sink.accept(event);
      return null;
    } catch (RuntimeException exception) {
      return exception;
    }
  }
}
