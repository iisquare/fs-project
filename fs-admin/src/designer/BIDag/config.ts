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

/** 节点参数编辑字段，Property.vue 依据节点类型渲染 */
export const fields: any = {
  JDBCSource: [
    {
      key: 'datasourceId', label: '数据源', component: 'datasource', datasourceTypes: ['mysql', 'doris', 'postgresql'],
      placeholder: '输入名称检索数据源', clearOptions: ['url', 'driver', 'username', 'password'],
    },
    { key: 'sql', label: '查询语句', component: 'textarea' },
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
    { key: 'collection', label: '集合' },
    { key: 'pipeline', label: '聚合管道', component: 'json' },
  ],
  ElasticsearchSource: [
    {
      key: 'datasourceId', label: '数据源', component: 'datasource', datasourceTypes: ['elasticsearch'],
      placeholder: '输入名称检索数据源', clearOptions: ['nodes', 'servers', 'username', 'password'],
    },
    { key: 'collection', label: '索引', placeholder: 'index 或 index-*' },
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
      clearOptions: ['servers', 'bootstrap', 'securityProtocol', 'saslMechanism', 'saslJaasConfig'],
    },
    { key: 'servers', label: '服务地址', placeholder: 'host1:9092,host2:9092；未选数据源时填写' },
    { key: 'topic', label: '主题' },
    { key: 'groupId', label: '消费组' },
    { key: 'offset', label: '起始位置(简)', component: 'select', options: ['latest', 'earliest'] },
    { key: 'startingOffsets', label: '起始位置(高级)', placeholder: 'earliest / latest / {"topic":{"0":100}}' },
    { key: 'endingOffsets', label: '批量结束位置', placeholder: 'latest / {"topic":{"0":200}}' },
    { key: 'securityProtocol', label: '安全协议', placeholder: 'PLAINTEXT / SASL_PLAINTEXT / SASL_SSL' },
    { key: 'saslMechanism', label: 'SASL机制', placeholder: 'PLAIN / SCRAM-SHA-256' },
    { key: 'saslJaasConfig', label: 'SASL认证配置', placeholder: 'org.apache.kafka.common.security.plain.PlainLoginModule required username="..." password="...";' },
    { key: 'consumerProperties', label: '消费者参数', component: 'json', placeholder: '{"auto.offset.reset":"latest"}' },
  ],
  MySQLCapture: [
    {
      key: 'datasourceId', label: '数据源', component: 'datasource', datasourceTypes: ['mysql'],
      placeholder: '输入名称检索数据源', clearOptions: ['hostname', 'port', 'username', 'password', 'database', 'databaseList'],
    },
    { key: 'databaseList', label: '数据库/正则', placeholder: '多个值用换行分隔；留空使用数据源数据库' },
    { key: 'tableList', label: '表/正则', placeholder: 'db\\.table 或 .*' },
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
    { key: 'tableList', label: '表/正则', placeholder: 'schema.table 或 .*' },
    { key: 'startup', label: '启动位置', component: 'select', options: ['latest', 'initial', 'earliest'] },
    { key: 'slotName', label: '复制槽', placeholder: '留空按节点ID自动生成' },
    { key: 'decodingPluginName', label: '解码插件', component: 'select', options: ['pgoutput', 'decoderbufs'] },
    { key: 'includeSchema', label: '输出Schema', component: 'switch' },
    { key: 'debeziumProperties', label: 'Debezium参数', component: 'json', placeholder: '{"snapshot.mode":"initial"}' },
  ],
  MongoCapture: [
    {
      key: 'datasourceId', label: '数据源', component: 'datasource', datasourceTypes: ['mongodb'],
      placeholder: '输入名称检索数据源', clearOptions: ['hosts', 'username', 'password', 'databaseList'],
    },
    { key: 'hosts', label: '连接地址', placeholder: 'mongos1:27017,mongos2:27017；未选数据源时填写' },
    { key: 'username', label: '用户名' },
    { key: 'password', label: '密码' },
    { key: 'databaseList', label: '数据库/正则', placeholder: '多个值用换行分隔；留空使用数据源数据库' },
    { key: 'collectionList', label: '集合/正则', placeholder: '.*' },
    { key: 'startup', label: '启动位置', component: 'select', options: ['latest', 'initial', 'earliest', 'committed'] },
    { key: 'copyExisting', label: '复制存量数据', component: 'switch' },
    { key: 'scanFullChangelog', label: '扫描完整变更日志', component: 'switch' },
    { key: 'includeSchema', label: '输出Schema', component: 'switch' },
    { key: 'connectionOptions', label: '连接参数', placeholder: 'replicaSet=rs0&authSource=admin' },
    { key: 'batchSize', label: '批大小', component: 'number' },
    { key: 'pollAwaitTimeMillis', label: '轮询等待(ms)', component: 'number' },
    { key: 'pollMaxBatchSize', label: '轮询最大批大小', component: 'number' },
    { key: 'heartbeatIntervalMillis', label: '心跳间隔(ms)', component: 'number' },
  ],
  SQLTransform: [
    { key: 'sql', label: 'SQL语句', component: 'sql' },
  ],
  ConvertTransform: [
    { key: 'mode', label: '处理方式', component: 'select', options: ['KEEP_SOURCE', 'REMOVE_SOURCE', 'ONLY_TARGET', 'REMOVE_TARGET'] },
    { key: 'items', label: '字段映射', component: 'json', placeholder: '[{"target":"字段","source":"来源","clsType":"java.lang.String"}]' },
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
    { key: 'table', label: '目标表', placeholder: 'schema.table' },
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
    { key: 'collection', label: '集合' },
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
    { key: 'collection', label: '索引' },
    { key: 'idField', label: '主键字段' },
    { key: 'mode', label: '写入模式', component: 'select', options: ['index', 'create', 'update', 'upsert'] },
    { key: 'batchSize', label: '批大小', component: 'number' },
  ],
  KafkaSink: [
    {
      key: 'datasourceId', label: '数据源', component: 'datasource', datasourceTypes: ['kafka'],
      placeholder: '输入名称检索Kafka数据源',
      clearOptions: ['servers', 'bootstrap', 'securityProtocol', 'saslMechanism', 'saslJaasConfig'],
    },
    { key: 'servers', label: '服务地址', placeholder: 'host1:9092,host2:9092；未选数据源时填写' },
    { key: 'topic', label: '主题' },
    { key: 'checkpoint', label: 'Spark流检查点', placeholder: '留空自动按节点名生成' },
    { key: 'topicKey', label: '动态主题字段', placeholder: '消息JSON中的字段名（Spark/Flink）' },
    { key: 'partitionKey', label: '动态分区字段', placeholder: '消息JSON中的字段名（Flink）' },
    { key: 'messageField', label: '消息字段', placeholder: '默认读取 message' },
    { key: 'semantic', label: 'Flink投递语义', component: 'select', options: ['NONE', 'AT_LEAST_ONCE', 'EXACTLY_ONCE'] },
    { key: 'securityProtocol', label: '安全协议' },
    { key: 'saslMechanism', label: 'SASL机制' },
    { key: 'saslJaasConfig', label: 'SASL认证配置' },
    { key: 'producerProperties', label: '生产者参数', component: 'json', placeholder: '{"acks":"all"}' },
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
      datasourceId: undefined, collection: '', pipeline: '',
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
      datasourceId: undefined, servers: '', topic: '', groupId: '', offset: 'latest', startingOffsets: '', endingOffsets: 'latest',
      securityProtocol: '', saslMechanism: '', saslJaasConfig: '', consumerProperties: {},
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
      datasourceId: undefined, hosts: '', username: '', password: '', databaseList: '', collectionList: '.*',
      startup: 'latest', copyExisting: true, scanFullChangelog: false, includeSchema: false,
      connectionOptions: '', batchSize: 0, pollAwaitTimeMillis: 1000, pollMaxBatchSize: 0, heartbeatIntervalMillis: 0,
    }),
  ],
}, {
  name: '数据加工',
  children: [
    widget('SQLTransform', 'SQL', 'SQL转换', 'flow.transform', { spark: ['batch'], flink: ['batch'] }, { sql: '' }),
    widget('ConvertTransform', '字段转换', '字段映射与类型转换', 'flow.transform', { spark: ['batch'] }, {
      mode: 'KEEP_SOURCE', items: [],
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
      datasourceId: undefined, collection: '',
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
      datasourceId: undefined, servers: '', topic: '', checkpoint: '', topicKey: '', partitionKey: '',
      messageField: 'message', semantic: 'AT_LEAST_ONCE',
      securityProtocol: '', saslMechanism: '', saslJaasConfig: '', producerProperties: {},
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
