package com.majortom.algorithms.visualization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.majortom.algorithms.core.base.BaseAlgorithms;
import com.majortom.algorithms.core.base.BaseStructure;
import com.majortom.algorithms.core.export.StructureSnapshotSerializer;
import com.majortom.algorithms.core.runtime.ExecutionArchive;
import com.majortom.algorithms.core.runtime.ExecutionContext;
import com.majortom.algorithms.core.runtime.ExecutionControl;
import com.majortom.algorithms.core.runtime.ExecutionException;
import com.majortom.algorithms.core.runtime.ExecutionFrame;
import com.majortom.algorithms.core.runtime.ExecutionRecord;
import com.majortom.algorithms.core.runtime.ExecutionStatsSnapshot;
import com.majortom.algorithms.core.runtime.ExecutionTimeline;
import com.majortom.algorithms.visualization.impl.controller.BaseModuleController;
import com.majortom.algorithms.visualization.manager.AlgorithmThreadManager;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.util.Duration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * 算法可视化控制器基类。
 *
 * <p>它是 UI 控件、算法对象、执行上下文和可视化组件之间的中枢。
 * 子类只需要提供模块 ID、算法启动方式、统计文案和模块专属控件；
 * 本基类统一处理运行、停止、暂停、回放、导出、比较和全局按钮绑定。</p>
 *
 * @param <S> 当前模块渲染的数据结构类型
 */
public abstract class BaseController<S extends BaseStructure<?>> implements Initializable {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    protected static final ExecutionArchive EXECUTION_ARCHIVE = new ExecutionArchive();

    protected final DoubleProperty delayMs = new SimpleDoubleProperty(50.0);
    protected ExecutionStatsSnapshot stats = ExecutionStatsSnapshot.empty();

    protected final BaseVisualizer<S> visualizer;
    protected Label statsLabel;
    protected TextArea logArea;
    protected Slider delaySlider;
    protected Slider timelineSlider;
    protected HBox customControlBox;

    protected Button startBtn;
    protected Button pauseBtn;
    protected Button resetBtn;
    protected Button replayBtn;
    protected Button exportBtn;
    protected Button compareBtn;

    protected ExecutionContext<S> executionContext;
    protected ExecutionRecord<S> lastExecution;

    private Timeline replayTimeline;
    private boolean updatingTimelineSlider;

    public BaseController(BaseVisualizer<S> visualizer) {
        this.visualizer = visualizer;
    }

    /**
     * JavaFX 初始化入口。
     *
     * @param location FXML 地址
     * @param resources 国际化资源
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupI18n();
    }

    /**
     * 将执行帧渲染到当前模块。
     *
     * @param frame 执行上下文生成的帧
     */
    protected final void renderFrame(ExecutionFrame<S> frame) {
        this.stats = frame.stats();
        if (visualizer != null) {
            visualizer.render(frame.snapshot(), frame.focusA(), frame.focusB());
        }
        refreshStatsDisplay();
        syncTimelineSlider(frame);
    }

    /**
     * 启动一次算法执行。
     *
     * <p>这里创建 {@link ExecutionContext}，把 {@link ExecutionControl} 适配到
     * {@link AlgorithmThreadManager}，然后把算法放到 worker 线程中执行。</p>
     *
     * @param algorithm 算法实例
     * @param data 数据结构实例
     */
    public final void startAlgorithm(BaseAlgorithms<S> algorithm, S data) {
        stopAlgorithm();
        clearExecutionState();
        resetStats();

        String inputSignature = executionInputSignature(data);
        final ExecutionContext<S> context = new ExecutionContext<>(
                moduleId(),
                executionAlgorithmId(algorithm),
                inputSignature,
                new ExecutionControl<>() {
                    @Override
                    public void acceptFrame(ExecutionFrame<S> frame, boolean awaitStep) {
                        Runnable task = () -> {
                            if (executionContext == null) {
                                return;
                            }
                            renderFrame(frame);
                        };
                        if (awaitStep) {
                            AlgorithmThreadManager.syncAndWait(task);
                        } else {
                            AlgorithmThreadManager.postStatus(task);
                        }
                    }

                    @Override
                    public boolean isCancelled() {
                        return Thread.currentThread().isInterrupted();
                    }

                    @Override
                    public long getDelayMillis() {
                        return AlgorithmThreadManager.getDelay();
                    }
                });
        this.executionContext = context;

        algorithm.setExecutionContext(context);

        appendLog("Started: " + executionAlgorithmId(algorithm));

        AlgorithmThreadManager.run(() -> {
            try {
                executeAlgorithm(algorithm, data);
            } catch (BaseAlgorithms.AlgorithmInterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                if (!"execution.cancelled".equals(e.getCode())) {
                    handleAlgorithmError(e);
                }
            } catch (Exception e) {
                handleAlgorithmError(e);
            }
        }, () -> {
            this.lastExecution = context.finish();
            this.executionContext = null;
            EXECUTION_ARCHIVE.add(lastExecution);
            this.stats = lastExecution.summary();
            refreshStatsDisplay();
            prepareTimelineControls();
            onAlgorithmFinished();
        });
    }

