# spark

Spark计算框架，侧重离线计算。

## JDK17

Spark runs on Java 17/21, Scala 2.13, Python 3.9+, and R 3.5+ (Deprecated).
When using the Scala API, it is necessary for applications to use the same version of Scala that Spark was compiled for.
Since Spark 4.0.0, it’s Scala 2.13.

由于周边生态组件暂未适配Spark4.x，故短期内暂不升级Spark版本，通过兼容JDK8的方式运行。

### VM options

- Run/Debug Configurations > Add Run Options > Add VM options
```
-Dfile.encoding=UTF-8
-XX:+IgnoreUnrecognizedVMOptions
# fixed: module java.base does not export sun.nio.ch to unnamed module (Spark >= 3.4，StorageUtils 引用 sun.nio.ch.DirectBuffer)
--add-opens java.base/sun.nio.ch=ALL-UNNAMED
# 以下为 Spark 3.5 官方默认模块参数，等同于 org.apache.spark.launcher.JavaModuleOptions#defaultModuleOptions()
--add-opens java.base/java.lang=ALL-UNNAMED
--add-opens java.base/java.lang.invoke=ALL-UNNAMED
--add-opens java.base/java.lang.reflect=ALL-UNNAMED
--add-opens java.base/java.io=ALL-UNNAMED
--add-opens java.base/java.net=ALL-UNNAMED
--add-opens java.base/java.nio=ALL-UNNAMED
--add-opens java.base/java.util=ALL-UNNAMED
--add-opens java.base/java.util.concurrent=ALL-UNNAMED
--add-opens java.base/java.util.concurrent.atomic=ALL-UNNAMED
--add-opens java.base/jdk.internal.ref=ALL-UNNAMED
--add-opens java.base/sun.nio.cs=ALL-UNNAMED
--add-opens java.base/sun.security.action=ALL-UNNAMED
--add-opens java.base/sun.util.calendar=ALL-UNNAMED
--add-opens java.security.jgss/sun.security.krb5=ALL-UNNAMED
-Djdk.reflect.useDirectMethodHandle=false
```

## 最佳实践

### 结构信息

```
Encoder<Row> encoder = dataset.encoder();
String ddl = encoder.schema().toDDL();
StructType schema = StructType.fromDDL(ddl);
StructType schema = DataTypes.createStructType(Arrays.asList(
    DataTypes.createStructField("table", DataTypes.StringType, false),
    DataTypes.createStructField("message", DataTypes.StringType, true)
));
Encoder<Row> encoder = RowEncoder.encoderFor(schema);
Encoder<YourBean> encoder = Encoders.bean(YourBean.class);
Encoder<JsonNode> encoder = Encoders.javaSerialization(JsonNode.class);
```

### 修改JDBC连接参数

在某些情况下，如Postgresql只有setAutoCommit(false)的情况下，fetchSize才生效，所以需要定制Provider。
因JdbcRelationProvider.createRelation没有直接提供Connection，基于DataSourceV2自定义数据源完全开发的成本又过高。
故采用自定义数据库驱动的方式用来拦截连接器。

- 自定义Driver驱动
```
public class PostgresDriver extends org.postgresql.Driver {
    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        Connection connection = super.connect(url, info);
        if (null != connection) connection.setAutoCommit(false);
        return connection;
    }
}
```
- 采用自定义驱动读取数据源
```
Map<String, String> config = new LinkedHashMap<>();
config.put("driver", PostgresDriver.class.getName());
config.put("fetchSize", 500);
session.read().format("jdbc").options(config).load();
```

### 常见问题

- 申请资源死循环

Driver端为本地代码，远程服务端Executor需要反向连接Driver进行通信，域名无法解析或链路不通导致通信失败。

- 类转换异常
```
java.lang.ClassCastException: cannot assign instance of java.lang.invoke.SerializedLambda to field org.apache.spark.rdd.MapPartitionsRDD.f of type scala.Function3 in instance of org.apache.spark.rdd.MapPartitionsRDD
```
将依赖上传至jars目录，仅将当前代码打包为瘦Jar，通过setJars参数作为第三方Jar附加到作业中。
```
SparkConf config = new SparkConf();
config.setAppName("remote-test");
config.setMaster("spark://m1:7077,m2:7077,m3:7077");
config.setJars(new String[]{ "http://path/to/app.jar" });
SparkSession session = SparkSession.builder().config(config).getOrCreate();
```

### Excel 数据源

Excel 读取由 `ExcelUtil` 基于 Apache POI 实现，不再依赖 `spark-excel`。首版支持
xls/xlsx、指定工作表、表头、基础类型推断、公式计算值和空行过滤。

代码中读取：

```java
Dataset<Row> dataset = ExcelUtil.read(
        session,
        "/data/input.xlsx",
        "'Sheet1'!",
        true
);
```

编排节点 `ExcelSource` 的常用配置：

```json
{
  "path": "file:///data/input.xlsx",
  "dataAddress": "'Sheet1'!",
  "header": true,
  "headerRow": 0,
  "limit": 1000,
  "maxRows": 100000,
  "maxColumns": 1024,
  "maxBytes": 104857600
}
```

Excel 会先由 Driver 读取，再创建 Dataset，适用于配置导入、调试和中小规模数据。
当前不处理合并单元格、加密文件、复杂公式和超大工作簿；超大文件应优先转换为
Parquet/CSV 后再进入 Spark。

可指定类运行读取测试：

```
gradle :app:spark:test -PrunSparkTests \
  --tests com.iisquare.fs.app.spark.util.ExcelUtilTests
```

### 插件开发

- 在 `ext:spark` 本地调试时无需构建 JAR：清空 `jarURI` 并填写完整 `pkgClass`，节点会从当前进程 classpath 加载。
- 正式运行时应配置可被计算节点访问的 `jarURI`，由 `JarClassLoader` 加载插件。
- `JarClassLoader` 使用标准双亲委派；不要将框架主依赖重复打包进插件，否则可能因类型来自不同 Class 实例而导致校验失败。
- 本地调试可使用 `SparkApplication.debug(uri)`，默认限制数据源读取 200 行；`SparkApplication.debug(uri, 0)` 表示不限制。

## 参考
- [Spark远程调用的几个坑](https://www.cnblogs.com/hanko/p/14086667.html)
- [Data Sources](https://spark.apache.org/docs/latest/sql-data-sources.html)
- [Elasticsearch for Apache Hadoop](https://www.elastic.co/guide/en/elasticsearch/hadoop/current/configuration.html)
