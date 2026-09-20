package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.core.metadata.StructureModule;
import com.majortom.algorithms.core.registry.ComponentRegistry;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.runtime.value.ValueAdapters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Owns type availability and committed per-module element type, independent of selectors. */
final class WorkbenchValueTypeCatalog {
    private final ComponentRegistry components;
    private final Map<String, String> selectedValueTypes;

    WorkbenchValueTypeCatalog(ComponentRegistry components, Map<String, String> selections) {
        this.components = components;
        this.selectedValueTypes = selections;
    }

    String displayName(String type) {
        Class<?> resolved;
        try { resolved = ValueAdapters.requireType(type); }
        catch (IllegalArgumentException unsupported) { return type; }
        String display = resolved.getSimpleName();
        if (!ValueAdapters.typeName(resolved).equals(display)) return resolved.getName();
        String key = "label.value_type." + display;
        String localized = I18N.text(key);
        return localized.equals(key) ? ValueAdapters.typeName(resolved) : localized;
    }

    boolean hasAlgorithmForAnySupportedType(String moduleId) {
        return !algorithmAvailableValueTypes(moduleId).isEmpty();
    }

    List<String> algorithmAvailableValueTypes(String moduleId) {
        return components.algorithms().stream()
                .filter(descriptor -> descriptor.module() == StructureModule.fromId(moduleId))
                .map(descriptor -> descriptor.valueType())
                .filter(ValueAdapters::supports)
                .filter(ValueAdapters::canReplay)
                .filter(type -> !StructureIds.TREE.equals(moduleId) || Comparable.class.isAssignableFrom(type))
                .map(ValueAdapters::typeName)
                .distinct().toList();
    }

    List<String> availableValueTypes(String moduleId) {
        return switch (moduleId) {
            case StructureIds.ARRAY, StructureIds.LINKED_LIST, StructureIds.STACK, StructureIds.QUEUE, StructureIds.GRAPH -> ValueAdapters.supportedTypeNames();
            case StructureIds.TREE -> ValueAdapters.supportedTypes().stream()
                    .filter(type -> Comparable.class.isAssignableFrom(type))
                    .map(ValueAdapters::typeName).toList();
            case StructureIds.STRING -> List.of(String.class.getSimpleName());
            default -> components.algorithmValueTypes(StructureModule.fromId(moduleId));
        };
    }

    List<ValueTypeOption> valueTypeOptions(List<String> available) {
        List<ValueTypeOption> options = new ArrayList<>();
        for (String type : ValueAdapters.supportedTypeNames()) {
            options.add(new ValueTypeOption(type, available.contains(type)));
        }
        return List.copyOf(options);
    }

    String selectedValueType(String moduleId) {
        String selected = selectedValueTypes.get(moduleId);
        if (selected != null) {
            return selected;
        }
        List<String> available = availableValueTypes(moduleId);
        if (available.isEmpty()) {
            return null;
        }
        selected = available.getFirst();
        selectedValueTypes.put(moduleId, selected);
        return selected;
    }

}
