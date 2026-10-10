# data-io

独立的 Java 数据读写模块。只负责外部字节流与普通 Java 数据对象之间的转换；不依赖 `core`、`structures`、`algorithms` 或 JavaFX。

## 接口

- `DataReader<T>`：从调用方管理的 `InputStream` 读取数据。
- `DataWriter<T>`：向调用方管理的 `OutputStream` 写入数据。
- `DataCodec<T>`：同时具备读取和写入能力。
- `JsonDataCodec<T>`：Jackson JSON 实现，支持 `Class<T>` 和 `TypeReference<T>`。
- `FileDataIO`：通过 `Path` 读写文件，自动关闭文件流。写入先序列化到同目录临时文件，再尝试原子替换；文件系统不支持原子移动时退化为普通替换。

## 使用示例

```java
import com.fasterxml.jackson.core.type.TypeReference;
import com.majortom.algorithms.dataio.FileDataIO;
import com.majortom.algorithms.dataio.JsonDataCodec;
import java.nio.file.Path;
import java.util.List;

record City(String code, String name) {}

JsonDataCodec<List<City>> codec =
    new JsonDataCodec<>(new TypeReference<List<City>>() {});

Path file = Path.of("cities.json");
FileDataIO.write(file, List.of(new City("440300", "深圳市")), codec);
List<City> cities = FileDataIO.read(file, codec);
```

流模式适用于资源文件、内存缓冲区等场景。调用方负责关闭流；`JsonDataCodec` 不会主动关闭输入/输出流。

文件模式的目标文件所在目录须已存在。若读取失败，调用方拿不到新数据；若写入或序列化在替换前失败，原目标文件不会被提前截断。非原子文件系统不保证最后一步的替换是原子的。

解析得到的数据对象由业务层验证并应用。例如 JSON 可以映射为业务自定义的 `GraphData<T>`，但 `data-io` 不负责初始化 `Graph`、记录事件或刷新界面。

## 与算法框架组合

此模块不引用 `Graph`、`Array` 或任何 `StructureInitializer`。结构特定的数据建模、校验、初始化和导出全部留在 `structures`，客户端通过 `StructureDataFiles` 组合两端。参见 [初始化与文件读写示例](../docs/数据初始化与文件读写.md)。
