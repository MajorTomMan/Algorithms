package com.majortom.algorithms.core.runtime;

import com.majortom.algorithms.core.base.BaseStructure;
import com.majortom.algorithms.core.tree.node.TreeNode;

import java.util.ArrayList;
import java.util.List;

/**
 * 单次算法运行的执行上下文。
 *
 * <p>算法实现类在运行过程中不直接操作 JavaFX，也不直接管理线程暂停、回放和导出。
 * 它们只需要在关键步骤调用 {@link #sync(BaseStructure, Object, Object, String)}，
 * 本上下文就会完成三件事：复制当前结构快照、写入 {@link ExecutionTimeline}、
 * 并通过 {@link ExecutionControl} 把帧交给可视化层渲染。</p>
 *
 * <p>这个类是 core 和 visualization 的边界：core 只认识 {@link BaseStructure}
 * 和执行记录，可视化层通过 {@link ExecutionControl#acceptFrame(ExecutionFrame, boolean)}
 * 决定是否等待 UI 绘制、是否响应暂停、以及播放速度。</p>
 *
 * @param <S> 本次算法操作的数据结构类型，例如排序数组、树、图或迷宫
 */
public class ExecutionContext<S extends BaseStructure<?>> {

    /**
     * 模块 ID，例如 sort、tree、graph、maze。
     * <p>结束时会写入 {@link ExecutionRecord}，供回放、比较和导出识别来源模块。</p>
     */
    private final String moduleId;

    /**
     * 算法 ID，用于区分同一模块下的不同算法。
     */
    private final String algorithmId;

    /**
     * 输入签名，描述本次运行使用的输入数据。
     */
    private final String inputSignature;

    /**
     * 执行控制器，由可视化或测试层提供，用来接收帧、读取取消状态和播放延迟。
     */
    private final ExecutionControl<S> control;

    /**
     * 本次运行产生的完整时间轴。
     */
    private final ExecutionTimeline<S> timeline = new ExecutionTimeline<>();

    /**
     * 本次运行的统计数据，例如比较次数、交换次数、耗时等。
     */
    private final ExecutionStats stats = new ExecutionStats();

    /**
     * 算法运行时记录的消息，最终会进入执行记录。
     */
    private final List<ExecutionMessage> messages = new ArrayList<>();

    /**
     * 创建一次算法运行上下文，并立即标记统计开始时间。
     *
     * @param moduleId 模块 ID，例如 sort、tree、graph、maze
     * @param algorithmId 算法 ID
     * @param inputSignature 输入数据签名
     * @param control 可选的执行控制器；为空时只记录时间轴，不触发 UI 同步
     */
    public ExecutionContext(
            String moduleId,
            String algorithmId,
            String inputSignature,
            ExecutionControl<S> control) {
        this.moduleId = moduleId;
        this.algorithmId = algorithmId;
        this.inputSignature = inputSignature;
        this.control = control;
        this.stats.markStarted();
    }

    /**
     * 记录一个默认需要等待渲染完成的同步点。
     *
     * <p>算法一般调用这个重载即可。参数 {@code a} 和 {@code b} 会作为焦点对象保存到
     * {@link ExecutionFrame}，可视化层通常把它们渲染成高亮元素。</p>
     *
     * @param structure 当前算法结构
     * @param a 第一个焦点对象，例如索引、节点或单元格
     * @param b 第二个焦点对象
     * @param label 当前步骤说明
     */
    public void sync(S structure, Object a, Object b, String label) {
        sync(structure, a, b, label, true);
    }

    /**
     * 记录一个同步点，并决定是否等待可视化层完成本帧。
     *
     * <p>这是执行层最关键的方法：它会先检查取消状态，再从结构中同步统计数据，
     * 然后调用 {@link BaseStructure#copy()} 生成不可被后续算法步骤污染的快照。
     * 快照会被包装成 {@link ExecutionFrame} 写入时间轴，并交给
     * {@link ExecutionControl} 发送到 UI 线程。</p>
     *
     * @param structure 当前算法结构
     * @param a 第一个焦点对象
     * @param b 第二个焦点对象
     * @param label 当前步骤说明
     * @param awaitStep 是否让控制器等待渲染与播放延迟完成
     */
    @SuppressWarnings("unchecked")
    public void sync(S structure, Object a, Object b, String label, boolean awaitStep) {
        requireActive();
        stats.syncFrom(structure);

        S snapshot = (S) structure.copy();
        ExecutionFrame<S> frame = new ExecutionFrame<>(
                timeline.size(),
                stats.snapshot().durationMillis(),
                label,
                normalizeFocus(a),
                normalizeFocus(b),
                snapshot,
                stats.snapshot());

        timeline.add(frame);

        if (control != null) {
            control.acceptFrame(frame, awaitStep);
        }

        requireActive();
    }

    /**
     * 记录一条运行消息。
     *
     * <p>消息不会影响算法流程，主要用于 UI 日志、导出记录或后续错误诊断。</p>
     *
     * @param level 消息等级
     * @param code 稳定消息编码
     * @param text 可直接展示或导出的消息文本
     */
    public void message(ExecutionMessageLevel level, String code, String text) {
        messages.add(new ExecutionMessage(System.currentTimeMillis(), level, code, text));
    }

    /**
     * 判断本次执行是否已被取消。
     *
     * @return 如果可视化层或调用方请求取消，返回 true
     */
    public boolean isCancelled() {
        return control != null && control.isCancelled();
    }

    /**
     * 读取当前播放延迟。
     *
     * @return 每帧渲染后的等待毫秒数；没有控制器时返回 0
     */
    public long delayMillis() {
        return control == null ? 0L : control.getDelayMillis();
    }

    /**
     * 获取当前统计快照。
     *
     * @return 不可变语义的统计快照，用于 UI 展示或执行记录
     */
    public ExecutionStatsSnapshot currentStats() {
        return stats.snapshot();
    }

    /**
     * 获取本次运行已经产生的时间轴。
     *
     * @return 执行时间轴
     */
    public ExecutionTimeline<S> timeline() {
        return timeline;
    }

    /**
     * 结束本次算法运行并生成完整执行记录。
     *
     * <p>控制器通常在算法线程结束时调用它，然后把返回的记录交给回放、比较或导出功能。</p>
     *
     * @return 包含模块、算法、输入、时间轴、统计和消息的执行记录
     */
    public ExecutionRecord<S> finish() {
        stats.markEnded();
        return new ExecutionRecord<>(
                moduleId,
                algorithmId,
                inputSignature,
                System.currentTimeMillis(),
                timeline,
                stats.snapshot(),
                List.copyOf(messages));
    }

    /**
     * 若执行已经取消则抛出运行时异常。
     *
     * <p>算法在循环内可以主动调用它；{@link #sync(BaseStructure, Object, Object, String, boolean)}
     * 也会在渲染前后各检查一次，以便尽快响应停止按钮。</p>
     */
    public void requireActive() {
        if (isCancelled()) {
            throw new ExecutionException("execution.cancelled", "Execution cancelled");
        }
    }

    /**
     * 把复杂焦点对象转换成更适合序列化和显示的值。
     *
     * <p>树节点如果直接进入执行帧，导出和 UI 可能要理解完整节点结构。
     * 这里把 {@link TreeNode} 统一降级为节点数据，让焦点表达更稳定。</p>
     *
     * @param focus 原始焦点对象
     * @return 可保存到执行帧的焦点值
     */
    private Object normalizeFocus(Object focus) {
        if (focus instanceof TreeNode<?> treeNode) {
            return treeNode.data;
        }
        return focus;
    }
}
