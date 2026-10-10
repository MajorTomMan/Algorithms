package com.majortom.algorithms.visualization.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.majortom.algorithms.dataio.FileDataIO;
import com.majortom.algorithms.dataio.JsonDataCodec;
import com.majortom.algorithms.structure.array.Array;
import com.majortom.algorithms.structure.array.ArrayInitializer;
import com.majortom.algorithms.structure.array.ArrayStructure;
import com.majortom.algorithms.structure.graph.Graph;
import com.majortom.algorithms.structure.graph.GraphData;
import com.majortom.algorithms.structure.graph.GraphInitializer;
import com.majortom.algorithms.structure.graph.GraphStructure;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Application-layer composition of independent data I/O and structure initializers.
 * No file format or I/O implementation is needed in the structures module.
 */
public final class StructureDataFiles {
    private StructureDataFiles() {}

    public static <T> Graph<T> loadGraph(Path file, TypeReference<GraphData<T>> type)
            throws IOException {
        GraphData<T> data = FileDataIO.read(file, new JsonDataCodec<>(type));
        return new GraphInitializer<T>().create(data);
    }

    public static <T> void saveGraph(Path file, GraphStructure<T> graph,
                                      TypeReference<GraphData<T>> type) throws IOException {
        Objects.requireNonNull(graph, "graph");
        GraphData<T> data = new GraphInitializer<T>().export(graph);
        FileDataIO.write(file, data, new JsonDataCodec<>(type));
    }

    public static <T> Array<T> loadArray(Path file, TypeReference<List<T>> type)
            throws IOException {
        List<T> data = FileDataIO.read(file, new JsonDataCodec<>(type));
        return new ArrayInitializer<T>().create(data);
    }

    public static <T> void saveArray(Path file, ArrayStructure<T> array,
                                      TypeReference<List<T>> type) throws IOException {
        Objects.requireNonNull(array, "array");
        List<T> values = new ArrayList<>();
        for (T value : array) {
            values.add(value);
        }
        FileDataIO.write(file, values, new JsonDataCodec<>(type));
    }
}
