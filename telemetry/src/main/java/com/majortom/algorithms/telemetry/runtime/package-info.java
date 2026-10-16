/**
 * 算法/结构/练习共享的独立遥测会话与历史。
 * 
 *  <p>TelemetryFramework 按稳定范围创建 TelemetrySession，
 *  采样结束后由不可变 Profile 给调用方读取；
 *  TelemetryStore 限制终态 Profile 的历史容量。
 * 
 *  <p>这里不拥有 JavaFX 图表、算法步骤或动画时钟；
 *  资源采样与真实执行生命周期对齐，避免把动画等待时间混入性能指标。
 */
package com.majortom.algorithms.telemetry.runtime;
