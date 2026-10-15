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

- ScriptTransformNode的jarURI在本地开发时通过JarClassLoader加载，线上直接通过CLI提交文件后清除jarURI值。
- 在app中调试plugins需要配置对应的jarURI路径；在plugins中调试app则无需配置当前jarURI路径，plugins即为app。
- JarClassLoader会优先加载同一个Jar文件下的父类，请勿将主依赖打包进插件中，否则isAssignableFrom校验不通过。
