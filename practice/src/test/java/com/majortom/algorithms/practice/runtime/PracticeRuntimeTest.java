package com.majortom.algorithms.practice.runtime;

import com.majortom.algorithms.core.problem.ProblemSource;
import com.majortom.algorithms.practice.runtime.worker.PracticeWorkerLauncher;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PracticeRuntimeTest {
    private static final String FIXTURE_ROOT = "com.majortom.algorithms.practice.runtime.fixture";
    private final PracticeProblemRegistry registry = PracticeProblemRegistry.discover(
            getClass().getClassLoader(), List.of(FIXTURE_ROOT));

    @Test
    void discoversConfiguredProblemRootsAndRunsResolvedEntryDirectly() {
        assertEquals(3, registry.problems().size());
        assertEquals("Search Insert Problem",
                registry.require(ProblemSource.LOCAL, "search-insert-probe").name());
        Object result = new PracticeRunner().run(
                registry.require(ProblemSource.LOCAL, "search-insert-probe"),
                new Integer[] {1, 3, 5, 6}, 5);
        assertEquals(2, result);
    }

    @Test
    void jdiWorkerProducesImmutableRecordingThatSurvivesWorkerExit() {
        var recording = new PracticeWorkerLauncher().run(
                registry.require(ProblemSource.LOCAL, "climbing-stairs-probe"), Duration.ofSeconds(4), 5);
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
