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

### 仪表盘，基于报表
- 通过可视化界面对多个数据报表进行排版布局。
- 支持背景图、大屏组件、组态、3D交互。
- 支持钻取、联动、跳转，除日期、地理位置外允许用户自定义层次结构。
- 提供页面分享，通过弹性布局适配不同浏览器尺寸。

### 故事，基于报表
- 在线制作PPT，方便动态展示数据报表。

## 接口

| 接口 | 说明 | 权限 |
| --- | --- | --- |
| POST `/dataQueryLog/list` | 查询日志列表，支持类型、状态、查询对象、查询用户与查询时间过滤 | `bi:dataQueryLog:` |
| POST `/dataQueryLog/info` | 查询日志详情 | `bi:dataQueryLog:` |
| POST `/dataQueryLog/delete` | 删除查询日志 | `bi:dataQueryLog:delete` |
| POST `/dataQueryLog/config` | 查询类型、状态与排序字典 | `bi:dataQueryLog:` |

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
- `POST /bi/dag/debug`：指定节点调试（`{id, nodeId}`），返回各节点输出

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
