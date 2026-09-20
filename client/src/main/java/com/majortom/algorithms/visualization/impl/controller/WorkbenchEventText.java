package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.domain.execution.ExecutionLifecycleEvent;
import com.majortom.algorithms.core.event.algorithm.AlgorithmEvent;
import com.majortom.algorithms.core.event.structure.TreeStructureEvent;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Stateless descriptions and categories shared by event inspector and timeline. */
final class WorkbenchEventText {
    private static final DateTimeFormatter EVENT_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private WorkbenchEventText() {}

    static String eventDisplayName(EventEnvelope envelope) {
        String simple = envelope.event().getClass().getSimpleName();
        return simple.replaceAll("([a-z0-9])([A-Z])", "$1 $2").toUpperCase(Locale.ROOT);
    }

    static String describeCurrentStep(EventEnvelope envelope) {
        Object event = envelope.event();
        if (event instanceof AlgorithmEvent.Visited visited) {
            return "TARGET  " + formatReference(visited.ref());
        }
        if (event instanceof AlgorithmEvent.Examined examined) {
            return "FROM    " + formatReference(examined.fromRef()) + "\nTO      " + formatReference(examined.toRef());
        }
        if (event instanceof AlgorithmEvent.Compared compared) {
            return "LEFT    " + formatReference(compared.leftRef()) + "\nRIGHT   " + formatReference(compared.rightRef());
        }
        if (event instanceof AlgorithmEvent.Matched matched) {
            return "INDEX   " + matched.index() + "\nLENGTH  " + matched.length();
        }
        if (event instanceof AlgorithmEvent.Fallback fallback) {
            return "PATTERN " + fallback.fromIndex() + " → " + fallback.toIndex();
        }
        if (event instanceof AlgorithmEvent.Backtracked backtracked) {
            return "TARGET  " + formatReference(backtracked.ref());
        }
        if (event instanceof AlgorithmEvent.PathTraced pathTraced) {
            return "TARGET  " + formatReference(pathTraced.ref());
        }
        if (event instanceof TreeStructureEvent.NodeInserted inserted) {
            return "NODE    #" + inserted.nodeId() + "\nVALUE   " + inserted.value();
        }
        if (event instanceof TreeStructureEvent.NodeRemoved removed) {
            return "NODE    #" + removed.nodeId() + "\nVALUE   " + removed.value();
        }
        if (event instanceof TreeStructureEvent.ValueChanged changed) {
            return "NODE    #" + changed.nodeId() + "\nVALUE   " + changed.previousValue() + " → " + changed.value();
        }
        if (event instanceof TreeStructureEvent.LeftChanged changed) {
            return "NODE    #" + changed.nodeId() + "\nLEFT    " + formatIdChange(changed.previousChildId(), changed.childId());
        }
        if (event instanceof TreeStructureEvent.RightChanged changed) {
            return "NODE    #" + changed.nodeId() + "\nRIGHT   " + formatIdChange(changed.previousChildId(), changed.childId());
        }
        if (event instanceof TreeStructureEvent.RootChanged changed) {
            return "ROOT    " + formatIdChange(changed.previousRootId(), changed.rootId());
        }
        if (event instanceof TreeStructureEvent.ChildInserted inserted) {
            return "PARENT  #" + inserted.parentId() + "\nCHILD   #" + inserted.childId() + "  @" + inserted.index();
        }
        if (event instanceof TreeStructureEvent.ChildRemoved removed) {
            return "PARENT  #" + removed.parentId() + "\nCHILD   #" + removed.childId() + "  @" + removed.index();
        }
        String text = envelope.event().toString();
        if (text.length() > 120) text = text.substring(0, 117) + "...";
        return text;
    }

    static String describeEventEnvelope(EventEnvelope envelope) {
        return String.format(Locale.ROOT,
                "Sequence     %d%nTime         %s%nKind         %s%nSource       %s%n%n%s",
                envelope.sequence(), EVENT_TIME_FORMATTER.format(envelope.timestamp().atZone(ZoneId.systemDefault())),
                eventCategory(envelope), envelope.source(), describeCurrentStep(envelope));
    }

    static String eventCategory(EventEnvelope envelope) {
        Object event = envelope.event();
        if (event instanceof com.majortom.algorithms.core.event.structure.StructureEvent) return "Structure Event";
        if (event instanceof AlgorithmEvent) return "Algorithm Event";
        if (event instanceof ExecutionLifecycleEvent) return "Runtime Event";
        return "Execution Event";
    }

    static String eventDotClass(EventEnvelope envelope) {
        if (envelope.event() instanceof AlgorithmEvent) return "event-dot-observation";
        if (envelope.event() instanceof com.majortom.algorithms.core.event.structure.StructureEvent) return "event-dot-structure";
        if (envelope.event() instanceof ExecutionLifecycleEvent) return "event-dot-runtime";
        return "event-dot-idle";
    }

    static String formatReference(AlgorithmEvent.Reference reference) {
        if (reference instanceof AlgorithmEvent.EntityRef entity) return entity.domain().toUpperCase(Locale.ROOT) + " #" + entity.id();
        if (reference instanceof AlgorithmEvent.IndexRef index) return index.source() + "[" + index.index() + "]";
        if (reference instanceof AlgorithmEvent.CoordinateRef cell) return "(" + cell.row() + ", " + cell.column() + ")";
        if (reference instanceof AlgorithmEvent.ValueRef value) return String.valueOf(value.value());
        return String.valueOf(reference);
    }

    static String formatIdChange(Long previous, Long next) {
        String left;
        if (previous == null) {
            left = "none";
        } else {
            left = "#" + previous;
        }
        String right;
        if (next == null) {
            right = "none";
        } else {
            right = "#" + next;
        }
        return left + " → " + right;
    }

}