    /**
     * 停止当前算法并停止回放。
     */
    public void stopAlgorithm() {
        stopReplay();
        AlgorithmThreadManager.stopAll();
    }

    /**
     * 切换暂停/恢复状态。
     */
    public void togglePause() {
        if (AlgorithmThreadManager.isPaused()) {
            AlgorithmThreadManager.resume();
        } else {
            AlgorithmThreadManager.pause();
        }
    }

    /**
     * 切换执行记录回放。
     */
    public void toggleReplay() {
        if (!hasExecutionData() || AlgorithmThreadManager.isRunning()) {
            return;
        }
        if (replayTimeline != null && replayTimeline.getStatus() == Animation.Status.RUNNING) {
            stopReplay();
            return;
        }

        List<ExecutionFrame<S>> frames = lastExecution.timeline().frames();
        if (frames.isEmpty()) {
            return;
        }

        replayTimeline = new Timeline(new KeyFrame(
                Duration.millis(Math.max(40L, AlgorithmThreadManager.getDelay())),
                event -> advanceReplayFrame()));
        replayTimeline.setCycleCount(Animation.INDEFINITE);
        renderFrame(frames.get(0));
        replayTimeline.play();
    }

    /**
     * 跳转到执行时间轴的指定进度。
     *
     * @param progress 0 到 1 之间的进度值
     */
    public void seekTimeline(double progress) {
        if (!hasExecutionData()) {
            return;
        }
        stopReplay();

        int size = lastExecution.timeline().size();
        if (size == 0) {
            return;
        }

        int index = (int) Math.round(progress * (size - 1));
        index = Math.max(0, Math.min(size - 1, index));
        renderFrame(lastExecution.timeline().get(index));
    }

    /**
     * 判断是否已有可回放、导出或比较的执行记录。
     *
     * @return 有执行记录且时间轴非空时返回 true
     */
    public boolean hasExecutionData() {
        return lastExecution != null && !lastExecution.timeline().isEmpty();
    }

