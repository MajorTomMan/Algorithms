package com.majortom.algorithms.visualization.impl.controller;

import com.majortom.algorithms.core.event.structure.HashStructureEvent;
import com.majortom.algorithms.core.metadata.StructureIds;
import com.majortom.algorithms.core.registry.AlgorithmTypeSignature;
import com.majortom.algorithms.visualization.algorithm.AlgorithmCatalog;
import com.majortom.algorithms.visualization.module.AlgorithmSelectionSupport;
import com.majortom.algorithms.visualization.structure.SnapshotAlgorithmInputSupport;
import com.majortom.algorithms.core.runtime.EventEnvelope;
import com.majortom.algorithms.core.snapshot.HashTableSnapshot;
import com.majortom.algorithms.core.snapshot.HashTableStateSnapshot;
import com.majortom.algorithms.core.snapshot.StructureSnapshot;
import com.majortom.algorithms.structure.hash.HashTableStructure;
import com.majortom.algorithms.visualization.impl.visualizer.HashTableVisualizer;
import com.majortom.algorithms.visualization.impl.visualizer.presenter.HashTablePresenter;
import com.majortom.algorithms.visualization.international.I18N;
import com.majortom.algorithms.visualization.render.runtime.RenderContext;
import com.majortom.algorithms.visualization.runtime.Reduction;
import com.majortom.algorithms.visualization.runtime.VisualValue;
import com.majortom.algorithms.visualization.runtime.hash.HashTableEventReducer;
import com.majortom.algorithms.visualization.runtime.hash.HashTableViewState;
import com.majortom.algorithms.visualization.runtime.value.ValueAdapter;
import com.majortom.algorithms.visualization.runtime.value.ValueAdapters;
import com.majortom.algorithms.visualization.structure.HashRuntimeTypeSupport;
import com.majortom.algorithms.visualization.structure.StructureSnapshotSupport;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Random;
import java.util.function.Consumer;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/**
 * Hash-table workbench orchestration.
 *
 * <p>The concrete collision strategy lives entirely in the registered HashTableStructure
 * implementation. This controller consumes only its public contract plus factual HashStructureEvent
 * instances.</p>
 */
