package com.majortom.algorithms.telemetry.runtime;

import com.majortom.algorithms.telemetry.api.TelemetryCapabilities;
import com.majortom.algorithms.telemetry.api.TelemetryMetricDescriptor;
import com.majortom.algorithms.telemetry.api.TelemetryProfile;
import com.majortom.algorithms.telemetry.api.TelemetrySample;
import com.majortom.algorithms.telemetry.api.TelemetryScopeId;
import com.majortom.algorithms.telemetry.api.TelemetrySessionId;
import com.majortom.algorithms.telemetry.api.TelemetrySessionState;
import com.majortom.algorithms.telemetry.api.TelemetryValue;
import com.majortom.algorithms.telemetry.probe.TelemetryProbe;
import com.majortom.algorithms.telemetry.probe.TelemetryProbeSession;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JavaFX-neutral control plane for execution telemetry.
 *
 * <p>One sampler drives every registered probe in a session. Probes own measurement semantics;
 * this framework owns scope identity, lifecycle, sampling, immutable snapshots, and failure
 * isolation. A broken probe is ignored rather than allowed to affect the observed algorithm.</p>
 */
public final class DefaultTelemetryFramework implements TelemetryFramework, AutoCloseable {
  public static final long DEFAULT_SAMPLE_INTERVAL_MILLIS = 100L;
  public static final int DEFAULT_MAXIMUM_SAMPLES = 4_096;

  private final List<TelemetryProbe> probes;
  private final Map<String, TelemetryMetricDescriptor> descriptors;
  private final TelemetryCapabilities capabilities;
  private final ScheduledExecutorService sampler;
  private final AtomicLong sequence = new AtomicLong();
  private final long sampleIntervalMillis;
  private final int maximumSamples;

  public DefaultTelemetryFramework(List<? extends TelemetryProbe> probes) {
    this(probes, DEFAULT_SAMPLE_INTERVAL_MILLIS);
  }

  public DefaultTelemetryFramework(List<? extends TelemetryProbe> probes, long sampleIntervalMillis) {
    this(probes, sampleIntervalMillis, DEFAULT_MAXIMUM_SAMPLES);
  }

  public DefaultTelemetryFramework(List<? extends TelemetryProbe> probes, long sampleIntervalMillis,
      int maximumSamples) {
    if (maximumSamples < 2) {
      throw new IllegalArgumentException("maximumSamples must be at least 2");
    }
    this.maximumSamples = maximumSamples;
    if (sampleIntervalMillis < 1L) {
      throw new IllegalArgumentException("sampleIntervalMillis must be positive");
    }
    this.sampleIntervalMillis = sampleIntervalMillis;
    this.probes = List.copyOf(Objects.requireNonNull(probes, "probes"));
    this.descriptors = discoverDescriptors(this.probes);
    this.capabilities = discoverCapabilities(this.probes);
    this.sampler = Executors.newSingleThreadScheduledExecutor(new SamplerThreadFactory());
  }

  @Override
  public TelemetryCapabilities capabilities() {
    return capabilities;
  }

  @Override
  public TelemetrySession begin(TelemetryScopeId scope) {
    TelemetrySessionId id = new TelemetrySessionId(sequence.incrementAndGet(), scope);
    Session session = new Session(id);
    session.start();
    return session;
  }

  @Override
  public void close() {
    sampler.shutdownNow();
  }

  private final class Session implements TelemetrySession {
    private final TelemetrySessionId id;
    private final Object lock = new Object();
    private final List<TelemetryProbeSession> probeSessions = new ArrayList<>();
    private final List<TelemetrySample> samples = new ArrayList<>();
    private long startedAtNanos = System.nanoTime();

    private TelemetrySessionState state = TelemetrySessionState.CREATED;
    private ScheduledFuture<?> samplingTask;
    private TelemetryProfile terminalProfile;

    private Session(TelemetrySessionId id) {
      this.id = id;
    }

    private void start() {
      for (TelemetryProbe probe : probes) {
        try {
          TelemetryProbeSession opened = probe.open(id);
          if (opened != null) {
            probeSessions.add(opened);
          }
        } catch (RuntimeException ignored) {
          // Probe failure must never prevent execution from starting.
        }
      }
      synchronized (lock) {
        state = TelemetrySessionState.RECORDING;
      }
      attachCurrentThread();
      sampleNow();
      samplingTask = sampler.scheduleAtFixedRate(
          this::sampleSafely,
          sampleIntervalMillis,
          sampleIntervalMillis,
          TimeUnit.MILLISECONDS);
    }

    @Override
    public TelemetrySessionId id() {
      return id;
    }

    @Override
    public TelemetrySessionState state() {
      synchronized (lock) {
        return state;
      }
    }

    @Override
    public boolean attachCurrentThread() {
      synchronized (lock) {
        if (state != TelemetrySessionState.RECORDING) {
          return false;
        }
      }
      boolean attached = false;
      for (TelemetryProbeSession probe : probeSessions) {
        try {
          attached |= probe.attachCurrentThread();
        } catch (RuntimeException ignored) {
          // Attribution support is probe-specific and best-effort.
        }
      }
      return attached;
    }

    @Override
    public void rebase() {
      synchronized (lock) {
        if (state != TelemetrySessionState.RECORDING) {
          throw new IllegalStateException("Cannot rebase telemetry session from " + state);
        }
        for (TelemetryProbeSession probe : probeSessions) {
          try {
            probe.rebase();
          } catch (RuntimeException ignored) {
            // Probe-specific reset failure degrades that probe only.
          }
        }
        startedAtNanos = System.nanoTime();
        samples.clear();
        Map<String, TelemetryValue> values = readProbeValues(false);
        appendSampleLocked(0L, values);
      }
    }

