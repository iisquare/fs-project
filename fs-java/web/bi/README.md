# BI(Business Intelligence，商业智能)

数据矩阵、智能报表、自定义大屏。

## 技术

- 大数据：异构数据源计算。
- 人工智能：机器学习、智能推荐。
- 大屏设计器：数据可视化、钻取、组态化。

## 概念

### 大数据标准化
1. 数据接入。
2. 数据预处理。
3. 数据存储。
4. 数据处理。
5. 数据可视化。
6. 数据治理。
7. 安全与隐私保护。

## 功能

### 数据源
- 数据源仅配置数据存储服务，Excel和API单独管理。
- 通过SQL创建数据集，支持定时任务持久化到Doris中用于加速查询。
- 将数据源接入Trino中，数据集作为视图面向应用端提供服务。
- 智能查询可以配置数据集、数据集关联关系，由大模型生成SQL查询结果。
- 数据集SQL支持内置变量：用户标识、用户名、用户昵称、角色（逗号分隔的多个ID、空时为0）。

### 数据加工
- 基于DAG数据流程图，组合算子，生成目标数据。
- 支持近实时流计算和离线批处理。
- 支持传统机器学习算法、常用深度学习模型、智能推荐等功能。

### 数据集，基于数据源
- 基于单个或多个数据源生成数据维度和度量，自动维护计算结果。
- 支持表、字段别名，表关联、过滤，数据筛选，分组聚合。
- 支持在线预览、自定义查询，提供API调用和批量导出等功能。

### 数据矩阵，基于数据集
- 指定数据集的某一列作为行号，用于展示定制化数据表单。
- 按照行列确定数据单元，支持单元格公式计算。
- 支持循环列表配置，采用特殊字符标识行开始和行结束。
- 支持单元格引用分析，可视化追溯数据来源。

### 数据主题，基于数据集
- 组合多个数据集，并以外键方式配置数据集之间的多字段关联关系。
- 支持主题标签与角色授权，作为统一的数据语义配置供智能查询、报表等场景复用。

### 查询日志，基于查询行为
- 只记录真正执行的 SQL 查询：即席查询（SQL查询）、数据集查询与数据主题查询统一记录查询日志；
  获取主题详情、数据字典等元数据操作不产生日志，日志表里不会再出现空 SQL 的记录。
- 日志包含查询时间、查询用户、来源IP、请求地址、查询语句、耗时、返回行数与结果状态等关键信息；
  失败时额外记录异常原因与详细内容（异常消息及根因、SQL解析或数据集校验明细），便于定位问题。
- 智能体（大模型）数据主题工具按主题发起的查询会带上主题标识，日志中可直接定位查询所属的数据主题。
- 日志写入失败不影响查询结果；支持按查询类型、状态、查询对象、查询用户与查询时间范围检索。

### 定时任务
- 可手动或定时触发数据加工、数据集、数据矩阵计算任务。
- 并设定中间计算结果缓存日期格式和保留时长。
- 支持防抖、节流等配置，自动维护失败重试队列，数据写入具备幂等性。

### 报表，基于数据集
- 基于单个数据集创建数据表格或报表。
- 支持动态查询计算，用于层级结构的钻取。
- 支持指标、饼图、柱状图、雷达、漏斗、桑基、关系穿透等常用图形。
- 提供报表分享和iframe嵌入，可快速集成在第三方系统页面中。

### 数据大屏，基于数据集
- 提供独立的大屏设计器，通过拖拽、点选和属性面板组合文本、指标卡、表格、常用图表及 AntV DataV 风格标题、边框、扫描、网格和分割装饰。
- 布局容器支持自由定位、相对定位、固定定位、弹性布局和栅格布局，并可递归嵌套；弹性布局可配置方向、换行、主轴/交叉轴对齐及子项伸缩，定位布局的位置和宽高支持像素、百分比及撑满父容器；栅格布局支持固定行高和按行列数等分填满。
- 画布区分设计基准与显示尺寸，显示宽高支持像素和百分比；图表、表格和文本在组件内部自适应尺寸。
- 右侧属性面板分为“数据”和“样式”页签，采用紧凑折叠分组、双列数值输入和可视化对齐操作；通用样式覆盖填充、描边、内外边距、字体、阴影、背景模糊、对齐、透明度、溢出与叠放层级。
- 每个数据组件独立选择数据集，自由组合多个维度和多个聚合指标，并支持 AND/OR 条件过滤。
- 画布按 1920 × 1080 设计，编辑器和只读预览均等比适配屏幕；预览页面支持浏览器全屏。
- 提供实时数据刷新、撤销/重做、组件复制/删除、图层面板和未保存提醒。
- 提供大屏模板管理。模板在独立配置页集中维护基础信息、页面布局、通用组件样式和特殊组件个性化样式；基于模板创建大屏时生成空白画布并继承模板配置。
- 新建和编辑大屏均直接进入设计器，大屏名称、状态、排序、描述和引用模板在右侧“数据”页签的根画布信息中维护，通过顶部“保存”统一提交。
- 大屏保存引用的模板 ID。已有大屏切换模板时只更新后续新增组件的默认样式，现有组件和画布保持不变。

