/**
 * 注解描述符与反射访问器的共享解释层。
 * 
 *  <p>ValueMetadata 管理 @ValueType、@ValueId、@ValueField，
 *  GraphMappingMetadata 管理 @GraphNodes、@GraphEdges。
 *  元数据只规定如何识别字段，不执行 JSON 加载和结构变更。
 * 
 *  <p>这种分层保证 client 展示与 structures 映射使用相同的业务属性语义，
 *  又不会让 core 依赖这两个上层模块。
 */
package com.majortom.algorithms.core.metadata;
