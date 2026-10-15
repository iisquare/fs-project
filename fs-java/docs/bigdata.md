## Hadoop

- JDK: 17
- Hadoop: 3.5.x
- Scala: 2.12
- Spark: 3.5.x
- Flink: 2.x

### Hadoop 3.3.x (3.3.6)

编译时必须使用JDK 8，不支持用JDK 11进行编译；JDK 8 和 JDK 11（仅运行时）。

### Hadoop 3.5.x (3.5.0)

This is the first Hadoop release with full support for Java 17. On the server side, Java 17 is required. On the client side, Java 17 and Java 21 are supported.



## Spark

### Spark 3.x (3.5.9)

Pre-built for Apache Hadoop 3.3 and later

Spark runs on Java 8/11/17, Scala 2.12/2.13, Python 3.8+, and R 3.5+. Java 8 prior to version 8u371 support is deprecated as of Spark 3.5.0. When using the Scala API, it is necessary for applications to use the same version of Scala that Spark was compiled for. For example, when using Scala 2.13, use Spark compiled for 2.13, and compile code/applications for Scala 2.13 as well.

For Java 11, setting `-Dio.netty.tryReflectionSetAccessible=true` is required for the Apache Arrow library. This prevents the `java.lang.UnsupportedOperationException: sun.misc.Unsafe or java.nio.DirectByteBuffer.(long, int) not available` error when Apache Arrow uses Netty internally.

### Spark 4.x (4.1.3 / 4.2.0)

Pre-built for Apache Hadoop 3.4 and later

Spark runs on Java 17/21, Scala 2.13, Python 3.10+, and R 3.5+ (Deprecated). When using the Scala API, it is necessary for applications to use the same version of Scala that Spark was compiled for. Since Spark 4.0.0, it’s Scala 2.13.

Spark runs on Java 17/21/25, Scala 2.13, Python 3.10+, and R 4.0+ (Deprecated). Java 25 prior to version 25.0.3 support is deprecated as of Spark 4.2.0. When using the Scala API, it is necessary for applications to use the same version of Scala that Spark was compiled for. Since Spark 4.0.0, it’s Scala 2.13.



## Flink

### Flink 1.x (1.20.5)

Flink runs on all UNIX-like environments, i.e. Linux, Mac OS X, and Cygwin (for Windows). You need to have **Java 11** installed. To check the Java version installed, type in your terminal:

Since version 1.15 Flink dropped the support of Scala 2.11 and it will use Scala 2.12 to build by default.

### Flink 2.x (2.2.1 / 2.3.0)

Requires Java 11, 17, or 21，Support for Java 11 was added in 1.10.0.

We use Java 17 by default in Flink 2.0.0 and is the recommended Java version to run Flink on. This is the default version for docker images.

Support for Java Records was added in Flink 1.19 ([FLINK-32380](https://issues.apache.org/jira/browse/FLINK-32380)). Java records are handled as [POJO types](https://nightlies.apache.org/flink/flink-docs-release-2.3/docs/dev/datastream/fault-tolerance/serialization/types_serialization/#pojos) and serialized via their canonical constructor.

Experimental support for Java 21 was added in 2.0.0. ([FLINK-33163](https://issues.apache.org/jira/browse/FLINK-33163))

- 1. Flink仍然只支持Scala 2.12.7，并且只能通过破坏兼容性升级到Scala 2.12的更高版本。
- 2. Flink不支持Scala 2.13。
- 3. 与Scala中的DataStream API相比，Java中的DataStream API具有更多可用的特性。

鉴于Scala的当前状态，本FLIP建议弃用Flink 1.17中的所有Scala API，并从Flink 2.0中删除所有Scala API。



## Flink CDC

### Flink CDC 3.7.x (3.5.0 / 3.6.0-1.20 / 3.6.0-2.2)

- **JDK**: JDK 11 or later (Flink CDC is built on JDK 11 since 3.6.0)
- **Apache Flink**: Flink 1.20.x or Flink 2.2.x



## 生态集成

### 流批一体（BI 计算编排）

BI 模块的计算编排对 Flink 与 Spark 均支持流式计算与批处理，由流程的 `engine` 与 `model` 组合决定：

| 引擎 \ 模式 | batch | stream |
| --- | --- | --- |
| Flink（web:flink） | DataStream BATCH 模式；SQL 走 Table API；JDBC 使用内置有界 Source | DataStream STREAMING 模式（Kafka、MySQL CDC 等） |
| Spark（web:spark） | Spark SQL 批处理（JDBC、Mongo 等） | Structured Streaming（Kafka 读写、控制台输出） |

实现要点：

1. Flink 2.x 移除了 DataSet API，流批统一由 DataStream API 承载，仅通过 `RuntimeExecutionMode`
   （`STREAMING`/`BATCH`）区分；BATCH 模式要求数据源声明为 `Boundedness.BOUNDED`。
2. 官方 `flink-connector-jdbc` 目前最高支持 Flink 1.20（无 2.x 版本），故 Flink 侧 JDBC 批处理
   使用内置的单分片 FLIP-27 有界 Source（`app:flink` 的 `JDBCBoundedSource`），需自行提供 JDBC 驱动。
3. Flink 批处理 SQL 依赖 Table API，运行期需要 `flink-table-planner-loader` 与 `flink-table-runtime`。
4. Spark 流式作业以异步方式运行：提交后返回流查询标识，运行状态与取消通过流查询注册表管理。

### Kafka

```bash
# Flink
implementation group: 'org.apache.flink', name: 'flink-connector-kafka', version: '3.4.0-1.20'
implementation group: 'org.apache.flink', name: 'flink-connector-kafka', version: '5.0.0-2.2'
# Spark
testImplementation group: 'org.apache.spark', name: 'spark-sql-kafka-0-10_2.13', version: '4.1.2'
testImplementation group: 'org.apache.spark', name: 'spark-sql-kafka-0-10_2.13', version: '4.2.0-preview5'
```



### Elasticsearch

```bash
# Flink
implementation group: 'org.apache.flink', name: 'flink-connector-elasticsearch7', version: '3.1.0-1.20'
implementation group: 'org.apache.flink', name: 'flink-connector-elasticsearch7', version: '4.0.0-2.0'
implementation group: 'org.apache.flink', name: 'flink-connector-elasticsearch8', version: '3.1.0-1.20'
implementation group: 'org.apache.flink', name: 'flink-connector-elasticsearch8', version: '4.0.0-2.0'
# Spark (Only 3.x)
compileOnly group: 'org.elasticsearch', name: 'elasticsearch-spark-30_2.13', version: '8.19.18'
```

## 参考连接：

- [winutils](https://github.com/cdarlint/winutils)
- [Native windows libraries for hadoop](https://github.com/notepass/hadoop-native-win-libs)
