/**
 * 算法发现与结构发现的组合入口。
 * 
 *  <p>AlgorithmDiscovery 查找 @Algorithm 的唯一 @AlgorithmEntry 和可选
 *  @AlgorithmInitialize，验证结构契约与泛型类型签名后形成 AlgorithmDescriptor。
 *  ComponentDiscovery 联合 StructureDiscovery 构建只读注册表。
 * 
 *  <p>此阶段不运行算法，也不执行每次运行前的数据初始化；
 *  初始化调用发生在 AlgorithmDescriptor.invoke 的执行上下文中。
 */
package com.majortom.algorithms.algorithm.discovery;
