package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.core.metadata.StructureModule;
import com.majortom.algorithms.core.metadata.ValueMetadata;
import com.majortom.algorithms.core.registry.AlgorithmTypeSignature;
import com.majortom.algorithms.core.registry.ComponentRegistry;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.runtime.value.ValueAdapters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 工作台值类型的筛选与显示策略层，独立于 ComboBox 和业务实体构造。
 *
 * <p>候选类型来自 ValueAdapters，适配结构的范围取自 @ValueType.value；
 * 展示名称取 ValueMetadata.name（例如 City → “城市”）；
 * 底层保留 Java 类名作为选择 ID，避免重名和本地化导致算法类型误匹配。
 *
 * <p>树模块额外要求 Comparable；哈希表同时要求键/值类型兼容；
 * 图模块允许 Object 通用算法与精确实体算法共存。
 * 此类不修改结构状态：最终切换由各控制器执行。
 */
final class WorkbenchValueTypeCatalog {
    private final ComponentRegistry components;
    private final Map<String, String> selectedValueTypes;

    WorkbenchValueTypeCatalog(ComponentRegistry components, Map<String, String> selections) {
        this.components = components;
        this.selectedValueTypes = selections;
    }

    /**
     * 显示名称优先级：@ValueType.name → 本地化内置类型 → Java 类型名。
     * 显示名称绝不可回写成 Class<?> 注册键。
     */
    String displayName(String type) {
        Class<?> resolved;
        try { resolved = ValueAdapters.requireType(type); }
        catch (IllegalArgumentException unsupported) { return type; }
        var metadata = ValueMetadata.describe(resolved);
        if (metadata.declared()) return metadata.name();
        String display = resolved.getSimpleName();
        if (!ValueAdapters.typeName(resolved).equals(display)) return resolved.getName();
        String key = "label.value_type." + display;
        String localized = I18N.text(key);
        return localized.equals(key) ? ValueAdapters.typeName(resolved) : localized;
    }

    boolean hasAlgorithmForAnySupportedType(String moduleId) {
        StructureModule module = StructureModule.fromId(moduleId);
        return components.algorithmTypeSignatures(module).stream()
                .anyMatch(signature -> supportsAlgorithmSignature(moduleId, signature));
    }

    List<String> algorithmAvailableValueTypes(String moduleId) {
        StructureModule module = StructureModule.fromId(moduleId);
        return components.algorithmTypeSignatures(module).stream()
                .filter(signature -> signature.arity() == 1)
                .filter(signature -> supportsAlgorithmSignature(moduleId, signature))
                .map(AlgorithmTypeSignature::primaryType)
                .map(ValueAdapters::typeName)
                .distinct()
                .toList();
    }

    /**
     * 依据当前模块结构契约过滤候选类型。
     * 仅修改 UI 列表不够：对应控制器的 supportedValueTypes
     * 还必须采用相同的结构契约以阻止非法程序化切换。
     */
    List<String> availableValueTypes(String moduleId) {
        return switch (moduleId) {
            case StructureIds.ARRAY, StructureIds.LINKED_LIST, StructureIds.STACK,
                    StructureIds.QUEUE, StructureIds.GRAPH -> compatibleTypes(moduleId).stream()
                            .map(ValueAdapters::typeName)
                            .toList();
            case StructureIds.TREE -> compatibleTypes(moduleId).stream()
                    .filter(type -> Comparable.class.isAssignableFrom(type))
                    .map(ValueAdapters::typeName)
                    .toList();
            case StructureIds.STRING -> List.of(String.class.getSimpleName());
            default -> components.algorithmTypeSignatures(StructureModule.fromId(moduleId)).stream()
                    .filter(signature -> signature.arity() == 1)
                    .map(AlgorithmTypeSignature::primaryType)
                    .filter(ValueAdapters::supports)
                    .filter(type -> ValueMetadata.describe(type)
                            .supports(components.requireStructure(moduleId).contract()))
                    .map(ValueAdapters::typeName)
                    .distinct()
                    .toList();
        };
    }

    private List<Class<?>> compatibleTypes(String moduleId) {
        return ValueAdapters.supportedTypesFor(components.requireStructure(moduleId).contract());
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

    private boolean supportsAlgorithmSignature(
            String moduleId, AlgorithmTypeSignature signature) {
        if (StructureIds.HASH.equals(moduleId) && signature.arity() != 2) {
            return false;
        }
        if (!StructureIds.HASH.equals(moduleId) && signature.arity() != 1) {
            return false;
        }
        for (Class<?> type : signature.types()) {
            if (!ValueAdapters.supports(type) || !ValueAdapters.canReplay(type)
                    || !ValueMetadata.describe(type)
                            .supports(components.requireStructure(moduleId).contract())) {
                return false;
            }
        }
        return !StructureIds.TREE.equals(moduleId)
                || Comparable.class.isAssignableFrom(signature.primaryType());
    }
}
