package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.core.metadata.StructureModule;
import com.majortom.algorithms.core.registry.ComponentRegistry;
import com.majortom.algorithms.visualization.algorithm.AlgorithmCatalog;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.module.WorkbenchModuleDefinition;
import com.majortom.algorithms.visualization.navigation.FamilyEntry;
import com.majortom.algorithms.visualization.navigation.FamilyNavigator;
import com.majortom.algorithms.visualization.runtime.value.ValueAdapters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

/** Owns family navigation and available algorithm lists; module transitions stay in the Workbench. */
final class WorkbenchModuleNavigation {
    private final ComponentRegistry components;
    private final List<WorkbenchModuleDefinition> moduleDefinitions;
    private final FamilyNavigator familyNavigator;
    private final Consumer<WorkbenchModuleDefinition> onSelectFamily;
    private final Function<String, String> selectedValueType;

    WorkbenchModuleNavigation(ComponentRegistry components, List<WorkbenchModuleDefinition> definitions,
            FamilyNavigator navigator, Consumer<WorkbenchModuleDefinition> onSelectFamily,
            Function<String, String> selectedValueType) {
        this.components = components;
        this.moduleDefinitions = definitions;
        this.familyNavigator = navigator;
        this.onSelectFamily = onSelectFamily;
        this.selectedValueType = selectedValueType;
    }

    void install() {
        familyNavigator.setEntries(moduleDefinitions.stream()
                .map(definition -> familyEntry(definition, false, () -> onSelectFamily.accept(definition)))
                .toList());
    }

    private FamilyEntry familyEntry(
            WorkbenchModuleDefinition definition,
            boolean disabled,
            Runnable action) {
        return new FamilyEntry(
                definition.id(),
                definition.navigation().glyph(),
                javafx.beans.binding.Bindings.createStringBinding(
                        () -> familyName(definition.id()),
                        I18N.localeProperty()),
                disabled,
                action);
    }

    void syncSelection(String moduleId) {
        familyNavigator.setSelectedFamily(moduleId);
    }

    String familyName(String moduleId) {
        return I18N.text("label.structure." + moduleId).toUpperCase(Locale.ROOT);
    }

    String familyIndex(String moduleId) {
        for (int index = 0; index < moduleDefinitions.size(); index++) {
            if (moduleDefinitions.get(index).id().equals(moduleId)) {
                return "%02d".formatted(index + 1);
            }
        }
        return "--";
    }

    /**
     * Algorithms exposed by a family in the workspace rail.
     *
     * <p>This deliberately describes the whole family, not only the currently active
     * structure variant.  A family such as Tree can start on General Tree while its
     * algorithms live on the AVL variant.  Using the active controller's list here
     * made the rail change availability during a module transition and could bounce
     * the workspace back to Structure before the target variant was selected.</p>
     */
    List<AlgorithmNavigationItem> algorithmNavigationItems(String moduleId) {
        List<String> algorithmIds = new ArrayList<>();
        if (StructureIds.MAZE.equals(moduleId)) {
            algorithmIds.addAll(AlgorithmCatalog.forWorkbenchModule(moduleId));
        } else {
            String selected = selectedValueType.apply(moduleId);
            if (selected == null) {
                return List.of();
            }
            Class<?> valueType = ValueAdapters.requireType(selected);
            if (!ValueAdapters.canReplay(valueType)) return List.of();
            algorithmIds.addAll(AlgorithmCatalog.forWorkbenchModule(moduleId, valueType));
            List<String> registered = components.algorithms(StructureModule.fromId(moduleId), valueType)
                    .stream().map(com.majortom.algorithms.core.registry.AlgorithmDescriptor::id).toList();
            algorithmIds.removeIf(id -> !registered.contains(id));
        }
        return algorithmIds.stream().distinct().map(AlgorithmNavigationItem::new).toList();
    }

    private void addAlgorithmsForAllTypes(List<String> target, String family, String excludedPrefix) {
        for (String valueType : components.algorithmValueTypes(StructureModule.fromId(family))) {
            for (String algorithmId : components.algorithmIds(StructureModule.fromId(family), valueType)) {
                if (excludedPrefix == null || !algorithmId.startsWith(excludedPrefix)) {
                    target.add(algorithmId);
                }
            }
        }
    }

    void updateAvailability(boolean algorithmMode, boolean running,
            java.util.function.Predicate<String> hasAlgorithmForAnySupportedType) {
        if (familyNavigator == null) {
            return;
        }
        for (WorkbenchModuleDefinition definition : moduleDefinitions) {
            boolean unavailableInAlgorithm = algorithmMode
                    && algorithmNavigationItems(definition.id()).isEmpty()
                    && !hasAlgorithmForAnySupportedType.test(definition.id());
            familyNavigator.setFamilyDisabled(definition.id(), running || unavailableInAlgorithm);
        }
    }

}