### 故事，基于报表
- 在线制作PPT，方便动态展示数据报表。

## 接口

| 接口 | 说明 | 权限 |
| --- | --- | --- |
| POST `/dataQueryLog/list` | 查询日志列表，支持类型、状态、查询对象、查询用户与查询时间过滤 | `bi:dataQueryLog:` |
| POST `/dataQueryLog/info` | 查询日志详情 | `bi:dataQueryLog:` |
| POST `/dataQueryLog/delete` | 删除查询日志 | `bi:dataQueryLog:delete` |
| POST `/dataQueryLog/config` | 查询类型、状态与排序字典 | `bi:dataQueryLog:` |
| POST `/dashboard/list` | 数据大屏列表 | `bi:dashboard:` |
| POST `/dashboard/info` | 数据大屏详情 | `bi:dashboard:` |
| POST `/dashboard/save` | 新增或修改数据大屏；保存 `templateId` 引用，新建时应用模板布局与组件默认样式 | `bi:dashboard:add` / `bi:dashboard:modify` |
| POST `/dashboard/delete` | 删除数据大屏 | `bi:dashboard:delete` |
| POST `/dashboard/search` | 按数据集、维度、指标和过滤条件查询图表数据 | `bi:dashboard:search` |
| POST `/dashboard/templates` | 获取创建大屏可用的启用模板 | `bi:dashboard:` |
| POST `/dashboard/template` | 获取指定启用模板的完整配置，用于设计器即时应用 | `bi:dashboard:` |
| POST `/dashboardTemplate/list` | 大屏模板列表 | `bi:dashboardTemplate:` |
| POST `/dashboardTemplate/info` | 大屏模板详情 | `bi:dashboardTemplate:` |
| POST `/dashboardTemplate/save` | 新增或修改大屏模板 | `bi:dashboardTemplate:add` / `bi:dashboardTemplate:modify` |
| POST `/dashboardTemplate/delete` | 删除大屏模板 | `bi:dashboardTemplate:delete` |

数据大屏模板建表、菜单和权限初始化脚本见 `docs/fs-project-bi-dashboard-template.sql`。

## 最佳实践

### 即席查询

- Spark可动态配置数据源，支持UDF和嵌入MapReduce处理
- Trino谓词下推的查询性能较高，通过配置文件接入数据源
- 在处理大数据量小范围的筛选查询时，可将固定逻辑配置为Trino查询，结合Spark处理动态逻辑

# BI 流批一体计算任务编排

在 `bi` 模块下提供流批一体的计算任务编排能力：流计算通过 `web:flink` 执行，批处理通过
`web:spark` 执行；支持指定节点调试、批处理定时调度、远程集群配置与提交、作业运行状态监控
预警与自动恢复。

## 数据模型

| 表 | 说明 |
| --- | --- |
| `fs_bi_cluster` | 计算集群配置（本地/远程 Spark、Flink），`content` 为 JSON 提交配置 |
| `fs_bi_job` | 作业运行记录，用于状态追踪、预警与自动恢复 |
| `fs_bi_diagram` | 追加 `cluster_id`、`expression`、`schedule`、`notify`、`auto_recover`、`max_retry`、`last_time`、`next_time` |

建表脚本见 `docs/fs-project-bi-job.sql`。

## 集群配置示例

Flink（REST 提交）：

```json
{
  "submitType": "rest",
  "rest": "http://flink-host:8081",
  "jar": "/opt/jobs/fs-project-thin-flink.jar",
  "entryClass": "com.iisquare.fs.app.flink.FlinkApplication",
  "parallelism": 2
}
```

Spark（命令行提交）：

```json
{
  "submitType": "command",
  "master": "yarn",
  "deployMode": "cluster",
  "home": "/opt/spark",
  "jar": "/opt/jobs/fs-project-thin-spark.jar",
  "entryClass": "com.iisquare.fs.app.spark.SparkApplication",
  "conf": { "spark.executor.memory": "2g" }
}
```

