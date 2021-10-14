# spark

Spark计算框架，侧重离线计算。

## 最佳实践

### 插件开发

- ScriptTransformNode的jarURI在本地开发时通过JarClassLoader加载，线上直接通过CLI提交文件后清除jarURI值。
- 在app中调试plugins需要配置对应的jarURI路径；在plugins中调试app则无需配置当前jarURI路径，plugins即为app。
- JarClassLoader会优先加载同一个Jar文件下的父类，请勿将主依赖打包进插件中，否则isAssignableFrom校验不通过。
