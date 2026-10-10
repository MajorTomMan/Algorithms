package com.majortom.algorithms.dataio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/** Path convenience functions; the reader/writer themselves remain stream-based. */
public final class FileDataIO {
  private FileDataIO() {
  }

  public static <T> T read(Path path, DataReader<T> reader) throws IOException {
    Objects.requireNonNull(path, "path");
    Objects.requireNonNull(reader, "reader");
    try (InputStream input = Files.newInputStream(path)) {
      return reader.read(input);
    }
  }

  /**
   * Writes a sibling temporary file before replacing the destination.
   * The parent directory must exist; if atomic move is unsupported, normal replacement is used.
   */
  public static <T> void write(Path path, T value, DataWriter<? super T> writer)
      throws IOException {
    Objects.requireNonNull(path, "path");
    Objects.requireNonNull(value, "value");
    Objects.requireNonNull(writer, "writer");

    Path destination = path.toAbsolutePath();
    Path directory = Objects.requireNonNull(destination.getParent(), "parent directory");
    Path fileName = Objects.requireNonNull(destination.getFileName(), "file name");
    Path temporary = Files.createTempFile(directory, "." + fileName + ".", ".tmp");

    try {
      try (OutputStream output = Files.newOutputStream(
          temporary, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
        writer.write(value, output);
      }

      try {
        Files.move(temporary, destination,
            StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException unsupported) {
        Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(temporary);
    }
  }
}
