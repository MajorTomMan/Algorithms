package com.majortom.algorithms.core.io;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.databind.MappingIterator;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

/**
 * Reads a header-based CSV table into a list of caller-selected row objects.
 *
 * <p>The first row supplies property names. Quoted commas, escaped double quotes and
 * embedded newlines are handled by Jackson CSV. The input stream belongs to the caller.
 *
 * @param <T> the type of each row (not the List type)
 */
public final class CsvDataReader<T> implements DataReader<List<T>> {
  private static final CsvMapper CSV = new CsvMapper();
  private static final CsvSchema HEADER = CsvSchema.emptySchema().withHeader();

  private final JavaType rowType;

  public CsvDataReader(Class<T> rowType) {
    this.rowType = CSV.constructType(Objects.requireNonNull(rowType, "rowType"));
  }

  public CsvDataReader(TypeReference<T> rowType) {
    this.rowType = CSV.constructType(Objects.requireNonNull(rowType, "rowType"));
  }

  @Override
  public List<T> read(InputStream input) throws IOException {
    Objects.requireNonNull(input, "input");
    try (MappingIterator<T> rows = CSV.readerFor(rowType).with(HEADER).readValues(input)) {
      return List.copyOf(rows.readAll());
    }
  }
}