public final class HashTableController extends BaseModuleController<HashTableViewState>
    implements AlgorithmSelectionSupport,
        StructureSnapshotSupport<HashTableStateSnapshot<Object, Object>>,
        SnapshotAlgorithmInputSupport<HashTableStateSnapshot<Object, Object>>,
        HashRuntimeTypeSupport {

  private final HashTableStructure<Object, Object> table;
  private Class<?> runtimeKeyType = Integer.class;
  private Class<?> runtimeHashValueType = Integer.class;
  private ValueAdapter<Object> keyAdapter = ValueAdapters.requireObjectAdapter(Integer.class);
  private ValueAdapter<Object> hashValueAdapter = ValueAdapters.requireObjectAdapter(Integer.class);
  private Consumer<Selection> selectionListener = ignored -> {};
  private Consumer<String> algorithmSelectionListener = ignored -> {};
  private AlgorithmSelectorBinder algorithmBinder;
  private StructureSnapshot<HashTableStateSnapshot<Object, Object>> algorithmInputSnapshot;
  private String selectedAlgorithmId;
  private boolean structureSelectionEnabled = true;

  @FXML private Label typeLabel;
  @FXML private Label algorithmLabel;
  @FXML private ComboBox<String> algorithmSelector;
  @FXML private Label operationsLabel;
  @FXML private TextField keyField;
  @FXML private TextField valueField;
  @FXML private Button putBtn;
  @FXML private Button removeBtn;
  @FXML private Button findBtn;

  @SuppressWarnings("unchecked")
  public HashTableController(RenderContext renderContext) {
    super(new HashTableVisualizer(), new HashTablePresenter(), "/fxml/HashTableControls.fxml",
        renderContext);
    table = (HashTableStructure<Object, Object>)
        structure(StructureIds.HASH, HashTableStructure.class);
    if (table.capacity() < 1) {
      throw new IllegalStateException("HashTable capacity must be positive");
    }
    if (!table.isEmpty()) {
      throw new IllegalStateException(
          "Registered HashTable must be empty after default construction so factual bucket state "
              + "can be built from StructureEvents");
    }
    renderStructureState(HashTableViewState.empty(table.capacity()));
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    super.initialize(location, resources);
    hashVisualizer().setSelectionListener(this::handleVisualSelection);
    hashVisualizer().setBucketSelectionListener(this::handleBucketSelection);
    algorithmBinder = new AlgorithmSelectorBinder(algorithmSelector, this::algorithmIds,
        ignored -> algorithmSelectionListener.accept(selectedAlgorithmId()));
    algorithmBinder.refresh();
  }

  @FXML
  private void handlePut() {
    Object key = parse(keyField, keyAdapter);
    Object value = parse(valueField, hashValueAdapter);
    if (key == null || value == null) {
      logI18n("message.hash.invalid_entry");
      return;
    }

    if (!executeAndReduce("put", () -> table.put(key, value))) {
      return;
    }
    selectByKey(key);
    logI18n("message.hash.put", key, value);
  }

  @FXML
  private void handleRemove() {
    Object key = parse(keyField, keyAdapter);
    if (key == null) {
      logI18n("message.hash.invalid_entry");
      return;
    }
    if (!table.containsKey(key)) {
      logI18n("message.hash.not_found", key);
      return;
    }

    Object[] removed = new Object[1];
    if (!executeAndReduce("remove", () -> removed[0] = table.remove(key))) {
      return;
    }
    clearVisualSelection();
    logI18n("message.hash.removed", key, removed[0]);
  }

  @FXML
  private void handleFind() {
    Object key = parse(keyField, keyAdapter);
    if (key == null) {
      logI18n("message.hash.invalid_entry");
      return;
    }
    if (!table.containsKey(key)) {
      clearVisualSelection();
      logI18n("message.hash.not_found", key);
      return;
    }
    Object value = table.get(key);
    selectByKey(key);
    logI18n("message.hash.found", key, value);
  }

  private boolean executeAndReduce(String operationId, Runnable mutation) {
    int eventStart = structureEvents().size();
    HashTableViewState before = latestStructureState();
    if (before == null) {
      before = HashTableViewState.empty(table.capacity());
    }

    if (!executeStructureOperation(operationId, () -> {
      mutation.run();
      return null;
    })) {
      return false;
    }

    HashTableEventReducer reducer = new HashTableEventReducer(before);
    HashTableViewState state = before;
    boolean factualHashEvent = false;
    List<EventEnvelope> events = structureEvents();
    for (int index = eventStart; index < events.size(); index++) {
      if (!(events.get(index).event() instanceof HashStructureEvent)) {
        continue;
      }
      factualHashEvent = true;
      Reduction<HashTableViewState> reduction = reducer.reduce(state, events.get(index));
      state = reduction.state();
    }

    if (!factualHashEvent) {
      logI18n("message.hash.events_required");
    }
    renderStructureState(state);
    refreshStatsDisplay();
    return true;
  }

  private Object parse(TextField field, ValueAdapter<Object> adapter) {
    try {
      return adapter.parse(field.getText());
    } catch (RuntimeException failure) {
      return null;
    }
  }

  private void selectByKey(Object key) {
    HashTableViewState state = latestStructureState();
    if (state == null) return;
    for (HashTableViewState.Bucket bucket : state.buckets()) {
      for (HashTableViewState.Entry entry : bucket.entries()) {
        if (Objects.equals(entry.key().value(), key)) {
          hashVisualizer().selectEntry(entry.id());
          return;
        }
      }
    }
  }

  public void setSelectionListener(Consumer<Selection> listener) {
    selectionListener = listener == null ? ignored -> {} : listener;
  }

  public void setStructureSelectionEnabled(boolean enabled) {
    if (structureSelectionEnabled != enabled) {
      clearVisualSelection();
    }
    structureSelectionEnabled = enabled;
  }

  private void handleBucketSelection(int bucketIndex) {
    requestPresentationRender();
    HashTableViewState state = latestStructureState();
    if (state == null || bucketIndex < 0 || bucketIndex >= state.capacity()) {
      clearVisualSelection();
      return;
    }
    HashTableViewState.Bucket bucket = state.buckets().get(bucketIndex);
    selectionListener.accept(new BucketSelection(
        bucket.index(), bucket.entries().size(), state.size(), state.capacity()));
  }

  private void handleVisualSelection(long entryId) {
    requestPresentationRender();
    HashTableViewState state = latestStructureState();
    if (state == null) return;
    for (HashTableViewState.Bucket bucket : state.buckets()) {
      for (HashTableViewState.Entry entry : bucket.entries()) {
        if (entry.id() != entryId) continue;
        if (structureSelectionEnabled) {
          keyField.setText(keyAdapter.format(entry.key().value()));
          valueField.setText(hashValueAdapter.format(entry.value().value()));
        }
        selectionListener.accept(new EntrySelection(
            entry.id(), bucket.index(), entry.key(), entry.value(), state.size(), state.capacity()));
        return;
      }
    }
    clearVisualSelection();
  }

  private void clearVisualSelection() {
    hashVisualizer().clearSelection();
    requestPresentationRender();
    selectionListener.accept(null);
  }

  @Override
  protected String moduleId() {
    return StructureIds.HASH;
  }

  @Override
  protected String formatStatsMessage() {
    return I18N.text("stats.hash", table.size(), table.capacity());
  }

  @Override
  protected void setupI18n() {
    if (typeLabel == null) return;
    typeLabel.textProperty().bind(I18N.createStringBinding("label.hash.title"));
    algorithmLabel.textProperty().bind(I18N.createStringBinding("label.common.algorithm"));
    operationsLabel.textProperty().bind(I18N.createStringBinding("label.hash.operations"));
    keyField.promptTextProperty().bind(I18N.createStringBinding("prompt.hash.key"));
    valueField.promptTextProperty().bind(I18N.createStringBinding("prompt.hash.value"));
    putBtn.textProperty().bind(I18N.createStringBinding("action.hash.put"));
    removeBtn.textProperty().bind(I18N.createStringBinding("action.hash.remove"));
    findBtn.textProperty().bind(I18N.createStringBinding("action.hash.find"));
  }

  @Override
  public void handleAlgorithmStart() {
    if (!ensureReplayableValue(runtimeKeyType)
        || !ensureReplayableValue(runtimeHashValueType)
        || isRunning()) {
      return;
    }

    StructureSnapshot<HashTableStateSnapshot<Object, Object>> inputSnapshot =
        algorithmInputSnapshot == null ? captureStructureSnapshot() : algorithmInputSnapshot;
    requireSnapshot(inputSnapshot);
    String algorithmId = selectedAlgorithmId();
    if (algorithmId == null) {
      algorithmLogI18n("message.hash.no_algorithm");
      return;
    }

    HashTableSnapshot<Object, Object> tableSnapshot = inputSnapshot.state().table();
    HashTableStructure<Object, Object> runtimeTable = newRuntimeTable(tableSnapshot);
    var descriptor = algorithm(algorithmId, currentAlgorithmTypeSignature());
    startAlgorithm(
        algorithmId,
        inputSnapshot.state(),
        () -> descriptor.invoke(runtimeTable),
        () -> new HashTableEventReducer(tableSnapshot));
  }

  @SuppressWarnings("unchecked")
  private HashTableStructure<Object, Object> newRuntimeTable(
      HashTableSnapshot<Object, Object> snapshot) {
    HashTableStructure<Object, Object> runtime =
        (HashTableStructure<Object, Object>)
            structure(StructureIds.HASH, HashTableStructure.class);
    runtime.initialize(snapshot);
    return runtime;
  }

  private AlgorithmTypeSignature currentAlgorithmTypeSignature() {
    return AlgorithmTypeSignature.of(runtimeKeyType, runtimeHashValueType);
  }

  @Override
  public boolean selectAlgorithm(String algorithmId) {
    if (algorithmId == null || !algorithmIds().contains(algorithmId)) {
      return false;
    }
    if (algorithmBinder != null) algorithmBinder.select(algorithmId);
    else selectedAlgorithmId = algorithmId;
    algorithmSelectionListener.accept(algorithmId);
    return true;
  }

  @Override
  public List<String> algorithmIds() {
    return AlgorithmCatalog.compatibleAlgorithms(
        HashTableStructure.class, currentAlgorithmTypeSignature());
  }

  @Override
  public String selectedAlgorithmId() {
    List<String> ids = algorithmIds();
    if (ids.isEmpty()) {
      selectedAlgorithmId = null;
      return null;
    }
    if (algorithmBinder != null) return algorithmBinder.selectedId();
    if (selectedAlgorithmId == null || !ids.contains(selectedAlgorithmId)) {
      selectedAlgorithmId = ids.getFirst();
    }
    return selectedAlgorithmId;
  }

  @Override
  public void setAlgorithmSelectionListener(Consumer<String> listener) {
    algorithmSelectionListener = listener == null ? ignored -> {} : listener;
    algorithmSelectionListener.accept(selectedAlgorithmId());
  }

  @Override
  protected Object structureMemoryRoot() {
    return table;
  }

  @Override
  protected boolean supportsDataTools() {
    return true;
  }

  @Override
  protected String bulkInputPromptKey() {
    return "prompt.hash.bulk";
  }

  @Override
  protected boolean canGenerateRandomData() {
    return ValueAdapters.maxDistinctSamples(runtimeKeyType) > 0
        && ValueAdapters.canGenerate(runtimeHashValueType);
  }

  @Override
  protected void applyBulkData(String input) {
    List<HashTableStructure.Entry<Object, Object>> entries = parseBulkEntries(input);
    if (entries == null) {
      return;
    }
    replaceEntries(entries, "bulk-replace", "message.data.bulk_applied");
  }

  @Override
  protected void randomizeData() {
    if (!canGenerateRandomData()) {
      return;
    }
    Random random = new Random();
    int count = Math.min(8, ValueAdapters.maxDistinctSamples(runtimeKeyType));
    List<HashTableStructure.Entry<Object, Object>> entries = new ArrayList<>(count);
    for (int index = 0; index < count; index++) {
      entries.add(new HashTableStructure.Entry<>(
          ValueAdapters.distinctValue(runtimeKeyType, index),
          ValueAdapters.randomValue(runtimeHashValueType, random)));
    }
    replaceEntries(entries, "randomize", "message.data.randomized");
  }

  private List<HashTableStructure.Entry<Object, Object>> parseBulkEntries(String input) {
    if (input == null || input.isBlank()) {
      logI18n("message.error.bulk_input_empty");
      return null;
    }
    List<HashTableStructure.Entry<Object, Object>> entries = new ArrayList<>();
    try {
      for (String token : input.split("[\\n;,]+")) {
        String pair = token.trim();
        if (pair.isEmpty()) continue;
        int separator = pair.indexOf('=');
        if (separator <= 0 || separator == pair.length() - 1) {
          throw new IllegalArgumentException("Expected key=value");
        }
        Object key = keyAdapter.parse(pair.substring(0, separator).trim());
        Object value = hashValueAdapter.parse(pair.substring(separator + 1).trim());
        entries.add(new HashTableStructure.Entry<>(key, value));
      }
    } catch (RuntimeException failure) {
      logI18n("message.error.bulk_input_invalid");
      return null;
    }
    if (entries.isEmpty()) {
      logI18n("message.error.bulk_input_empty");
      return null;
    }
    return List.copyOf(entries);
  }

  private void replaceEntries(
      List<HashTableStructure.Entry<Object, Object>> entries,
      String operationId,
      String messageKey) {
    List<HashTableSnapshot.Entry<Object, Object>> snapshotEntries = new ArrayList<>();
    int capacity = table.capacity();
    for (HashTableStructure.Entry<Object, Object> entry : entries) {
      int bucketIndex = Math.floorMod(entry.key().hashCode(), capacity);
      snapshotEntries.add(
          new HashTableSnapshot.Entry<>(bucketIndex, entry.key(), entry.value()));
    }
    HashTableSnapshot<Object, Object> replacement =
        new HashTableSnapshot<>(capacity, snapshotEntries);
    if (!executeAndReduce(operationId, () -> table.initialize(replacement))) {
      return;
    }
    clearVisualSelection();
    refreshStatsDisplay();
    logI18n(messageKey, entries.size());
  }

  @Override
  protected void onResetData() {
    algorithmInputSnapshot = null;
    clearVisualSelection();
    clearTablePreservingCapacity();
  }

  private void clearTablePreservingCapacity() {
    List<Object> keys = currentKeys();
    if (keys.isEmpty()) {
      renderStructureState(HashTableViewState.empty(table.capacity()));
      return;
    }
    executeStructureOperation("clear", () -> {
      for (Object key : keys) {
        table.remove(key);
      }
      return null;
    });
    renderStructureState(HashTableViewState.empty(table.capacity()));
    refreshStatsDisplay();
  }

  private List<Object> currentKeys() {
    List<Object> keys = new ArrayList<>();
    for (HashTableStructure.Entry<Object, Object> entry : table.entries()) {
      keys.add(entry.key());
    }
    return List.copyOf(keys);
  }

  @Override
  public StructureSnapshot<HashTableStateSnapshot<Object, Object>> captureStructureSnapshot() {
    HashTableViewState state = latestStructureState();
    if (state == null) {
      state = HashTableViewState.empty(table.capacity());
    }
    HashTableStateSnapshot<Object, Object> snapshot =
        HashTableStateSnapshot.of(runtimeKeyType, runtimeHashValueType, toSnapshot(state));
    return StructureSnapshot.create(StructureIds.HASH, snapshot);
  }

  @Override
  public void restoreStructureSnapshot(
      StructureSnapshot<HashTableStateSnapshot<Object, Object>> snapshot) {
    requireSnapshot(snapshot);
    HashTableSnapshot<Object, Object> tableSnapshot = snapshot.state().table();
    replaceWithSnapshot(tableSnapshot);
    clearVisualSelection();
    renderStructureState(HashTableViewState.fromSnapshot(tableSnapshot));
    refreshStatsDisplay();
  }

  @Override
  public void useSnapshotAsAlgorithmInput(
      StructureSnapshot<HashTableStateSnapshot<Object, Object>> snapshot) {
    requireSnapshot(snapshot);
    algorithmInputSnapshot = snapshot;
    invalidateExecutionForInputChange();
  }

  @Override
  public void useCurrentStructureAsAlgorithmInput() {
    algorithmInputSnapshot = null;
    invalidateExecutionForInputChange();
  }

  @Override
  public String algorithmInputSnapshotId() {
    return algorithmInputSnapshot == null ? null : algorithmInputSnapshot.id();
  }

  @Override
  protected boolean algorithmInputTracksCurrentStructure() {
    return algorithmInputSnapshot == null;
  }

  @Override
  protected void restoreAlgorithmState() {
    if (latestViewState() != null) {
      super.restoreAlgorithmState();
      return;
    }
    HashTableStateSnapshot<Object, Object> state = algorithmInputSnapshot == null
        ? captureStructureSnapshot().state()
        : algorithmInputSnapshot.state();
    renderViewState(HashTableViewState.fromSnapshot(state.table()));
  }

  @Override
  public void previewStructureSnapshot(
      StructureSnapshot<HashTableStateSnapshot<Object, Object>> snapshot) {
    requireSnapshot(snapshot);
    clearVisualSelection();
    renderPreviewState(HashTableViewState.fromSnapshot(snapshot.state().table()));
  }

  private void requireSnapshot(
      StructureSnapshot<HashTableStateSnapshot<Object, Object>> snapshot) {
    if (!StructureIds.HASH.equals(snapshot.moduleId())) {
      throw new IllegalArgumentException("snapshot belongs to module " + snapshot.moduleId());
    }
    snapshot.state().requireTypes(runtimeKeyType, runtimeHashValueType);
  }

  private void replaceWithSnapshot(HashTableSnapshot<Object, Object> snapshot) {
    if (!executeStructureOperation("restore", () -> {
      table.initialize(snapshot);
      return null;
    })) {
      throw new IllegalStateException("Hash snapshot restore failed");
    }
  }

  private static HashTableSnapshot<Object, Object> toSnapshot(HashTableViewState state) {
    List<HashTableSnapshot.Entry<Object, Object>> entries = new ArrayList<>();
    for (HashTableViewState.Bucket bucket : state.buckets()) {
      for (HashTableViewState.Entry entry : bucket.entries()) {
        entries.add(new HashTableSnapshot.Entry<>(
            bucket.index(), entry.key().value(), entry.value().value()));
      }
    }
    return new HashTableSnapshot<>(state.capacity(), entries);
  }

  @Override
  public String describeStructureSnapshot(HashTableStateSnapshot<Object, Object> state) {
    return I18N.text(
        "snapshot.hash.detail", state.table().entries().size(), state.table().capacity());
  }

  @Override
  public String snapshotPrimaryCount(HashTableStateSnapshot<Object, Object> state) {
    return Integer.toString(state.table().entries().size());
  }

  @Override
  public String snapshotSecondaryCount(HashTableStateSnapshot<Object, Object> state) {
    return Integer.toString(state.table().capacity());
  }

  @Override
  public Class<?> runtimeKeyType() {
    return runtimeKeyType;
  }

  @Override
  public Class<?> runtimeHashValueType() {
    return runtimeHashValueType;
  }

  @Override
  public List<Class<?>> supportedKeyTypes() {
    return ValueAdapters.supportedTypes();
  }

  @Override
  public List<Class<?>> supportedHashValueTypes() {
    return ValueAdapters.supportedTypes();
  }

  @Override
  public boolean hasValues() {
    return !table.isEmpty();
  }

  @Override
  public void setRuntimeTypes(Class<?> keyType, Class<?> valueType) {
    if (!supportedKeyTypes().contains(keyType)) {
      throw new IllegalArgumentException("Unsupported Hash key type: " + keyType.getName());
    }
    if (!supportedHashValueTypes().contains(valueType)) {
      throw new IllegalArgumentException("Unsupported Hash value type: " + valueType.getName());
    }
    if (runtimeKeyType.equals(keyType) && runtimeHashValueType.equals(valueType)) {
      return;
    }
    if (!table.isEmpty()) {
      clearTablePreservingCapacity();
    }

    runtimeKeyType = keyType;
    runtimeHashValueType = valueType;
    keyAdapter = ValueAdapters.requireObjectAdapter(keyType);
    hashValueAdapter = ValueAdapters.requireObjectAdapter(valueType);
    algorithmInputSnapshot = null;
    selectedAlgorithmId = null;
    if (algorithmBinder != null) algorithmBinder.refresh();
    algorithmSelectionListener.accept(selectedAlgorithmId());
    refreshRandomDataButton();
    clearVisualSelection();
    invalidateExecutionForStructureChange();
    renderStructureState(HashTableViewState.empty(table.capacity()));
    refreshStatsDisplay();
  }

  private HashTableVisualizer hashVisualizer() {
    return (HashTableVisualizer) visualizer;
  }

  public sealed interface Selection permits EntrySelection, BucketSelection {}

  public record EntrySelection(
      long id,
      int bucketIndex,
      VisualValue key,
      VisualValue value,
      int size,
      int capacity) implements Selection {}

  public record BucketSelection(
      int bucketIndex,
      int entryCount,
      int size,
      int capacity) implements Selection {}
}
