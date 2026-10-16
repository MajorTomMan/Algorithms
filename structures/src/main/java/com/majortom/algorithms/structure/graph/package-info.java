/**
 * 图拓扑的真实数据结构实现与业务对象映射。
 * 
 *  <p>GraphStructure 是算法所需的能力契约；Graph 持有 ID 化的多重图，
 *  GraphObjectMapper 把带注解的普通业务模型转换为拓扑快照。
 *  Graph.initialize 先完整校验再替换状态，使用一次 Reset 发布事实。
 * 
 *  <p>Vertex/Edge long ID 属于结构，不等于业务 @ValueId；
 *  本包既不读取 JSON 文件，也不决定 JavaFX 可视化坐标。
 */
package com.majortom.algorithms.structure.graph;
