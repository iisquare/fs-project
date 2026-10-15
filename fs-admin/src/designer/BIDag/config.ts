import DesignUtil from '@/utils/DesignUtil'

/**
 * 流批一体计算编排设计器配置。
 *
 * 节点数据格式与后端 DAGService 保持一致：
 * data = { index, name, icon, type, description, parallelism, kvConfigPrefix, options: {...} }
 */

const property = () => import('./Property.vue')

/** 节点参数默认值构造器 */
const payload = (options: any = {}) => () => ({ parallelism: 1, kvConfigPrefix: '', options })

/**
 * support 描述节点支持的计算引擎与处理模式组合，如 { spark: ['batch'], flink: ['batch', 'stream'] }。
 *
 * 约定（与后端节点注册表保持一致）：
 * - spark：batch 与 stream 是两套实现，按实际存在的实现标注；
 * - flink：流批共用 DataStream API，数据源/转换按所属模式标注，输出节点（sink）两种模式均可用；
 * - 未列出的引擎或模式即不展示该节点。
 */
const widget = (type: string, label: string, title: string, icon: string, support: Record<string, string[]>, options: any = {}) => ({
  type, label, title, icon, shape: 'flow-node', support, options: payload(options), property,
})

/** 节点参数编辑字段，Property.vue 依据节点类型渲染 */
export const fields: any = {
  JDBCSource: [
    { key: 'url', label: '连接地址', placeholder: 'jdbc:mysql://host:3306/db' },
    { key: 'driver', label: '驱动', component: 'select', options: ['MySQL', 'PostgreSQL', 'Oracle', 'SQLServer', 'ClickHouse', 'H2', 'SQLite'] },
    { key: 'username', label: '用户名' },
    { key: 'password', label: '密码' },
    { key: 'sql', label: '查询语句', component: 'textarea' },
    { key: 'fetchSize', label: '抓取条数', component: 'number' },
    { key: 'partitionColumn', label: '分区字段' },
    { key: 'lowerBound', label: '下界' },
    { key: 'upperBound', label: '上界' },
    { key: 'numPartitions', label: '分区数', component: 'number' },
    { key: 'iterable', label: '多配置遍历', component: 'switch' },
  ],
  MongoSource: [
    { key: 'hosts', label: '主机', placeholder: 'host1:27017,host2:27017' },
    { key: 'username', label: '用户名' },
    { key: 'password', label: '密码' },
    { key: 'database', label: '数据库' },
    { key: 'collection', label: '集合' },
    { key: 'pipeline', label: '聚合管道', component: 'json' },
  ],
  KafkaSource: [
    { key: 'servers', label: '服务地址', placeholder: 'host:9092' },
    { key: 'topic', label: '主题' },
    { key: 'groupId', label: '消费组' },
    { key: 'offset', label: '起始位置', component: 'select', options: ['latest', 'earliest'] },
    { key: 'checkpoint', label: '检查点', placeholder: '如 file:///tmp/checkpoint' },
  ],
  MySQLCapture: [
    { key: 'hostname', label: '主机' },
    { key: 'port', label: '端口', component: 'number' },
    { key: 'username', label: '用户名' },
    { key: 'password', label: '密码' },
    { key: 'database', label: '数据库' },
    { key: 'table', label: '表' },
    { key: 'serverId', label: 'ServerId' },
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
  ScriptTransform: [
    { key: 'jarURI', label: '依赖Jar', placeholder: 'http://host/path/to/plugin.jar' },
    { key: 'pkgClass', label: '实现类', placeholder: 'com.example.MyTransform' },
  ],
  ConsoleSink: [
    { key: 'echoConfig', label: '输出配置', component: 'switch' },
  ],
  MongoSink: [
    { key: 'hosts', label: '主机' },
    { key: 'username', label: '用户名' },
    { key: 'password', label: '密码' },
    { key: 'database', label: '数据库' },
    { key: 'collection', label: '集合' },
  ],
  ElasticsearchSink: [
    { key: 'nodes', label: '节点地址' },
    { key: 'index', label: '索引' },
    { key: 'idField', label: '主键字段' },
  ],
  KafkaSink: [
    { key: 'servers', label: '服务地址' },
    { key: 'topic', label: '主题' },
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
  name: '数据源',
  children: [
    widget('JDBCSource', '数据库', '关系型数据库读取', 'basic.database', { spark: ['batch'], flink: ['batch'] }, {
      url: '', driver: 'MySQL', username: '', password: '', sql: '', fetchSize: 0,
      partitionColumn: '', lowerBound: '', upperBound: '', numPartitions: 0, iterable: false,
    }),
    widget('MongoSource', 'MongoDB', 'MongoDB读取', 'basic.database', { spark: ['batch'] }, {
      hosts: '', username: '', password: '', database: '', collection: '', pipeline: '',
    }),
    widget('KafkaSource', 'Kafka', 'Kafka消息读取', 'basic.rabbitmq', { spark: ['stream'], flink: ['stream'] }, {
      servers: '', topic: '', groupId: '', offset: 'latest', checkpoint: '',
    }),
    widget('MySQLCapture', 'MySQL CDC', 'MySQL变更捕获', 'basic.database', { flink: ['stream'] }, {
      hostname: '', port: 3306, username: '', password: '', database: '', table: '', serverId: '',
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
    widget('ScriptTransform', '脚本处理', '加载外部插件处理', 'flow.script', { spark: ['batch', 'stream'], flink: ['batch', 'stream'] }, {
      jarURI: '', pkgClass: '',
    }),
  ],
}, {
  name: '数据输出',
  children: [
    widget('ConsoleSink', '控制台', '控制台输出', 'flow.sink', { spark: ['batch', 'stream'], flink: ['batch', 'stream'] }, {
      echoConfig: false,
    }),
    widget('MongoSink', 'MongoDB', 'MongoDB写入', 'basic.database', { spark: ['batch'] }, {
      hosts: '', username: '', password: '', database: '', collection: '',
    }),
    widget('ElasticsearchSink', 'Elasticsearch', 'Elasticsearch写入', 'basic.elasticsearch', { spark: ['batch'], flink: ['batch', 'stream'] }, {
      nodes: '', index: '', idField: '',
    }),
    widget('KafkaSink', 'Kafka', 'Kafka消息写入', 'basic.rabbitmq', { spark: ['stream'], flink: ['batch', 'stream'] }, {
      servers: '', topic: '',
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
