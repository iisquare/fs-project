# flink

Flink计算框架，侧重流式处理。

## JDK17

### VM options

- Run/Debug Configurations > Add Run Options > Add VM options
```
--add-opens java.base/java.util=ALL-UNNAMED
--add-exports java.base/sun.reflect.annotation=ALL-UNNAMED
```

## 最佳实践

### 结构信息

```
stream.returns(Types.LIST(Types.STRING));
```

### 数据交互

Flink 2.x 已移除 DataSet API，流批统一基于 DataStream API，通过执行环境上的
`RuntimeExecutionMode`（`STREAMING`/`BATCH`）切换：

- `model=stream` → `RuntimeExecutionMode.STREAMING`
- `model=batch` → `RuntimeExecutionMode.BATCH`，数据源必须声明为有界（BOUNDED）

批处理SQL算子借助Table API（`StreamTableEnvironment`）实现，运行时依赖
`flink-table-planner-loader` 与 `flink-table-runtime`。

- 流处理算子
```
DataStream
```
- 数据格式
```
JsonNode
```
- 合并返回值
```
FlinkUtil.union()
```

### 插件开发

- 在 `ext:flink` 本地调试时无需构建 JAR：清空 `jarURI` 并填写完整 `pkgClass`，节点会从当前进程 classpath 加载。
- 正式运行时应配置可被计算节点访问的 `jarURI`，由 `JarClassLoader` 加载插件。
- `JarClassLoader` 使用标准双亲委派；不要将框架主依赖重复打包进插件，否则可能因类型来自不同 Class 实例而导致校验失败。
- 本地调试可使用 `FlinkApplication.debug(uri)`，默认限制 JDBC 数据源读取 200 行；`FlinkApplication.debug(uri, 0)` 表示不限制。
