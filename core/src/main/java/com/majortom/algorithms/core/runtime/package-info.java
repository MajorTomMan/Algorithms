/**
 * 领域执行运行时，负责生命周期、协作式控制和权威事件排序。
 * 
 *  <p><b>设计理由：</b>算法和结构只报告事实，RunControl 决定执行许可，
 *  ExecutionRuntime 管理开始/完成/取消并为事件提供 runId/sequence；
 *  无论 CLI、练习系统还是 JavaFX 客户端，都复用同一个事实流语义。
 * 
 *  <p>本包不依赖 UI、渲染帧率或特定 Graph/Array 实现。
 *  JavaFX 工作线程由 client 适配层创建，重放也在 client 中完成。
 */
package com.majortom.algorithms.core.runtime;