`jar` 为运行 `web:flink` / `web:spark` 服务的宿主机可读路径；提交时作业参数使用编排流程的
Base64 快照或 `diagramUrl`，由 `FlinkApplication` / `SparkApplication` 的 `main` 载入执行。

## 接口

编排流程（`web:bi`）：

- `POST /bi/diagram/{info,list,save,delete,config}`：编排流程 CRUD
- `GET /bi/dag/diagram?id=`：导出计算有向无环图
- `POST /bi/dag/run`：提交作业（`{id, clusterId, data}`）
- `POST /bi/dag/debug`：指定节点调试（`{id, nodeId, previewLimit}`），返回各节点输出
- `POST /bi/dag/tables`：读取数据源可选资源（`{datasourceId}`），支持关系库表/视图、MongoDB 集合、Elasticsearch 索引和 Kafka 主题
- `POST /bi/dag/columns`：读取指定表的字段结构（`{datasourceId, table}`）

### 节点即时调试

调试请求会同时返回 `execution`（逐节点类型、耗时、状态、异常）和 `outputs`
（Dataset 的 schema、最多 20 行数据样例）。设计器在画布下方直接展示执行明细与输出样例，
不再跳转页面；`outputs` 键名使用 `dag_{diagramId}_{cellId}`，与画布节点保持一致。

调试模式会自动向上游数据源注入读取限制：

- MongoSource：聚合管道末尾追加 `$limit`，并使用单分区读取。
- JDBCSource：将原查询包装为 `SELECT * FROM (...) fs_debug_source LIMIT N`。
- ElasticsearchSource：通过 `es.scroll.limit` 限制实际拉取文档数，并在 Dataset 上再次限流。
- Neo4jSource / OntologySource：在 Cypher 末尾追加或收紧 `LIMIT N`；Cypher 使用参数化
  `LIMIT $limit` 时保留语句自身的参数约束。
- 正常作业提交不注入上述限制。

### Elasticsearch 与 Neo4j 节点

Spark 批处理提供以下通用数据源节点：

- `ElasticsearchSource`：按索引读取 Query DSL 结果，可限制返回字段和读取条数。
- `ElasticsearchSink`：按配置写入索引，支持 `index/create/update/upsert`、主键字段和批大小。
- `Neo4jSource`：执行带参数的 Cypher 查询。单返回列是节点或映射时，结果属性展开为
  Dataset 顶层字段；节点额外携带 `_elementId` 和 `_labels`，关系额外携带类型与端节点标识。
- `Neo4jSink`：上游 Dataset 每批转换为 `$rows`，通过 `UNWIND $rows AS row` 执行写入。
  Cypher 由节点自行提供，因此 MERGE、关系写入、更新和删除语义均可按业务定义。

Neo4j 数据源配置使用 Bolt/Neo4j URI、用户名、密码、数据库和连接池参数。Neo4j 节点引用的
数据源由“数据源管理”统一维护，执行前才展开连接凭据，不会写回编排配置。

### Kafka 消息队列节点

`KafkaSource` / `KafkaSink` 同时注册在 Spark 与 Flink 的流处理、批处理模式：

- 数据源既可通过“数据源管理”中的 Kafka 连接引用，也可继续在节点上直接填写服务地址，兼容历史编排。
- 支持 `PLAINTEXT`、`SASL_PLAINTEXT`、`SASL_SSL` 等安全协议和自定义消费者/生产者参数。
- Spark 读取结果保留 Kafka 原始列，并额外生成字符串 `message` 列；Flink 读取输出
  `{topic, partition, offset, timestamp, message}` JSON。
- 批量读取通过 `startingOffsets` / `endingOffsets` 形成有界区间，支持 `earliest`、`latest`
  和分区 offset JSON，例如 `{"orders":{"0":100,"1":200}}`；Flink 额外支持 `committed` 和
  `timestamp:毫秒`。
- 写入节点默认读取 `message` 字段，也可指定其他消息字段；Spark 与 Flink 均支持动态主题：
  从消息 JSON 指定字段取值。动态分区和 Flink 投递语义仅适用于 Flink。
- Spark 流写入使用独立 checkpoint，未配置时按节点名称自动生成，避免多个 Kafka 写入节点共用检查点。

RabbitMQ 当前没有随 Flink 2.x / Spark 3.5 一并提供的稳定通用连接器，本期暂不内置；
后续可按独立连接器或自定义 Source/Sink 扩展，不影响 Kafka 节点。

### CDC 变更捕获节点

Flink 流处理模式提供三类原生 CDC 读取节点：

