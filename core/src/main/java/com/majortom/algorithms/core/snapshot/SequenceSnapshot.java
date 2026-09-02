package com.majortom.algorithms.core.snapshot; import java.util.*; public record SequenceSnapshot<T>(List<T> values){public SequenceSnapshot{values=List.copyOf(Objects.requireNonNull(values));}}
