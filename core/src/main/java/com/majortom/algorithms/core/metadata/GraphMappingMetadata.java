package com.majortom.algorithms.core.metadata;

import com.majortom.algorithms.core.annotation.GraphEdges;
import com.majortom.algorithms.core.annotation.GraphNodes;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 将 @GraphNodes / @GraphEdges 解析成可复用访问器的纯元数据层。
 *
 * <p>本类只读取属性描述及类型约束，不创建图顶点、边或 JavaFX 节点。
 * GraphObjectMapper 消费这些描述符，先收集节点，再解析边端点，
 * 最后使用 Graph.fromSnapshot 创建实际结构。
 *
 * <p>ClassValue 按运行时实体类型缓存元数据；边成员访问器又按
 * EdgeProperties 属性组合缓存，避免多次扫描同类不同配置的边。
 * 此处分离“怎么读取业务对象”和“怎么存储图拓扑”，
 * 使业务 JSON/CSV 格式完全独立于结构实现。
 */
public final class GraphMappingMetadata {
  private GraphMappingMetadata() {}

  private static final ClassValue<Descriptor> CONTAINERS = new ClassValue<>() {
    @Override protected Descriptor computeValue(Class<?> type) {
      List<AnnotatedMembers.Accessor> nodes = AnnotatedMembers.marked(type, GraphNodes.class);
      List<EdgeCollection> edges = AnnotatedMembers.marked(type, GraphEdges.class).stream()
          .map(member -> new EdgeCollection(member,
              EdgeProperties.from(member.annotation(GraphEdges.class))))
          .toList();
      return new Descriptor(nodes, edges);
    }
  };

  // The same edge class may be used by differently annotated edge collections.
  private static final ClassValue<Map<EdgeProperties, EdgeDescriptor>> EDGE_TYPES =
      new ClassValue<>() {
        @Override protected Map<EdgeProperties, EdgeDescriptor> computeValue(Class<?> type) {
          return new ConcurrentHashMap<>();
        }
      };

  public static Descriptor describe(Class<?> type) {
    return CONTAINERS.get(Objects.requireNonNull(type, "type"));
  }

  public record Descriptor(List<AnnotatedMembers.Accessor> nodes,
                           List<EdgeCollection> edges) {
    public Descriptor {
      nodes = List.copyOf(nodes);
      edges = List.copyOf(edges);
    }
  }

  public record EdgeCollection(AnnotatedMembers.Accessor member, EdgeProperties properties) {
    public EdgeCollection {
      Objects.requireNonNull(member, "member");
      Objects.requireNonNull(properties, "properties");
    }

    /** Resolve edge values through the mapping declared on this collection. */
    public EdgeValues read(Object edge) {
      Objects.requireNonNull(edge, "edge");
      Class<?> type = edge.getClass();
      EdgeDescriptor descriptor = EDGE_TYPES.get(type).computeIfAbsent(
          properties, fields -> resolve(type, fields));
      return descriptor.read(edge);
    }
  }

  public record EdgeProperties(String from, String to, String weight, String bidirectional) {
    public EdgeProperties {
      Objects.requireNonNull(from, "from");
      Objects.requireNonNull(to, "to");
      Objects.requireNonNull(weight, "weight");
      Objects.requireNonNull(bidirectional, "bidirectional");
      if (from.isBlank() || to.isBlank())
        throw new IllegalArgumentException("Graph edge endpoints must not be blank");
    }

    public static EdgeProperties from(GraphEdges annotation) {
      Objects.requireNonNull(annotation, "annotation");
      return new EdgeProperties(annotation.from(), annotation.to(),
          annotation.weight(), annotation.bidirectional());
    }
  }

  /** Validated properties of an edge, without graph-specific objects. */
  public record EdgeValues(Object from, Object to, double weight, boolean bidirectional) {}

  private record EdgeDescriptor(AnnotatedMembers.Accessor from,
                                AnnotatedMembers.Accessor to,
                                AnnotatedMembers.Accessor weight,
                                AnnotatedMembers.Accessor bidirectional,
                                EdgeProperties names) {
    EdgeValues read(Object edge) {
      Object source = Objects.requireNonNull(from.read(edge), "Null edge source");
      Object target = Objects.requireNonNull(to.read(edge), "Null edge target");
      double cost = 1.0;
      if (weight != null) {
        Object raw = weight.read(edge);
        if (!(raw instanceof Number number))
          throw new IllegalArgumentException(
              "Graph edge weight must be numeric: " + names.weight());
        cost = number.doubleValue();
        if (!Double.isFinite(cost))
          throw new IllegalArgumentException("Non-finite graph edge weight");
      }
      boolean twoWay = false;
      if (bidirectional != null) {
        Object raw = bidirectional.read(edge);
        if (!(raw instanceof Boolean flag))
          throw new IllegalArgumentException("Graph edge bidirectional flag must be boolean");
        twoWay = flag;
      }
      return new EdgeValues(source, target, cost, twoWay);
    }
  }

  private static EdgeDescriptor resolve(Class<?> type, EdgeProperties fields) {
    return new EdgeDescriptor(
        AnnotatedMembers.named(type, fields.from()),
        AnnotatedMembers.named(type, fields.to()),
        fields.weight().isBlank() ? null : AnnotatedMembers.named(type, fields.weight()),
        fields.bidirectional().isBlank()
            ? null : AnnotatedMembers.named(type, fields.bidirectional()),
        fields);
  }
}
