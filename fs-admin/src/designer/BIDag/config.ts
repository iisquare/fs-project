import DesignUtil from '@/utils/DesignUtil'

/**
 * 流批一体计算编排设计器配置。
 *
 * 节点数据格式与后端 DAGService 保持一致：
 * data = { index, name, icon, type, description, parallelism, kvConfigPrefix, options: {...} }
 */

const property = () => import('./Property.vue')
const jarProperty = () => import('./JarNodeProperty.vue')

/** 节点参数默认值构造器 */
const payload = (options: any = {}) => () => ({ parallelism: 1, kvConfigPrefix: '', options })

/**
 * support 描述节点支持的计算引擎与处理模式组合，如 { spark: ['batch'], flink: ['batch', 'stream'] }。
 *
 * 约定（与后端节点注册表保持一致）：
 * - spark：batch 与 stream 是两套实现，按实际存在的实现标注；
 * - flink：流批共用 DataStream API，数据源/转换按所属模式标注，输出节点（sink）两种模式均可用；
 * - 未列出的引擎或模式会在组件面板中置灰，不可拖入画布。
 */
const directionOf = (type: string) => {
  if (type.endsWith('Source') || type.endsWith('Capture')) return 'source'
  if (type.endsWith('Sink')) return 'sink'
  if (type.endsWith('Config')) return 'config'
  return 'transform'
}

const widget = (type: string, label: string, title: string, icon: string, support: Record<string, string[]>, options: any = {}) => ({
  type, label, title, icon, shape: 'flow-node', support, direction: directionOf(type), options: payload(options), property,
})

/** 节点级配置引导，属性面板在参数上方展示 */
export const nodeGuides: any = {
  FilterTransform: {
    summary: '保留满足条件的记录。条件只作用于一个字段，多个条件请串联多个过滤节点，或使用 SQL 节点。',
    points: [
      '字段填写当前数据中的列名；Flink 的 JSON/CDC 数据可使用 after.status、user.name 等路径。',
      '等于、包含等字符串条件可设置是否区分大小写；数值比较请把值类型选为 number。',
      'IN 条件使用英文逗号分隔，例如 active,pending；IS_NULL / IS_NOT_NULL 无需填写比较值。',
    ],
    example: '示例：field = status，operator = EQ，value = active',
  },
  UnionTransform: {
    summary: '把所有直接上游数据按连线顺序合并成一个输出，不需要配置额外参数。',
    points: [
      '上游字段结构必须一致；列位置和类型不一致时，先使用字段映射或 SQL 节点对齐。',
      'Spark 与 Flink 的流处理和批处理均可使用。',
    ],
    example: '示例：两个 Kafka 读取节点同时连接到数据合并节点。',
  },
  ConvertTransform: {
    summary: '每条映射把来源字段写入目标字段，可选择是否保留原有字段。目标字段会排在输出前面。',
    points: [
      '连接上游数据节点和下游 JDBC 写入节点后，点击“读取连线字段”即可读取两侧字段名称与类型；属性面板的映射表会显示 anchor 和连接线。',
      '读取到的上下游字段结构和映射曲线会保存在节点配置中，重新打开编排后可直接恢复。',
      '来源字段留空时默认与目标字段同名；来源字段不存在时输出 null。',
      '类型留空时保持来源字段类型；字符串转数字等转换请明确选择目标类型。',
      'KEEP_SOURCE 保留未冲突的源字段，REMOVE_SOURCE 删除被引用的源字段，ONLY_TARGET 只保留映射结果，REMOVE_TARGET 仅删除同名目标字段。',
    ],
    example: '示例：输出字段 user_id，来源字段 after.id，类型 Long。',
  },
  WindowTransform: {
    summary: 'Flink 处理时间滚动窗口。窗口达到最大条数或窗口时长到期时触发一次，适合攒批后写入 Kafka、Elasticsearch 等下游。',
    points: [
      '分区字段留空时使用全局窗口；填写后会按字段值分组，每个分组独立计数和超时。',
      '最大条数用于低延迟触发，窗口时长是兜底触发时间，两者先到先触发。',
      '输出 JSON 字段：windowStart、windowEnd、windowKey、windowSize、timeMode、count、items。',
    ],
    example: '示例：分区字段 tenant_id，窗口 60 秒，最大 100 条，表示同一租户每满 100 条或每 60 秒输出一批。',
  },
  AggregateTransform: {
    summary: '按分组字段执行聚合表达式。分组字段会自动加入输出，无需在聚合表达式里重复选择。',
    points: [
      '分组字段使用英文逗号分隔；留空表示对全部数据做一次聚合。',
      '聚合表达式使用当前引擎支持的 SQL 语法，例如 count(*) AS total、sum(amount) AS amount_total。',
    ],
    example: '示例：groupBy = category,status，aggregations = count(*) AS total,sum(amount) AS amount_total',
  },
  SortTransform: {
    summary: '按照其中一个字段对批处理结果排序。',
    points: [
      '字段填写当前数据中的列名；需要多字段排序时请使用 SQL 节点。',
      '流处理没有确定的全量终点，因此该节点只在批处理模式开放。',
    ],
  },
  LimitTransform: {
    summary: '截取批处理结果的前 N 行，不保证业务意义上的“最新”或“最大”，需要顺序时请先连接排序节点。',
    points: [
      '返回行数必须大于 0。',
      '该节点只在批处理模式开放。',
    ],
  },
}

