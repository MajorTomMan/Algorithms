package com.majortom.algorithms.visualization.runtime.value;

import java.util.List;

/** JavaFX-free display declaration. Only fields explicitly returned here are inspected. */
public interface ValuePresenter<T> {
  Class<T> type();
  List<DisplayField<T>> fields();
}