- `MySQLCapture`：基于 Binlog，支持数据库/表正则、启动位置、Server ID、分片、结构变更和新增表扫描。
- `PostgreSQLCapture`：基于逻辑复制，支持 Schema/表正则、`pgoutput`/`decoderbufs`、复制槽和快照模式。
- `MongoCapture`：基于 Change Stream，支持数据库/集合正则、存量复制、完整变更日志和轮询参数。

三类节点都可以从“数据源管理”选择对应连接，并兼容直接填写连接信息的历史方式。输出统一为
Debezium JSON 字符串，可直接接入 Kafka 写入节点或后续自定义处理节点。

部署侧要求：

- MySQL 开启 Binlog，使用 `ROW` 格式并配置唯一的 Server ID。
- PostgreSQL 开启 logical replication，按需设置 `wal_level=logical`，并授予复制与发布槽权限。
- MongoDB 使用副本集或 mongos，并授予 Change Stream 所需权限。
- CDC 为持续流任务，依赖 Flink checkpoint 保存和恢复消费位置。

Spark 3.5 没有项目可直接复用的原生 CDC Source，本期没有用普通 JDBC 轮询模拟 CDC。

CDC Source 本身没有确定的结束位置，因此只在 Flink 流处理模式开放。设计器切换引擎或处理模式时，
节点面板不会隐藏其他节点，不兼容的节点会置灰并禁止拖入画布。

### 知识图谱本体节点

`OntologySource` 和 `OntologySink` 用于把 BI 编排与知识图谱本体联动：

- 读取：选择本体和实体后，按实体标签生成 `MATCH`；属性过滤和排序字段必须在本体中已声明。
- 写入：按实体主键生成 `MERGE`，并从上游 Dataset 中筛选本体已声明字段后写入 Neo4j。
- 本体中的必填字段会作为写入前置校验；未声明字段不会写入图节点。
- 本体选择只用于生成 Cypher 与校验，图数据仍由节点选择的 Neo4j 数据源承载。

通用 `Neo4jSource/Neo4jSink` 适合复杂 Cypher；本体节点适合遵循知识图谱模型的标准化读写，
两者可以混合使用。

### 通用处理节点

设计器的“数据加工”分组提供以下处理节点：

- `FilterTransform`：按字段和条件过滤数据。Spark 将上游 Dataset 转为过滤表达式；Flink
  同时兼容 `Row`、`JsonNode` 和 JSON 字符串输入，CDC 数据可使用 `after.status` 等点路径。
- `UnionTransform`：按入边顺序合并多个上游数据，要求参与合并的数据结构一致。
- `ConvertTransform`：按 `target/source/clsType` 映射字段并转换类型。Spark 输出 Dataset，
  Flink Row 输入保持 Row 输出，JSON 和 JSON 字符串输入输出 JsonNode。设计器会根据
  映射节点的连线读取上游输出 schema，并从下游 JDBC 目标表读取列定义；字段可点击加入映射，
  属性面板以表格展示字段、anchor 和连接线，可拖拽 anchor 重新连接或按同名自动匹配。
- `WindowTransform`：Flink 流处理窗口，支持可选的按键字段。窗口达到最大条数或窗口时间
  到期时触发，复用 `CountTimeoutTrigger`，输出包含窗口起止时间、键、条数和明细的 JSON 批次。
- `AggregateTransform`：按 `groupBy` 字段分组，并通过 `aggregations` 配置
  `count(*) AS total` 等聚合表达式。
- `SortTransform`：按指定字段升序或降序排列。
- `LimitTransform`：限制输出行数。

过滤和数据合并支持流处理与批处理。分组聚合、排序和行数限制只在批处理模式开放，
避免在无界流上引入无限状态或无法得出结果的处理语义；更复杂的连接、窗口和去重逻辑
可继续使用 SQL 节点或自定义 JAR 节点。Flink 窗口当前使用处理时间语义。

预览行数可通过后端配置调整：

```yaml
fs:
  bi:
    debug:
      preview-limit: 20
```

### 自定义 JAR 节点

设计器通过以下接口上传、扫描和下载节点 JAR：

- `POST /bi/diagram/jar/upload`：multipart 字段 `file`，仅接受 JAR
- `POST /bi/diagram/jar/list`：按 `keyword` 分页检索文件名、SHA-256 或作业节点
- `GET /bi/diagram/jar/info?id=`：读取单个 JAR 及已解析节点
- `GET /bi/diagram/jar/nodes?id=`：读取指定 JAR 内可用的 `DAGNode` 实现
- `GET /bi/diagram/jar/download?id=`：供计算引擎加载插件

