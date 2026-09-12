package com.majortom.algorithms.practice.runtime;

import com.majortom.algorithms.practice.runtime.model.ProblemSource;
import com.majortom.algorithms.practice.runtime.worker.PracticeWorkerLauncher;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class PracticeRuntimeTest {
    private final PracticeProblemRegistry registry = PracticeProblemRegistry.discover(getClass().getClassLoader());

    @Test
    void discoversProblemsAndRunsResolvedEntryDirectly() {
        assertTrue(registry.problems().size() >= 3);
        Object result = new PracticeRunner().run(
                registry.require(ProblemSource.LEETCODE, "35"),
                new Integer[] {1, 3, 5, 6}, 5);
        assertEquals(2, result);
    }

    @Test
    void jdiWorkerProducesImmutableRecordingThatSurvivesWorkerExit() {
        var recording = new PracticeWorkerLauncher().run(
                registry.require(ProblemSource.LEETCODE, "70"), Duration.ofSeconds(4), 5);
        assertFalse(recording.timedOut());
        assertEquals(0, recording.exitCode());
        assertEquals(8, recording.result());
        assertFalse(recording.frames().isEmpty());
        var first = recording.frames().getFirst();
        assertTrue(first.lineNumber() > 0);
        assertEquals(first, recording.frames().getFirst());
    }

    @Test
    void timeoutKillsOnlyWorkerAndKeepsAlreadyRecordedFrames() {
        var recording = new PracticeWorkerLauncher().run(
                registry.require(ProblemSource.LOCAL, "infinite-loop-probe"), Duration.ofMillis(350));
        assertTrue(recording.timedOut());
        assertEquals(124, recording.exitCode());
        assertFalse(recording.frames().isEmpty());
    }
}
