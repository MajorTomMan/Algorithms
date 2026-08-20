package com.majortom.algorithms.visualization.runtime;

import com.majortom.algorithms.core.runtime.ResourceSampler;
import com.majortom.algorithms.core.runtime.ResourceUsage;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.OptionalLong;

/** Best-effort JVM-local resource sampler for a desktop execution worker. */
final class LocalResourceSampler implements ResourceSampler {

    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    private final Runtime runtime = Runtime.getRuntime();
    private long workerThreadId = -1L;
    private long startCpuTimeNanos = -1L;
    private long peakMemoryBytes = -1L;
    private ResourceUsage usage = ResourceUsage.empty();
    private boolean running;

    @Override
    public synchronized void start() {
        workerThreadId = Thread.currentThread().threadId();
        startCpuTimeNanos = currentCpuTimeNanos();
        peakMemoryBytes = usedMemoryBytes();
        usage = ResourceUsage.empty();
        running = true;
    }

    @Override
    public synchronized void stop() {
        if (!running) {
            return;
        }
        usage = currentUsage();
        running = false;
    }

    @Override
    public synchronized ResourceUsage sample() {
        if (running) {
            usage = currentUsage();
        }
        return usage;
    }

    private ResourceUsage currentUsage() {
        long memory = usedMemoryBytes();
        if (memory >= 0L) {
            if (peakMemoryBytes < memory) {
                peakMemoryBytes = memory;
            }
        }

        OptionalLong cpuTime = OptionalLong.empty();
        long currentCpu = currentCpuTimeNanos();
        if (startCpuTimeNanos >= 0L && currentCpu >= startCpuTimeNanos) {
            cpuTime = OptionalLong.of(currentCpu - startCpuTimeNanos);
        }

        OptionalLong peakMemory = OptionalLong.empty();
        if (peakMemoryBytes >= 0L) {
            peakMemory = OptionalLong.of(peakMemoryBytes);
        }
        return ResourceUsage.of(cpuTime, peakMemory, OptionalLong.empty());
    }

    private long currentCpuTimeNanos() {
        if (!threadBean.isCurrentThreadCpuTimeSupported()) {
            return -1L;
        }
        try {
            long value = threadBean.getThreadCpuTime(workerThreadId);
            if (value >= 0L) {
                return value;
            }
        } catch (UnsupportedOperationException | SecurityException ignored) {
            // Resource sampling is best effort and must not affect execution.
        }
        return -1L;
    }

    private long usedMemoryBytes() {
        try {
            long value = runtime.totalMemory() - runtime.freeMemory();
            if (value >= 0L) {
                return value;
            }
        } catch (RuntimeException ignored) {
            // Resource sampling is best effort and must not affect execution.
        }
        return -1L;
    }
}