/** 节点参数编辑字段，Property.vue 依据节点类型渲染 */
export const fields: any = {
  JDBCSource: [
    {
      key: 'datasourceId', label: '数据源', component: 'datasource', datasourceTypes: ['mysql', 'doris', 'postgresql'],
      placeholder: '输入名称检索数据源', clearOptions: ['url', 'driver', 'username', 'password'],
    },
    {
      key: 'sql', label: '查询语句', component: 'sql', dialect: 'sql',
      placeholder: '点击“获取表名”选择数据表并生成查询，或直接输入 SQL',
    },
    { key: 'fetchSize', label: '抓取条数', component: 'number' },
    { key: 'partitionColumn', label: '分区字段' },
    { key: 'lowerBound', label: '下界' },
    { key: 'upperBound', label: '上界' },
    { key: 'numPartitions', label: '分区数', component: 'number' },
    { key: 'iterable', label: '多配置遍历', component: 'switch' },
  ],
  MongoSource: [
    {
      key: 'datasourceId', label: '数据源', component: 'datasource', datasourceTypes: ['mongodb'],
      placeholder: '输入名称检索数据源', clearOptions: ['hosts', 'username', 'password', 'database'],
    },
    {
      key: 'database', label: '数据库',
      placeholder: '可手动填写；通过“获取集合”选择时自动回填',
    },
    {
      key: 'collection', label: '集合', component: 'resource', resourceLabel: '集合',
      resourceDatabaseField: 'database', resourceDatabaseFilter: true,
    },
    { key: 'pipeline', label: '聚合管道', component: 'json' },
  ],
  ElasticsearchSource: [
    {
      key: 'datasourceId', label: '数据源', component: 'datasource', datasourceTypes: ['elasticsearch'],
      placeholder: '输入名称检索数据源', clearOptions: ['nodes', 'servers', 'username', 'password'],
    },
    { key: 'collection', label: '索引', component: 'resource', resourceLabel: '索引', placeholder: 'index 或 index-*' },
    { key: 'query', label: '查询DSL', component: 'json', placeholder: '{"query":{"match_all":{}}}' },
    { key: 'sourceFields', label: '返回字段', placeholder: 'name,status；为空返回全部字段' },
    { key: 'limit', label: '读取条数', component: 'number', placeholder: '0 表示不限制' },
  ],
  Neo4jSource: [
    {
      key: 'datasourceId', label: '数据源', component: 'datasource', datasourceTypes: ['neo4j'],
      placeholder: '输入名称检索数据源', clearOptions: ['uri', 'username', 'password', 'database'],
    },
    { key: 'cypher', label: 'Cypher查询', component: 'textarea', placeholder: 'MATCH (n:Person) RETURN n' },
    { key: 'params', label: '查询参数', component: 'json', placeholder: '{"name":"张三"}' },
    { key: 'limit', label: '读取条数', component: 'number', placeholder: '0 表示不限制' },
  ],
  KafkaSource: [
    {
      key: 'datasourceId', label: '数据源', component: 'datasource', datasourceTypes: ['kafka'],
      placeholder: '输入名称检索Kafka数据源',
      clearOptions: ['servers', 'bootstrap', 'securityProtocol', 'saslMechanism', 'saslJaasConfig', 'consumerProperties'],
    },
    { key: 'topic', label: '主题', component: 'resource', resourceLabel: '主题' },
    { key: 'groupId', label: '消费组' },
    { key: 'offset', label: '起始位置(简)', component: 'select', options: ['latest', 'earliest'] },
    { key: 'startingOffsets', label: '起始位置(高级)', placeholder: 'earliest / latest / {"topic":{"0":100}}' },
    { key: 'endingOffsets', label: '批量结束位置', placeholder: 'latest / {"topic":{"0":200}}' },
  ],
  MySQLCapture: [
    {
      key: 'datasourceId', label: '数据源', component: 'datasource', datasourceTypes: ['mysql'],
      placeholder: '输入名称检索数据源', clearOptions: ['hostname', 'port', 'username', 'password', 'database', 'databaseList'],
    },
    { key: 'databaseList', label: '数据库/正则', placeholder: '多个值用换行分隔；留空使用数据源数据库' },
    {
      key: 'tableList', label: '表/正则', component: 'resource', resourceLabel: '表',
      resourceDatabaseField: 'databaseList',
      placeholder: 'db\\.table 或 .*',
    },
    { key: 'startup', label: '启动位置', placeholder: 'latest / initial / earliest / specific:文件,位置 / timestamp:毫秒' },
    { key: 'serverId', label: 'Server ID', placeholder: '多个CDC任务需使用不同ID' },
    { key: 'timeZone', label: '服务端时区', placeholder: 'Asia/Shanghai' },
    { key: 'includeSchema', label: '输出Schema', component: 'switch' },
    { key: 'includeSchemaChanges', label: '输出结构变更', component: 'switch' },
    { key: 'scanNewlyAddedTable', label: '扫描新增表', component: 'switch' },
    { key: 'poolSize', label: '连接池大小', component: 'number' },
    { key: 'fetchSize', label: '抓取条数', component: 'number' },
    { key: 'splitSize', label: '分片大小', component: 'number' },
    { key: 'connectTimeout', label: '连接超时(ms)', component: 'number' },
    { key: 'jdbcProperties', label: 'JDBC参数', component: 'json', placeholder: '{"useSSL":"false"}' },
    { key: 'debeziumProperties', label: 'Debezium参数', component: 'json', placeholder: '{"decimal.handling.mode":"string"}' },
  ],
  PostgreSQLCapture: [
    {
      key: 'datasourceId', label: '数据源', component: 'datasource', datasourceTypes: ['postgresql'],
      placeholder: '输入名称检索数据源', clearOptions: ['hostname', 'port', 'username', 'password', 'database'],
    },
    { key: 'database', label: '数据库', placeholder: '未选数据源时填写' },
    { key: 'schemaList', label: 'Schema/正则', placeholder: 'public；多个值用换行分隔' },
    {
      key: 'tableList', label: '表/正则', component: 'resource', resourceLabel: '表',
      resourceDatabaseField: 'database', resourceDatabaseFilter: true,
      placeholder: 'schema.table 或 .*',
    },
    { key: 'startup', label: '启动位置', component: 'select', options: ['latest', 'initial', 'earliest'] },
    { key: 'slotName', label: '复制槽', placeholder: '留空按节点ID自动生成' },
    { key: 'decodingPluginName', label: '解码插件', component: 'select', options: ['pgoutput', 'decoderbufs'] },
    { key: 'includeSchema', label: '输出Schema', component: 'switch' },
    { key: 'debeziumProperties', label: 'Debezium参数', component: 'json', placeholder: '{"snapshot.mode":"initial"}' },
  ],
  MongoCapture: [
    {
      key: 'datasourceId', label: '数据源', component: 'datasource', datasourceTypes: ['mongodb'],
      placeholder: '输入名称检索数据源', clearOptions: ['hosts', 'username', 'password', 'databaseList', 'connectionOptions'],
    },
    { key: 'databaseList', label: '数据库/正则', placeholder: '多个值用换行分隔；留空使用数据源数据库' },
    {
      key: 'collectionList', label: '集合/正则', component: 'resource', resourceLabel: '集合',
      resourceDatabaseField: 'databaseList',
      placeholder: '.*',
    },
    { key: 'startup', label: '启动位置', component: 'select', options: ['latest', 'initial', 'earliest', 'committed'] },
    { key: 'copyExisting', label: '复制存量数据', component: 'switch' },
    { key: 'scanFullChangelog', label: '扫描完整变更日志', component: 'switch' },
    { key: 'includeSchema', label: '输出Schema', component: 'switch' },
    { key: 'batchSize', label: '批大小', component: 'number' },
    { key: 'pollAwaitTimeMillis', label: '轮询等待(ms)', component: 'number' },
    { key: 'pollMaxBatchSize', label: '轮询最大批大小', component: 'number' },
    { key: 'heartbeatIntervalMillis', label: '心跳间隔(ms)', component: 'number' },
  ],
  SQLTransform: [
    {
      key: 'sql', label: 'SQL语句', component: 'sql', dialect: 'spark',
      placeholder: 'SELECT * FROM `上游节点名称` WHERE ...',
    },
  ],
  FilterTransform: [
    {
      key: 'field', label: '过滤字段', placeholder: '如 status；Flink流式CDC可使用 after.status',
      help: '填写当前数据中的列名或 JSON 点路径。',
    },
    {
      key: 'operator', label: '条件', component: 'select',
      options: [
        { value: 'EQ', label: '等于 (=)' },
        { value: 'NE', label: '不等于 (≠)' },
        { value: 'GT', label: '大于 (>)' },
        { value: 'GE', label: '大于等于 (≥)' },
        { value: 'LT', label: '小于 (<)' },
        { value: 'LE', label: '小于等于 (≤)' },
        { value: 'IN', label: '属于列表 (IN)' },
        { value: 'CONTAINS', label: '包含' },
        { value: 'STARTS_WITH', label: '以指定文本开头' },
        { value: 'ENDS_WITH', label: '以指定文本结尾' },
        { value: 'REGEX', label: '正则匹配' },
        { value: 'IS_NULL', label: '为空' },
        { value: 'IS_NOT_NULL', label: '不为空' },
      ],
      help: 'IN 使用英文逗号分隔多个值；为空类条件不需要比较值。',
    },
    {
      key: 'value', label: '比较值', placeholder: '如 active、100 或 active,pending',
      help: 'IS_NULL / IS_NOT_NULL 可以留空；其他条件需要填写期望值。',
    },
    {
      key: 'valueType', label: '值类型', component: 'select',
      options: [
        { value: 'string', label: '字符串' },
        { value: 'number', label: '数值' },
        { value: 'boolean', label: '布尔值' },
      ],
      help: '决定比较值的解析方式；数值比较请选择“数值”。',
    },
    {
      key: 'caseSensitive', label: '区分大小写', component: 'switch',
      help: '仅对字符串条件和正则表达式生效。',
    },
  ],
  AggregateTransform: [
    {
      key: 'groupBy', label: '分组字段', placeholder: '如 category,status；留空表示不分组',
      help: '使用英文逗号分隔字段名，分组字段会自动出现在输出中。',
    },
    {
      key: 'aggregations', label: '聚合表达式', component: 'textarea',
      placeholder: '如 count(*) AS total,sum(amount) AS amount_total',
      help: '填写当前 Spark/Flink SQL 引擎支持的聚合表达式，多个表达式用英文逗号分隔。',
    },
  ],
  SortTransform: [
    {
      key: 'field', label: '排序字段', placeholder: '如 created_at',
      help: '仅支持一个顶层字段，多字段排序请使用 SQL 节点。',
    },
    {
      key: 'direction', label: '排序方向', component: 'select',
      options: [{ value: 'ASC', label: '升序（小到大）' }, { value: 'DESC', label: '降序（大到小）' }],
    },
  ],
  LimitTransform: [
    {
      key: 'limit', label: '返回行数', component: 'number', placeholder: '必须大于0',
      help: '只截取前 N 行；需要明确“最新 N 行”时，请先连接排序节点。',
    },
  ],
  WindowTransform: [
    {
      key: 'keyField', label: '分区字段', placeholder: '留空使用全局窗口；如 tenant_id',
      help: '填写后按字段值分组，每个分组独立计数和超时；JSON 数据支持点路径。',
    },
    {
      key: 'windowSeconds', label: '窗口时长(秒)', component: 'number', placeholder: '如 60',
      help: '窗口到期时兜底触发，必须大于 0。',
    },
    {
      key: 'maxCount', label: '最大条数', component: 'number', placeholder: '如 100',
      help: '达到条数时提前触发，必须大于 0；与窗口时长先到先触发。',
    },
  ],
  ConvertTransform: [
    {
      key: 'mode', label: '源字段处理', component: 'select',
      options: [
        { value: 'KEEP_SOURCE', label: '保留源字段（目标字段覆盖）' },
        { value: 'REMOVE_SOURCE', label: '删除被引用源字段' },
        { value: 'ONLY_TARGET', label: '仅保留目标字段' },
        { value: 'REMOVE_TARGET', label: '仅删除同名源字段' },
      ],
      help: '默认保留未与目标字段冲突的源字段。',
    },
    {
      key: 'items', label: '字段映射', component: 'mapping',
      help: '按行添加目标字段、来源字段和目标类型；来源字段留空时默认与目标同名。',
    },
  ],
  JSONStringifyTransform: [
    { key: 'fields', label: '字段列表', component: 'json' },
  ],
  ConsoleSink: [
    { key: 'echoConfig', label: '输出配置', component: 'switch' },
  ],
  JDBCSink: [
    {
      key: 'datasourceId', label: '数据源', component: 'datasource', datasourceTypes: ['mysql', 'doris', 'postgresql'],
      placeholder: '输入名称检索数据源', clearOptions: ['url', 'driver', 'username', 'password'],
    },
    { key: 'table', label: '目标表', component: 'resource', resourceLabel: '表名', placeholder: 'schema.table' },
    { key: 'mode', label: '写入模式', component: 'select', options: ['append', 'overwrite', 'upsert'] },
    { key: 'keyFields', label: '主键字段', placeholder: 'domain,path' },
    { key: 'updateExclude', label: '不更新字段', placeholder: 'created_time' },
    { key: 'batchSize', label: '批大小', component: 'number' },
  ],
  MongoSink: [
    {
      key: 'datasourceId', label: '数据源', component: 'datasource', datasourceTypes: ['mongodb'],
      placeholder: '输入名称检索数据源', clearOptions: ['hosts', 'username', 'password', 'database'],
    },
    {
      key: 'database', label: '数据库',
      placeholder: '可手动填写；通过“获取集合”选择时自动回填',
    },
    {
      key: 'collection', label: '集合', component: 'resource', resourceLabel: '集合',
      resourceDatabaseField: 'database', resourceDatabaseFilter: true,
    },
  ],
  Neo4jSink: [
    {
      key: 'datasourceId', label: '数据源', component: 'datasource', datasourceTypes: ['neo4j'],
      placeholder: '输入名称检索数据源', clearOptions: ['uri', 'username', 'password', 'database'],
    },
    {
      key: 'cypher', label: '写入Cypher', component: 'textarea',
      placeholder: 'UNWIND $rows AS row\nMERGE (n:Person {id: row.id})\nSET n += row',
    },
    { key: 'batchSize', label: '批大小', component: 'number' },
  ],
  OntologySource: [
    {
      key: 'datasourceId', label: 'Neo4j数据源', component: 'datasource', datasourceTypes: ['neo4j'],
      placeholder: '输入名称检索数据源', clearOptions: ['uri', 'username', 'password', 'database'],
    },
    { key: 'ontologyId', label: '知识图谱', component: 'ontology', placeholder: '输入名称检索本体' },
    { key: 'entityLabel', label: '本体实体', component: 'ontologyEntity', placeholder: '选择本体内实体' },
    { key: 'filters', label: '属性过滤', component: 'json', placeholder: '{"status":"active"}' },
    { key: 'orderField', label: '排序字段', placeholder: '本体内定义的属性名' },
    { key: 'orderDirection', label: '排序方向', component: 'select', options: ['ASC', 'DESC'] },
    { key: 'limit', label: '读取条数', component: 'number', placeholder: '0 表示不限制' },
  ],
  OntologySink: [
    {
      key: 'datasourceId', label: 'Neo4j数据源', component: 'datasource', datasourceTypes: ['neo4j'],
      placeholder: '输入名称检索数据源', clearOptions: ['uri', 'username', 'password', 'database'],
    },
    { key: 'ontologyId', label: '知识图谱', component: 'ontology', placeholder: '输入名称检索本体' },
    { key: 'entityLabel', label: '本体实体', component: 'ontologyEntity', placeholder: '选择本体内实体' },
    { key: 'batchSize', label: '批大小', component: 'number' },
  ],
  ElasticsearchSink: [
    {
      key: 'datasourceId', label: '数据源', component: 'datasource', datasourceTypes: ['elasticsearch'],
      placeholder: '输入名称检索数据源', clearOptions: ['nodes', 'servers', 'username', 'password'],
    },
    { key: 'collection', label: '索引', component: 'resource', resourceLabel: '索引' },
    { key: 'idField', label: '主键字段' },
    { key: 'mode', label: '写入模式', component: 'select', options: ['index', 'create', 'update', 'upsert'] },
    { key: 'batchSize', label: '批大小', component: 'number' },
  ],
  KafkaSink: [
    {
      key: 'datasourceId', label: '数据源', component: 'datasource', datasourceTypes: ['kafka'],
      placeholder: '输入名称检索Kafka数据源',
      clearOptions: ['servers', 'bootstrap', 'securityProtocol', 'saslMechanism', 'saslJaasConfig', 'producerProperties'],
    },
    { key: 'topic', label: '主题', component: 'resource', resourceLabel: '主题' },
    { key: 'checkpoint', label: 'Spark流检查点', placeholder: '留空自动按节点名生成' },
    { key: 'topicKey', label: '动态主题字段', placeholder: '消息JSON中的字段名（Spark/Flink）' },
    { key: 'partitionKey', label: '动态分区字段', placeholder: '消息JSON中的字段名（Flink）' },
    { key: 'messageField', label: '消息字段', placeholder: '默认读取 message' },
    { key: 'semantic', label: 'Flink投递语义', component: 'select', options: ['NONE', 'AT_LEAST_ONCE', 'EXACTLY_ONCE'] },
  ],
  APIConfig: [
    { key: 'url', label: '接口地址' },
    { key: 'checkField', label: '校验字段', placeholder: 'code' },
    { key: 'checkValue', label: '校验取值', placeholder: '0' },
    { key: 'dataField', label: '数据字段', placeholder: 'data' },
  ],
  DateGenerateConfig: [
    { key: 'arg', label: '参数名称' },
    { key: 'datetime', label: '日期时间', placeholder: '为空时取当前时间戳' },
    { key: 'pattern', label: '格式', component: 'select', options: ['yyyy-MM-dd HH:mm:ss', 'yyyy-MM-dd', 'yyyyMMdd'] },
    { key: 'timezone', label: '时区', component: 'timezone' },
    { key: 'locale', label: '区域', component: 'locale' },
  ],
  JSONConfig: [
    { key: 'json', label: 'JSON内容', component: 'json', placeholder: '{}' },
  ],
  MergeConfig: [
    { key: 'mergeType', label: '合并方式', component: 'select', options: ['map', 'array'] },
    { key: 'arg', label: '参数名称' },
    { key: 'echoPrefix', label: '输出前缀' },
  ],
  NumberGenerateConfig: [
    { key: 'start', label: '起始值', component: 'number' },
    { key: 'step', label: '步长', component: 'number' },
    { key: 'end', label: '结束值', component: 'number' },
    { key: 'divisor', label: '除数', component: 'number' },
    { key: 'inner', label: '内层参数' },
    { key: 'outer', label: '外层参数' },
  ],
  CalendarOffsetConfig: [
    { key: 'arg', label: '参数名称' },
    { key: 'reference', label: '引用参数' },
    { key: 'method', label: '操作', component: 'select', options: ['set', 'add'] },
    { key: 'field', label: '日历字段', component: 'number' },
    { key: 'value', label: '变更值', component: 'number' },
    { key: 'offset', label: '偏移量', component: 'number' },
    { key: 'timezone', label: '时区', component: 'timezone' },
    { key: 'locale', label: '区域', component: 'locale' },
  ],
  DateFormatConfig: [
    { key: 'arg', label: '参数名称' },
    { key: 'reference', label: '引用参数' },
    { key: 'pattern', label: '格式', placeholder: 'yyyy-MM-dd HH:mm:ss' },
    { key: 'timezone', label: '时区', component: 'timezone' },
    { key: 'locale', label: '区域', component: 'locale' },
  ],
}

