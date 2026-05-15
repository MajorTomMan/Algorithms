package com.majortom.algorithms.core.export;

import com.majortom.algorithms.core.base.BaseStructure;
import com.majortom.algorithms.core.graph.BaseGraph;
import com.majortom.algorithms.core.maze.BaseMaze;
import com.majortom.algorithms.core.sort.BaseSort;
import com.majortom.algorithms.core.tree.BaseTree;
import com.majortom.algorithms.core.tree.node.TreeNode;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;

import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 结构快照序列化工具。
 *
 * <p>执行记录中保存的是 Java 对象快照。导出、输入签名和跨模块比较需要稳定的普通数据结构，
 * 因此这里把排序、迷宫、图、树等 {@link BaseStructure} 转成 Map/List/基础类型。</p>
 */
public final class StructureSnapshotSerializer {

    /**
     * 工具类不允许实例化。
     */
    private StructureSnapshotSerializer() {
    }

    /**
     * 将结构快照转换为可导出的 Map。
     *
     * @param structure 结构快照
     * @return 由基础类型、Map 和 List 组成的序列化结构
     */
    public static Map<String, Object> serializeStructure(BaseStructure<?> structure) {
        Map<String, Object> root = new LinkedHashMap<>();
        if (structure == null) {
            root.put("type", "null");
            return root;
        }

        root.put("type", structure.getClass().getName());
        root.put("compareCount", structure.getCompareCount());
        root.put("actionCount", structure.getActionCount());

        if (structure instanceof BaseSort<?> sort) {
            root.put("data", serializeValue(sort.getData()));
            root.put("activeIndex", sort.getActiveIndex());
            root.put("compareIndex", sort.getCompareIndex());
            return root;
        }

        if (structure instanceof BaseMaze<?> maze) {
            root.put("rows", maze.getRows());
            root.put("cols", maze.getCols());
            root.put("generated", maze.isGenerated());
            root.put("data", serializeValue(maze.getData()));
            return root;
        }

        if (structure instanceof BaseGraph<?> graph) {
            List<Map<String, Object>> nodes = new ArrayList<>();
            graph.getGraph().nodes().forEach(node -> nodes.add(serializeNode(node)));
            List<Map<String, Object>> edges = new ArrayList<>();
            graph.getGraph().edges().forEach(edge -> edges.add(serializeEdge(edge)));
            root.put("graphId", graph.getGraph().getId());
            root.put("nodes", nodes);
            root.put("edges", edges);
            return root;
        }

        if (structure instanceof BaseTree<?> tree) {
            root.put("size", tree.size());
            root.put("height", tree.height());
            root.put("highlight", tree.getCurrentHighlight() == null ? null : tree.getCurrentHighlight().data);
            root.put("root", serializeTreeNode(tree.getRoot()));
            return root;
        }

        root.put("data", serializeValue(structure.getData()));
        return root;
    }

    /**
     * 根据结构内容生成输入签名。
     *
     * <p>签名用于判断两次运行是否可以比较：同模块且同输入签名的记录才会进入比较候选。</p>
     *
     * @param structure 输入结构
     * @return SHA-256 签名；算法不可用时退回 hashCode
     */
    public static String signatureFor(BaseStructure<?> structure) {
        String payload = serializeStructure(structure).toString();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : hash) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(payload.hashCode());
        }
    }

    /**
     * 序列化 GraphStream 节点。
     *
     * @param node GraphStream 节点
     * @return 节点快照
     */
    private static Map<String, Object> serializeNode(Node node) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", node.getId());
        payload.put("data", serializeValue(node.getAttribute("data")));
        payload.put("visited", node.hasAttribute("visited"));
        payload.put("uiClass", node.getAttribute("ui.class"));
        return payload;
    }

    /**
     * 序列化 GraphStream 边。
     *
     * @param edge GraphStream 边
     * @return 边快照
     */
    private static Map<String, Object> serializeEdge(Edge edge) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", edge.getId());
        payload.put("source", edge.getSourceNode().getId());
        payload.put("target", edge.getTargetNode().getId());
        payload.put("directed", edge.isDirected());
        payload.put("weight", edge.getAttribute("weight"));
        payload.put("uiClass", edge.getAttribute("ui.class"));
        return payload;
    }

    /**
     * 递归序列化树节点。
     *
     * @param node 树节点
     * @return 节点及其子树快照
     */
    private static Map<String, Object> serializeTreeNode(TreeNode<?> node) {
        if (node == null) {
            return null;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("data", serializeValue(node.data));
        payload.put("height", node.height);
        payload.put("subTreeCount", node.subTreeCount);
        payload.put("status", node.status);
        List<Object> children = new ArrayList<>();
        List<? extends TreeNode<?>> rawChildren = node.getChildren();
        if (rawChildren != null) {
            for (TreeNode<?> child : rawChildren) {
                children.add(serializeTreeNode(child));
            }
        }
        payload.put("children", children);
        return payload;
    }

    /**
     * 将任意值转换为导出友好的基础结构。
     *
     * @param value 原始值
     * @return 基础类型、List、Map 或字符串形式
     */
    private static Object serializeValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Character) {
            return value;
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            List<Object> list = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                list.add(serializeValue(Array.get(value, i)));
            }
            return list;
        }
        if (value instanceof Collection<?> collection) {
            List<Object> list = new ArrayList<>(collection.size());
            for (Object item : collection) {
                list.add(serializeValue(item));
            }
            return list;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> payload = new LinkedHashMap<>();
            map.forEach((key, item) -> payload.put(String.valueOf(key), serializeValue(item)));
            return payload;
        }
        return String.valueOf(value);
    }
}
