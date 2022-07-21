# spark

Spark计算框架，侧重离线计算。

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
ExpressionEncoder<Row> encoder = RowEncoder.apply(schema);
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

### 插件开发

- ScriptTransformNode的jarURI在本地开发时通过JarClassLoader加载，线上直接通过CLI提交文件后清除jarURI值。
- 在app中调试plugins需要配置对应的jarURI路径；在plugins中调试app则无需配置当前jarURI路径，plugins即为app。
- JarClassLoader会优先加载同一个Jar文件下的父类，请勿将主依赖打包进插件中，否则isAssignableFrom校验不通过。

## 参考
- [spark远程调用的几个坑](https://www.cnblogs.com/hanko/p/14086667.html)