const widgets = [{
  name: '数据读取',
  children: [
    widget('JDBCSource', '数据库读取', '关系型数据库读取', 'basic.database', { spark: ['batch'], flink: ['batch'] }, {
      datasourceId: undefined, sql: '', fetchSize: 0,
      partitionColumn: '', lowerBound: '', upperBound: '', numPartitions: 0, iterable: false,
    }),
    widget('MongoSource', 'MongoDB读取', 'MongoDB读取', 'basic.mongodb', { spark: ['batch'] }, {
      datasourceId: undefined, database: '', collection: '', pipeline: '',
    }),
    widget('ElasticsearchSource', 'Elasticsearch读取', 'Elasticsearch索引读取', 'basic.elasticsearch', { spark: ['batch'] }, {
      datasourceId: undefined, collection: '', query: '{"query":{"match_all":{}}}', sourceFields: '', limit: 0,
    }),
    widget('Neo4jSource', 'Neo4j读取', '执行Cypher读取图数据', 'basic.neo4j', { spark: ['batch'] }, {
      datasourceId: undefined, cypher: 'MATCH (n) RETURN n', params: {}, limit: 1000,
    }),
    widget('OntologySource', '知识图谱读取', '按本体实体读取Neo4j数据', 'layout.knowledgeGraph', { spark: ['batch'] }, {
      datasourceId: undefined, ontologyId: undefined, entityLabel: '', filters: {}, orderField: '', orderDirection: 'ASC', limit: 1000,
    }),
    widget('KafkaSource', 'Kafka读取', 'Kafka流式或指定区间批量读取', 'basic.kafka', { spark: ['batch', 'stream'], flink: ['batch', 'stream'] }, {
      datasourceId: undefined, topic: '', groupId: '', offset: 'latest', startingOffsets: '', endingOffsets: 'latest',
    }),
    widget('MySQLCapture', 'MySQL CDC', 'MySQL Binlog变更捕获', 'basic.mysql', { flink: ['stream'] }, {
      datasourceId: undefined, databaseList: '', tableList: '.*', startup: 'latest', serverId: '',
      timeZone: 'Asia/Shanghai', includeSchema: false, includeSchemaChanges: false, scanNewlyAddedTable: false,
      poolSize: 20, fetchSize: 1024, splitSize: 0, connectTimeout: 30000,
      jdbcProperties: {}, debeziumProperties: {},
    }),
    widget('PostgreSQLCapture', 'PostgreSQL CDC', 'PostgreSQL逻辑复制变更捕获', 'basic.postgresql', { flink: ['stream'] }, {
      datasourceId: undefined, database: '', schemaList: 'public', tableList: '.*', startup: 'latest',
      slotName: '', decodingPluginName: 'pgoutput', includeSchema: true, debeziumProperties: {},
    }),
    widget('MongoCapture', 'MongoDB CDC', 'MongoDB Change Stream变更捕获', 'basic.mongodb', { flink: ['stream'] }, {
      datasourceId: undefined, databaseList: '', collectionList: '.*',
      startup: 'latest', copyExisting: true, scanFullChangelog: false, includeSchema: false,
      batchSize: 0, pollAwaitTimeMillis: 1000, pollMaxBatchSize: 0, heartbeatIntervalMillis: 0,
    }),
  ],
}, {
  name: '数据加工',
  children: [
    widget('SQLTransform', 'SQL', 'SQL转换', 'flow.transform', { spark: ['batch'], flink: ['batch'] }, { sql: '' }),
    widget('FilterTransform', '条件过滤', '按字段条件过滤数据，支持Flink JSON与CDC数据', 'flow.transform', { spark: ['batch', 'stream'], flink: ['batch', 'stream'] }, {
      field: '', operator: 'EQ', value: '', valueType: 'string', caseSensitive: true,
    }),
    widget('UnionTransform', '数据合并', '按入边顺序合并多个上游数据；要求结构一致', 'flow.transform', { spark: ['batch', 'stream'], flink: ['batch', 'stream'] }),
    widget('ConvertTransform', '字段映射', '映射目标字段并按需转换类型', 'flow.transform', { spark: ['batch'], flink: ['batch', 'stream'] }, {
      mode: 'KEEP_SOURCE', items: [],
    }),
    widget('WindowTransform', '窗口聚合', '按条数或窗口超时触发，输出带窗口边界和明细的JSON批次', 'flow.transform', { flink: ['stream'] }, {
      keyField: '', windowSeconds: 60, maxCount: 100,
    }),
    widget('AggregateTransform', '分组聚合', '按字段分组并计算聚合表达式', 'flow.transform', { spark: ['batch'], flink: ['batch'] }, {
      groupBy: '', aggregations: 'count(*) AS total',
    }),
    widget('SortTransform', '排序', '按指定字段排序', 'flow.transform', { spark: ['batch'], flink: ['batch'] }, {
      field: '', direction: 'ASC',
    }),
    widget('LimitTransform', '行数限制', '限制输出数据的行数', 'flow.transform', { spark: ['batch'], flink: ['batch'] }, {
      limit: 100,
    }),
    widget('JSONStringifyTransform', 'JSON序列化', '生成JSON字符串', 'flow.transform', { spark: ['batch'] }, {
      fields: [],
    }),
    {
      type: 'ScriptTransform',
      label: 'JAR作业',
      title: '上传或选择历史 JAR，并选择其中的作业节点',
      icon: 'flow.script',
      shape: 'flow-node',
      direction: 'transform',
      support: { spark: ['batch', 'stream'], flink: ['batch', 'stream'] },
      property: jarProperty,
      options: payload({ jarId: '', jarURI: '', pkgClass: '' }),
    },
  ],
}, {
  name: '数据写入',
  children: [
    widget('ConsoleSink', '控制台输出', '控制台输出', 'flow.sink', { spark: ['batch', 'stream'], flink: ['batch', 'stream'] }, {
      echoConfig: false,
    }),
    widget('JDBCSink', '数据库写入', '关系型数据库写入，支持主键 upsert', 'basic.database', { spark: ['batch'] }, {
      datasourceId: undefined, table: '', mode: 'append', keyFields: '', updateExclude: '', batchSize: 500,
    }),
    widget('MongoSink', 'MongoDB写入', 'MongoDB写入', 'basic.mongodb', { spark: ['batch'] }, {
      datasourceId: undefined, database: '', collection: '',
    }),
    widget('Neo4jSink', 'Neo4j写入', '通过Cypher UNWIND批量写入图数据', 'basic.neo4j', { spark: ['batch'] }, {
      datasourceId: undefined, cypher: 'UNWIND $rows AS row\nRETURN row', batchSize: 500,
    }),
    widget('OntologySink', '知识图谱写入', '按本体实体写入Neo4j图数据', 'layout.knowledgeGraph', { spark: ['batch'] }, {
      datasourceId: undefined, ontologyId: undefined, entityLabel: '', batchSize: 500,
    }),
    widget('ElasticsearchSink', 'Elasticsearch写入', 'Elasticsearch写入', 'basic.elasticsearch', { spark: ['batch'], flink: ['batch', 'stream'] }, {
      datasourceId: undefined, collection: '', idField: '', mode: 'index', batchSize: 1000,
    }),
    widget('KafkaSink', 'Kafka写入', 'Kafka流式或批量消息写入', 'basic.kafka', { spark: ['batch', 'stream'], flink: ['batch', 'stream'] }, {
      datasourceId: undefined, topic: '', checkpoint: '', topicKey: '', partitionKey: '',
      messageField: 'message', semantic: 'AT_LEAST_ONCE',
    }),
  ],
}, {
  name: '配置参数',
  children: [
    widget('APIConfig', '远端接口', '远端接口参数', 'flow.config', { spark: ['batch', 'stream'], flink: ['batch', 'stream'] }, {
      url: '', checkField: 'code', checkValue: '0', dataField: 'data',
    }),
    widget('DateGenerateConfig', '日期参数', '生成日期参数', 'basic.schedule', { spark: ['batch', 'stream'], flink: ['batch', 'stream'] }, {
      arg: '', datetime: '', pattern: 'yyyy-MM-dd HH:mm:ss', timezone: 'GMT+8', locale: 'zh_CN',
    }),
    widget('JSONConfig', 'JSON参数', 'JSON配置项', 'flow.config', { spark: ['batch', 'stream'], flink: ['batch', 'stream'] }, {
      json: '{}',
    }),
    widget('MergeConfig', '合并参数', '合并上游配置', 'flow.config', { spark: ['batch', 'stream'], flink: ['batch', 'stream'] }, {
      mergeType: 'map', arg: '', echoPrefix: '',
    }),
    widget('NumberGenerateConfig', '数值参数', '生成数值序列', 'flow.config', { spark: ['batch', 'stream'], flink: ['batch', 'stream'] }, {
      start: 0, step: 1, end: 0, divisor: 0, inner: '', outer: '',
    }),
    widget('CalendarOffsetConfig', '日历偏移', '日历字段偏移', 'flow.config', { spark: ['batch', 'stream'], flink: ['batch', 'stream'] }, {
      arg: '', reference: '', method: 'add', field: 0, value: 0, offset: 0, timezone: 'GMT+8', locale: 'zh_CN',
    }),
    widget('DateFormatConfig', '日期格式化', '时间戳格式化', 'flow.config', { spark: ['batch', 'stream'], flink: ['batch', 'stream'] }, {
      arg: '', reference: '', pattern: 'yyyy-MM-dd HH:mm:ss', timezone: 'GMT+8', locale: 'zh_CN',
    }),
  ],
}]

