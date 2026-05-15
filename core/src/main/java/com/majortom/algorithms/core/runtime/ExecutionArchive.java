package com.majortom.algorithms.core.runtime;

import com.majortom.algorithms.core.base.BaseStructure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 执行记录归档容器。
 *
 * <p>控制器可以把每次算法结束得到的 {@link ExecutionRecord} 放进这里。
 * 后续“同输入比较”和“历史回放”功能都从这个归档中查找记录。</p>
 */
public class ExecutionArchive {

    /**
     * 已完成的执行记录。
     */
    private final List<ExecutionRecord<? extends BaseStructure<?>>> records = new ArrayList<>();

    /**
     * 添加一条完成的执行记录。
     *
     * @param record 算法运行结束后生成的记录
     */
    public synchronized void add(ExecutionRecord<? extends BaseStructure<?>> record) {
        records.add(record);
    }

    /**
     * 返回全部执行记录的只读副本。
     *
     * @return 记录列表副本，调用方不能修改内部归档
     */
    public synchronized List<ExecutionRecord<? extends BaseStructure<?>>> all() {
        return Collections.unmodifiableList(new ArrayList<>(records));
    }

    /**
     * 查找可与当前记录比较的历史记录。
     *
     * <p>比较要求来自同一个模块，并且输入签名一致。这样排序、树、图、迷宫之间不会混在一起，
     * 不同输入规模或不同结构也不会误判为可比较。</p>
     *
     * @param current 当前执行记录
     * @return 可比较的历史记录列表
     */
    public synchronized List<ExecutionRecord<? extends BaseStructure<?>>> comparableRecords(
            ExecutionRecord<? extends BaseStructure<?>> current) {
        return records.stream()
                .filter(record -> record != current)
                .filter(record -> record.moduleId().equals(current.moduleId()))
                .filter(record -> record.inputSignature().equals(current.inputSignature()))
                .collect(Collectors.toList());
    }
}
