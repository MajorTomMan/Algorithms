package com.majortom.algorithms.core.runtime;

import com.majortom.algorithms.core.base.BaseStructure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 单次算法执行的时间轴。
 *
 * <p>{@link ExecutionContext} 每次同步都会追加一帧。可视化回放、结果比较和导出功能
 * 都通过这个对象按顺序读取执行过程。</p>
 *
 * @param <S> 时间轴中保存的结构快照类型
 */
public class ExecutionTimeline<S extends BaseStructure<?>> {

    /**
     * 按产生顺序保存的执行帧。
     */
    private final List<ExecutionFrame<S>> frames = new ArrayList<>();

    /**
     * 追加一帧。
     *
     * @param frame 新的执行帧
     */
    public void add(ExecutionFrame<S> frame) {
        frames.add(frame);
    }

    /**
     * 判断时间轴是否为空。
     *
     * @return 没有任何执行帧时返回 true
     */
    public boolean isEmpty() {
        return frames.isEmpty();
    }

    /**
     * 获取帧数量。
     *
     * @return 当前时间轴长度
     */
    public int size() {
        return frames.size();
    }

    /**
     * 按序号读取一帧。
     *
     * @param index 帧序号
     * @return 对应执行帧
     */
    public ExecutionFrame<S> get(int index) {
        return frames.get(index);
    }

    /**
     * 返回全部执行帧的只读视图。
     *
     * @return 执行帧列表
     */
    public List<ExecutionFrame<S>> frames() {
        return Collections.unmodifiableList(frames);
    }
}