export default {
  canvas: { options: () => ({}), property: () => import('./Canvas.vue') },
  edge: { options: () => ({}), property: () => import('./EdgeProperty.vue') },
  widgets: DesignUtil.widgets(widgets),
  nodeGuides,
  timezones: ['GMT', 'GMT+1', 'GMT+2', 'GMT+3', 'GMT+4', 'GMT+5', 'GMT+6', 'GMT+7', 'GMT+8', 'GMT+9', 'GMT+10', 'GMT+11', 'GMT-1', 'GMT-2', 'GMT-3', 'GMT-4', 'GMT-5', 'GMT-6', 'GMT-7', 'GMT-8', 'GMT-9', 'GMT-10', 'GMT-11'],
  locales: ['zh_CN', 'zh_TW', 'en_US', 'en_GB', 'ja_JP', 'ko_KR'],
  toolbars: [{
    type: 'hand', label: '拖动', icon: 'action.hand', selectable: true, selected: true, callback (toolbar: any, instance: any) { instance.flow.panning() },
  }, {
    type: 'lasso', label: '框选', icon: 'action.lasso', selectable: true, callback (toolbar: any, instance: any) { instance.flow.selecting() },
  }, {
    type: 'fit', label: '适合', icon: 'action.fit', callback (toolbar: any, instance: any) { instance.flow.fitting() },
  }, {
    type: 'divider',
  }, {
    type: 'clean', label: '清空', icon: 'action.clean', callback (toolbar: any, instance: any) {
      instance.flow.fromJSON()
      instance.flow.options.onBlankClick()
    },
  }],
  fields,
}
