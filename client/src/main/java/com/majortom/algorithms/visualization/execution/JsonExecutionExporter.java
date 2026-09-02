package com.majortom.algorithms.visualization.execution;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.majortom.algorithms.core.runtime.ExecutionSummary;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/** JSON file exporter for desktop execution records. */
public final class JsonExecutionExporter implements ExecutionExporter {

    private static final DateTimeFormatter FILE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private final Path exportDirectory;
    private final ObjectMapper mapper;
    private final ExecutionExportCodec codec;
    private final Clock clock;

    public JsonExecutionExporter(Path exportDirectory, ObjectMapper mapper, ExecutionExportCodec codec) {
        this(exportDirectory, mapper, codec, Clock.systemDefaultZone());
    }

    JsonExecutionExporter(
            Path exportDirectory,
            ObjectMapper mapper,
            ExecutionExportCodec codec,
            Clock clock) {
        this.exportDirectory = Objects.requireNonNull(exportDirectory, "exportDirectory");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.codec = Objects.requireNonNull(codec, "codec");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public Path export(ClientExecutionRecord record, ExecutionSummary summary) throws IOException {
        Objects.requireNonNull(record, "record");
        Objects.requireNonNull(summary, "summary");
        Files.createDirectories(exportDirectory);
        String timestamp = LocalDateTime.now(clock)
                .format(FILE_TIMESTAMP);
        Path file = exportDirectory.resolve(record.moduleId() + "_" + record.operationId()
                + "_" + timestamp + "_" + safeFilePart(record.recording().runId()) + ".json");
        try (OutputStream output = Files.newOutputStream(
                file, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(output, codec.encode(record, summary));
        }
        return file;
    }

    private String safeFilePart(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (Character.isLetterOrDigit(character) || character == '-' || character == '_'
                    || character == '.') {
                result.append(character);
            } else {
                result.append('_');
            }
        }
        return result.toString();
    }
}
