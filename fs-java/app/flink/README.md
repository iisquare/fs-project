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

- 批处理算子
```
DataSet
```
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