    /**
     * 导出最近一次执行记录。
     */
    public void exportExecution() {
        if (!hasExecutionData()) {
            appendLog("Nothing to export.");
            return;
        }

        try {
            Path exportDir = Path.of("exports");
            Files.createDirectories(exportDir);

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path file = exportDir.resolve(moduleId() + "_" + lastExecution.algorithmId() + "_" + timestamp + ".json");
            JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), buildExecutionExportPayload());
            appendLog("Exported: " + file);
        } catch (IOException e) {
            handleAlgorithmError(e);
        }
    }

    /**
     * 与历史同模块、同输入记录做统计比较。
     */
    public void compareExecutions() {
        if (!hasExecutionData()) {
            appendLog("No execution data available for comparison.");
            return;
        }

        List<ExecutionRecord<? extends BaseStructure<?>>> candidates = EXECUTION_ARCHIVE
                .comparableRecords(lastExecution);
        if (candidates.isEmpty()) {
            appendLog("No comparable executions found for the same input.");
            return;
        }

        appendLog("Comparison for input " + lastExecution.inputSignature().substring(0, 8) + ":");
        appendLog(describeRecord(lastExecution));
        for (ExecutionRecord<? extends BaseStructure<?>> record : candidates) {
            appendLog(describeRecord(record));
        }
    }

    private void advanceReplayFrame() {
        if (!hasExecutionData()) {
            stopReplay();
            return;
        }

        int currentIndex = currentTimelineIndex();
        int nextIndex = currentIndex + 1;
        if (nextIndex >= lastExecution.timeline().size()) {
            stopReplay();
            return;
        }
        renderFrame(lastExecution.timeline().get(nextIndex));
    }

    /**
     * 根据时间轴滑块计算当前帧序号。
     *
     * @return 当前帧序号
     */
    private int currentTimelineIndex() {
        if (!hasExecutionData() || timelineSlider == null) {
            return 0;
        }
        int size = lastExecution.timeline().size();
        if (size <= 1) {
            return 0;
        }
        return (int) Math.round(timelineSlider.getValue() * (size - 1));
    }

    /**
     * 停止当前回放动画。
     */
    private void stopReplay() {
        if (replayTimeline != null) {
            replayTimeline.stop();
            replayTimeline = null;
        }
    }

    /**
     * 算法完成后启用并定位时间轴控件。
     */
    private void prepareTimelineControls() {
        if (timelineSlider != null) {
            timelineSlider.setDisable(!hasExecutionData());
            updatingTimelineSlider = true;
            timelineSlider.setValue(hasExecutionData() ? 1.0 : 0.0);
            updatingTimelineSlider = false;
        }
    }

    /**
     * 根据正在渲染的帧同步时间轴滑块。
     *
     * @param frame 当前渲染帧
     */
    private void syncTimelineSlider(ExecutionFrame<S> frame) {
        if (timelineSlider == null) {
            return;
        }
        int size = Math.max(1, currentTimelineSize());
        updatingTimelineSlider = true;
        timelineSlider.setValue(size == 1 ? 0.0 : (double) frame.index() / (double) (size - 1));
        timelineSlider.setDisable(AlgorithmThreadManager.isRunning() || !hasExecutionData());
        updatingTimelineSlider = false;
    }

    /**
     * 清除当前执行上下文和最近执行记录。
     */
    private void clearExecutionState() {
        stopReplay();
        executionContext = null;
        lastExecution = null;
        if (timelineSlider != null) {
            timelineSlider.setDisable(true);
            updatingTimelineSlider = true;
            timelineSlider.setValue(0.0);
            updatingTimelineSlider = false;
        }
    }

    /**
     * 获取当前可用时间轴长度。
     *
     * @return 时间轴帧数
     */
    private int currentTimelineSize() {
        ExecutionTimeline<S> timeline = currentTimeline();
        return timeline == null ? 0 : timeline.size();
    }

    /**
     * 获取当前运行中或最近完成的时间轴。
     *
     * @return 时间轴；没有数据时返回 null
     */
    private ExecutionTimeline<S> currentTimeline() {
        if (executionContext != null && !executionContext.timeline().isEmpty()) {
            return executionContext.timeline();
        }
        return lastExecution == null ? null : lastExecution.timeline();
    }

    /**
     * 构造导出 JSON 的顶层载荷。
     *
     * @return 可由 Jackson 序列化的导出数据
     */
    private Map<String, Object> buildExecutionExportPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("moduleId", lastExecution.moduleId());
        payload.put("algorithmId", lastExecution.algorithmId());
        payload.put("inputSignature", lastExecution.inputSignature());
        payload.put("summary", statsPayload(lastExecution.summary()));
        payload.put("messages", lastExecution.messages());

        List<Map<String, Object>> frames = lastExecution.timeline().frames().stream().map(frame -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("index", frame.index());
            item.put("timestampOffsetMillis", frame.timestampOffsetMillis());
            item.put("label", frame.label());
            item.put("focusA", frame.focusA());
            item.put("focusB", frame.focusB());
            item.put("stats", statsPayload(frame.stats()));
            item.put("snapshot", StructureSnapshotSerializer.serializeStructure(frame.snapshot()));
            return item;
        }).toList();

        payload.put("frames", frames);
        return payload;
    }

    /**
     * 构造统计快照导出载荷。
     *
     * @param stats 统计快照
     * @return 导出 map
     */
    private Map<String, Object> statsPayload(ExecutionStatsSnapshot stats) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("durationMillis", stats.durationMillis());
        payload.put("frameCount", stats.frameCount());
        payload.put("compareCount", stats.compareCount());
        payload.put("actionCount", stats.actionCount());
        payload.put("extras", stats.extras());
        return payload;
    }

    /**
     * 把执行记录格式化为比较日志中的一行。
     *
     * @param record 执行记录
     * @return 日志文本
     */
    private String describeRecord(ExecutionRecord<? extends BaseStructure<?>> record) {
        return String.format(
                "%s | duration=%dms | frames=%d | actions=%d | compares=%d",
                record.algorithmId(),
                record.summary().durationMillis(),
                record.summary().frameCount(),
                record.summary().actionCount(),
                record.summary().compareCount());
    }

    /**
     * 向日志区域追加一行带时间戳的文本。
     *
     * @param message 日志文本
     */
    protected final void appendLog(String message) {
        if (logArea != null) {
            Platform.runLater(
                    () -> logArea.appendText(String.format("[%tT] %s%n", System.currentTimeMillis(), message)));
        }
    }

    /**
     * 刷新统计展示。
     */
    protected void refreshStatsDisplay() {
        if (statsLabel != null) {
            statsLabel.setText(formatStatsMessage());
        }
    }

    /**
     * 重置当前统计快照。
     */
    private void resetStats() {
        this.stats = ExecutionStatsSnapshot.empty();
    }

    /**
     * 绑定主界面全局按钮行为。
     */
    private void setupGlobalButtonActions() {
        if (startBtn != null) {
            startBtn.setOnAction(e -> handleAlgorithmStart());
        }
        if (pauseBtn != null) {
            pauseBtn.setOnAction(e -> togglePause());
        }
        if (resetBtn != null) {
            resetBtn.setOnAction(e -> {
                stopAlgorithm();
                clearExecutionState();
                ((BaseModuleController<S>) this).resetModule();
            });
        }
        if (replayBtn != null) {
            replayBtn.setOnAction(e -> toggleReplay());
        }
        if (exportBtn != null) {
            exportBtn.setOnAction(e -> exportExecution());
        }
        if (compareBtn != null) {
            compareBtn.setOnAction(e -> compareExecutions());
        }
    }

    /**
     * 获取执行记录中的算法 ID。
     *
     * @param algorithm 算法实例
     * @return 算法 ID
     */
    protected String executionAlgorithmId(BaseAlgorithms<S> algorithm) {
        return algorithm.getClass().getSimpleName();
    }

    /**
     * 根据输入结构生成执行输入签名。
     *
     * @param data 输入结构
     * @return 输入签名
     */
    protected String executionInputSignature(S data) {
        return StructureSnapshotSerializer.signatureFor((BaseStructure<?>) data.copy());
    }

    /**
     * 当前模块 ID。
     *
     * @return 模块 ID
     */
    protected abstract String moduleId();

    /**
     * 格式化统计展示文本。
     *
     * @return 统计文本
     */
    protected abstract String formatStatsMessage();

    /**
     * 将模块自定义控件安装到主界面底栏。
     *
     * @param container 主界面自定义控件容器
     */
    public abstract void setupCustomControls(HBox container);

    /**
     * 绑定模块国际化文案。
     */
    protected abstract void setupI18n();

    /**
     * 执行具体算法。
     *
     * @param algorithm 算法实例
     * @param data 数据结构
     */
    protected abstract void executeAlgorithm(BaseAlgorithms<S> algorithm, S data);

    /**
     * 主界面点击开始按钮时调用。
     */
    public abstract void handleAlgorithmStart();

    /**
     * 算法自然结束后的回调。
     */
    protected void onAlgorithmFinished() {
        appendLog(String.format("Finished. Duration: %dms", stats.durationMillis()));
    }

    /**
     * 处理算法运行异常。
     *
     * @param e 异常对象
     */
    protected void handleAlgorithmError(Exception e) {
        appendLog("Runtime Error: " + e.getMessage());
        e.printStackTrace();
    }

    /**
     * 注入主界面共享 UI 控件。
     *
     * <p>主控制器切换模块时调用它，把统计面板、日志、全局按钮、速度滑块和时间轴滑块
     * 传给当前模块控制器。</p>
     */
    public final void setUIReferences(
            Label statsLabel,
            TextArea logArea,
            Slider delaySlider,
            Slider timelineSlider,
            HBox customBox,
            Button startBtn,
            Button pauseBtn,
            Button resetBtn,
            Button replayBtn,
            Button exportBtn,
            Button compareBtn) {
        this.statsLabel = statsLabel;
        this.logArea = logArea;
        this.delaySlider = delaySlider;
        this.timelineSlider = timelineSlider;
        this.customControlBox = customBox;
        this.startBtn = startBtn;
        this.pauseBtn = pauseBtn;
        this.resetBtn = resetBtn;
        this.replayBtn = replayBtn;
        this.exportBtn = exportBtn;
        this.compareBtn = compareBtn;

        if (this.delaySlider != null) {
            this.delayMs.bind(this.delaySlider.valueProperty());
            this.delayMs.addListener((obs, oldVal, newVal) -> AlgorithmThreadManager.setDelay(newVal.longValue()));
            AlgorithmThreadManager.setDelay(delayMs.longValue());
        }

        if (this.timelineSlider != null) {
            this.timelineSlider.setDisable(true);
            this.timelineSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (!updatingTimelineSlider
                        && !AlgorithmThreadManager.isRunning()
                        && this.timelineSlider.isValueChanging()) {
                    seekTimeline(newVal.doubleValue());
                }
            });
        }

        setupGlobalButtonActions();
        this.initialize(null, null);
    }

    /**
     * 获取当前模块可视化组件。
     *
     * @return 可视化组件
     */
    public final BaseVisualizer<S> getVisualizer() {
        return visualizer;
    }

    /**
     * 获取可加入 JavaFX 布局树的可视化区域。
     *
     * @return 可视化 Region
     */
    public final Region getVisualizerView() {
        if (visualizer instanceof Region region) {
            return region;
        }
        throw new IllegalStateException("Visualizer must extend Region for layout integration.");
    }
}
