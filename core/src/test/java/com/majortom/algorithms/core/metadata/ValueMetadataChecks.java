package com.majortom.algorithms.core.metadata;

import com.majortom.algorithms.core.annotation.GraphNodes;
import com.majortom.algorithms.core.annotation.ValueDisplay;
import com.majortom.algorithms.core.annotation.ValueField;
import com.majortom.algorithms.core.annotation.ValueId;
import com.majortom.algorithms.core.annotation.ValueType;
import java.util.List;

/** Standalone checks for record-component propagation, method access and shared metadata. */
public final class ValueMetadataChecks {
  @ValueType(name = "City")
  record City(@ValueId String code,
      @ValueField(value = {ValueDisplay.SUMMARY, ValueDisplay.DETAIL}, label = "名称") String name) {
    @ValueField(label = "Area")
    int area() { return 99; }
  }
  record Network(@GraphNodes List<City> cities) {}
  public static void main(String[] args) {
    City city = new City("sz", "深圳");
    var d = ValueMetadata.describe(City.class);
    if (!d.declared() || !d.name().equals("City") || !d.businessIdOf(city).equals("sz")
        || d.fields().size() != 2 || !d.fields().getFirst().read(city).equals("深圳"))
      throw new AssertionError("record metadata");
    if (AnnotatedMembers.marked(Network.class, GraphNodes.class).size() != 1)
      throw new AssertionError("record component was scanned more than once");
    System.out.println("ValueMetadataChecks passed");
  }
}