    @Override
    public void sampleNow() {
      synchronized (lock) {
        if (state != TelemetrySessionState.RECORDING) {
          return;
        }
        Map<String, TelemetryValue> values = readProbeValues(false);
        appendSampleLocked(elapsedNanos(), values);
      }
    }

    @Override
    public TelemetryProfile snapshot() {
      synchronized (lock) {
        if (terminalProfile != null) {
          return terminalProfile;
        }
        Map<String, TelemetryValue> summary = readProbeValues(true);
        return new TelemetryProfile(
            id,
            state,
            capabilities,
            true,
            elapsedNanos(),
            descriptors,
            summary,
            samples);
      }
    }

    @Override
    public void complete() {
      finish(TelemetrySessionState.COMPLETED);
    }

    @Override
    public void fail() {
      finish(TelemetrySessionState.FAILED);
    }

    @Override
    public void cancel() {
      finish(TelemetrySessionState.CANCELLED);
    }

    private void finish(TelemetrySessionState terminalState) {
      List<TelemetryProbeSession> toClose;
      synchronized (lock) {
        if (state.executionEnded()) {
          return;
        }
        if (state != TelemetrySessionState.RECORDING) {
          throw new IllegalStateException("Cannot finish telemetry session from " + state);
        }
        Map<String, TelemetryValue> sampled = readProbeValues(false);
        appendSampleLocked(elapsedNanos(), sampled);
        Map<String, TelemetryValue> summary = readProbeValues(true);
        state = terminalState;
        terminalProfile = new TelemetryProfile(
            id,
            terminalState,
            capabilities,
            true,
            elapsedNanos(),
            descriptors,
            summary,
            samples);
        toClose = List.copyOf(probeSessions);
      }
      ScheduledFuture<?> task = samplingTask;
      if (task != null) {
        task.cancel(false);
      }
      for (TelemetryProbeSession probe : toClose) {
        try {
          probe.close();
        } catch (RuntimeException ignored) {
          // Closing diagnostics must not alter execution outcome.
        }
      }
    }

    private void sampleSafely() {
      try {
        sampleNow();
      } catch (RuntimeException ignored) {
        // Sampling is observational and is never allowed to fail the execution.
      }
    }

    private Map<String, TelemetryValue> readProbeValues(boolean summary) {
      Map<String, TelemetryValue> merged = new LinkedHashMap<>();
      for (TelemetryProbeSession probe : probeSessions) {
        try {
          Map<String, TelemetryValue> values = summary ? probe.summary() : probe.sample();
          if (values == null) {
            continue;
          }
          for (Map.Entry<String, TelemetryValue> entry : values.entrySet()) {
            if (!descriptors.containsKey(entry.getKey()) || entry.getValue() == null) {
              continue;
            }
            merged.put(entry.getKey(), entry.getValue());
          }
        } catch (RuntimeException ignored) {
          // A single probe can degrade independently.
        }
      }
      return Map.copyOf(merged);
    }

    private void appendSampleLocked(long elapsedNanos, Map<String, TelemetryValue> values) {
      if (values.isEmpty()) {
        return;
      }
      TelemetrySample next = new TelemetrySample(elapsedNanos, values);
      if (!samples.isEmpty()) {
        TelemetrySample previous = samples.getLast();
        if (previous.elapsedNanos() == next.elapsedNanos() && previous.values().equals(next.values())) {
          return;
        }
      }
      if (samples.size() >= maximumSamples) compactSamplesLocked();
      samples.add(next);
    }

    /** Bounded temporal decimation: keep the first and most recent readings.
     * Terminal summary metrics remain independent from the sampled curve. */
    private void compactSamplesLocked() {
      List<TelemetrySample> reduced = new ArrayList<>((samples.size() + 2) / 2 + 1);
      for (int index = 0; index < samples.size(); index += 2) {
        reduced.add(samples.get(index));
      }
      TelemetrySample last = samples.getLast();
      if (reduced.getLast() != last) reduced.add(last);
      if (reduced.size() >= maximumSamples) reduced.remove(1);
      samples.clear();
      samples.addAll(reduced);
    }

    private long elapsedNanos() {
      return Math.max(0L, System.nanoTime() - startedAtNanos);
    }
  }

  private static Map<String, TelemetryMetricDescriptor> discoverDescriptors(List<TelemetryProbe> probes) {
    Map<String, TelemetryMetricDescriptor> result = new LinkedHashMap<>();
    for (TelemetryProbe probe : probes) {
      Objects.requireNonNull(probe, "probe");
      for (TelemetryMetricDescriptor descriptor : probe.descriptors()) {
        TelemetryMetricDescriptor previous = result.putIfAbsent(descriptor.id(), descriptor);
        if (previous != null && !previous.equals(descriptor)) {
          throw new IllegalArgumentException("Conflicting telemetry metric descriptor: " + descriptor.id());
        }
      }
    }
    return Map.copyOf(result);
  }

  private static TelemetryCapabilities discoverCapabilities(List<TelemetryProbe> probes) {
    TelemetryCapabilities merged = TelemetryCapabilities.EMPTY;
    for (TelemetryProbe probe : probes) {
      merged = merged.merge(probe.capabilities());
    }
    return merged;
  }

  private static final class SamplerThreadFactory implements ThreadFactory {
    @Override
    public Thread newThread(Runnable runnable) {
      Thread thread = new Thread(runnable, "algorithms-telemetry-sampler");
      thread.setDaemon(true);
      return thread;
    }
  }
}
