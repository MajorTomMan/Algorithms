package com.majortom.algorithms.visualization.render.presentation;

import com.majortom.algorithms.visualization.render.api.PresentationModelSource;
import com.majortom.algorithms.visualization.render.api.PresentationSnapshotContext;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/** Thread-safe bridge for producers that already expose immutable presentation models. */
public final class MutablePresentationModelSource<M> implements PresentationModelSource<M> {
  private final AtomicReference<M> current;

  public MutablePresentationModelSource(M initial) {
    current = new AtomicReference<>(Objects.requireNonNull(initial, "initial"));
  }

  public void publish(M model) {
    current.set(Objects.requireNonNull(model, "model"));
  }

  @Override
  public M snapshot(PresentationSnapshotContext context) {
    Objects.requireNonNull(context, "context");
    return current.get();
  }
}