插件类需实现 `com.iisquare.fs.base.dag.core.DAGSource`、`DAGTransform`、
`DAGSink` 或 `DAGConfig`。设计器会从 JAR 读取输入/输出类型，并将插件包装为
`ScriptTransform` 节点；拖入画布后按正常节点方式连接上下游即可执行。

JAR 文件本体通过文件服务保存到 `fs-bi` 桶，BI 服务数据库只保存文件标识、
对象路径、SHA-256、大小和节点元数据。上传时完成解析，之后列表检索直接查询数据库。
部署前需要预先创建 `fs-bi` 桶，并保证 BI 服务能够调用文件服务。

```yaml
fs:
  bi:
    jar:
      bucket: fs-bi
      public-base-url: http://127.0.0.1:7815
      max-size: 1073741824
```

`public-base-url` 必须是 Spark/Flink 计算节点可访问的 BI 地址。计算引擎请求
`/diagram/jar/download` 后，BI 服务会实时向文件服务换取带时效的下载地址并重定向。
因此 `fs.site.urls.file` 映射的文件服务地址也必须能被计算节点访问。

### Spider 官方站点清洗

`ext:spark` 提供两个批处理节点。官方站点清洗不再作为内置数据加工组件，
而是通过画布中的“JAR作业”节点加载：

1. 构建 `ext:spark` 的 JAR。
2. 拖入“JAR作业”节点，在右侧属性中上传 JAR。
3. 在 JAR 作业中选择 `OfficialSiteCleanTransform`，按
   `MongoSource(fs_spider_index) → 官方站点清洗 → 数据库写入` 连线执行。
4. 在通用“数据库写入”节点中选择 `fs_site` 所在的数据源，配置：
   `table=fs_site_entry`、`mode=upsert`、`keyFields=domain,path`、
   `updateExclude=created_time`。

- `OfficialSiteCleanTransform`：识别科技、工信、发改、人社等政府官方渠道，
  生成域名、子目录、类别和官方标识，并按一级目录聚合入口。
- 清洗 Transform 直接输出 `fs_site_entry` 的目标字段；通用 JDBC Sink
  按 `domain + path` 执行 upsert。Spider 同步不经过 Spark，
  而是在“网站资产”页面或 `/siteBackend/sync/spider` 接口中由 `site.backend`
  通过 `SpiderRpc` 直接完成；可选择同步后立即启动采集作业。

“写入网站入口”已完全移除，站点清洗入库统一使用通用“数据库写入”节点。
JAR 详情会展示原始文件名、JAR 标识、
SHA-256、上传时间、大小和作业节点数量，支持按内容区分重复上传的版本并删除历史 JAR。

`site.backend` 建表、菜单、权限和运行配置见 `docs/fs_project_site_backend.sql`。

集群配置：

- `POST /bi/cluster/{info,list,save,delete,config,options,test}`

作业监控：

- `POST /bi/job/{info,list,delete,config,run,cancel,restart}`

计算引擎（`web:flink` / `web:spark`）：

- `POST /bi/dag`：执行或提交作业，支持 `cluster.mode=remote` 与 `debug`
- `POST /bi/cluster/test`：集群连通性测试
- `POST /bi/job/status`、`POST /bi/job/cancel`

## 监控与预警

- `JobMonitorService` 周期性轮询运行中作业的远程状态并回写 `fs_bi_job`；
- 作业失败时通过 `AlarmService` 记录预警，并可选推送到 `fs.bi.monitor.webhook`；
- 流程启用 `auto_recover` 且未超过 `max_retry` 时，自动重新提交流程（触发方式记为 `recover`）。

## 定时调度

- `DiagramScheduleService` 周期性扫描启用调度的流程，按 `expression`（Spring Cron）触发提交；
- 同一流程存在运行中作业时跳过本次调度，避免重叠执行。

## 可配置项

```yaml
fs:
  bi:
    monitor:
      enabled: true
      interval: 60000        # 监控轮询间隔（毫秒）
      initialDelay: 30000
      webhook: ""            # 预警推送地址，留空仅记录日志
    schedule:
      enabled: true
      interval: 30000        # 调度扫描间隔（毫秒）
      initialDelay: 20000
```

## 权限与菜单

新增页面需要 RBAC 侧登记资源与菜单（命名遵循 `module:controller:action`）：

- 编排流程：`bi:diagram:*`
- 集群配置：`bi:cluster:*`
- 作业监控：`bi:job:*`

## 参考
- [Quick BI](https://space.bilibili.com/489439272/channel/detail?cid=152024)
- [DataV](https://www.bilibili.com/video/BV1Xi4y1s7R6)
- [TableAU](https://www.bilibili.com/video/BV1E4411B7ef)
``
