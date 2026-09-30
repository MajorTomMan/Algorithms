package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.core.registry.AlgorithmTypeSignature;
import com.majortom.algorithms.visualization.algorithm.AlgorithmCatalog;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.module.WorkbenchModuleDefinition;
import com.majortom.algorithms.visualization.navigation.FamilyEntry;
import com.majortom.algorithms.visualization.navigation.FamilyNavigator;
import com.majortom.algorithms.visualization.runtime.value.ValueAdapters;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

/** Owns family navigation and available algorithm lists; module transitions stay in the Workbench. */
final class WorkbenchModuleNavigation {
    private final List<WorkbenchModuleDefinition> moduleDefinitions;
    private final FamilyNavigator familyNavigator;
    private final Consumer<WorkbenchModuleDefinition> onSelectFamily;
    private final Function<String, AlgorithmTypeSignature> selectedTypeSignature;

    WorkbenchModuleNavigation(
            List<WorkbenchModuleDefinition> definitions,
            FamilyNavigator navigator,
            Consumer<WorkbenchModuleDefinition> onSelectFamily,
            Function<String, AlgorithmTypeSignature> selectedTypeSignature) {
        this.moduleDefinitions = definitions;
        this.familyNavigator = navigator;
        this.onSelectFamily = onSelectFamily;
        this.selectedTypeSignature = selectedTypeSignature;
    }

    void install() {
        familyNavigator.setEntries(moduleDefinitions.stream()
                .map(definition -> familyEntry(
                        definition, false, () -> onSelectFamily.accept(definition)))
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

    List<AlgorithmNavigationItem> algorithmNavigationItems(String moduleId) {
        if (StructureIds.MAZE.equals(moduleId)) {
            return AlgorithmCatalog.forWorkbenchModule(moduleId).stream()
                    .distinct()
                    .map(AlgorithmNavigationItem::new)
                    .toList();
        }

        AlgorithmTypeSignature signature = selectedTypeSignature.apply(moduleId);
        if (signature == null || signature.types().stream().anyMatch(type -> !ValueAdapters.canReplay(type))) {
            return List.of();
        }
        return AlgorithmCatalog.forWorkbenchModule(moduleId, signature).stream()
                .distinct()
                .map(AlgorithmNavigationItem::new)
                .toList();
    }

    void updateAvailability(
            boolean algorithmMode,
            boolean running,
            java.util.function.Predicate<String> hasAlgorithmForAnySupportedType) {
        if (familyNavigator == null) {
            return;
        }
        for (WorkbenchModuleDefinition definition : moduleDefinitions) {
            boolean unavailableInAlgorithm = algorithmMode
                    && algorithmNavigationItems(definition.id()).isEmpty()
                    && !hasAlgorithmForAnySupportedType.test(definition.id());
            familyNavigator.setFamilyDisabled(
                    definition.id(), running || unavailableInAlgorithm);
        }
    }
}
