/**
 * 注解扫描结果的不可变注册与查找契约。
 * 
 *  <p>FrameworkClassScanner 枚举 classpath 顶层类型；
 *  algorithms/structures 各自解释自己的注解，最终在 ComponentRegistry
 *  组合并校验。稳定的结构契约、算法 ID、类型签名用于匹配；
 *  中文显示名不参与注册身份。
 * 
 *  <p>注册目录从不依赖 JavaFX 控制器，也不读取业务文件。
 */
package com.majortom.algorithms.core.registry;
