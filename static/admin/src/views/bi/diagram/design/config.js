const ENGINE_SPARK = 'spark'
const ENGINE_FLINK = 'flink'
const MODEL_BATCH = 'batch'
const MODEL_STREAM = 'stream'

const config = {
  diagram: { id: 0, name: '', engine: '', model: '' },
  uuid () { return new Date().getTime() + ('' + Math.random()).slice(-6) }
}

const DefaultOptions = () => {
  return {}
}

const CanvasOptions = () => {
  return { top: 0, width: 500, height: 500 }
}

const ImportConfigOptions = () => {
  return { id: '' }
}

const JSONConfigOptions = () => {
  return { json: '{}' }
}

const MergeConfigOptions = () => {
  return { arg: '', mergeType: 'map', echoPrefix: '' }
}

const APIConfigOptions = () => {
  return { url: '', method: 'GET', checkField: 'code', checkValue: '0', dataField: 'data' }
}

const ConsulConfigOptions = () => {
  return { url: '' }
}

const DateGenerateConfigOptions = () => {
  return { arg: '', datetime: '', pattern: 'yyyy-MM-dd HH:mm:ss', timezone: 'GMT+8', locale: 'zh_CN' }
}

const DateFormatConfigOptions = () => {
  return { arg: '', reference: '', pattern: 'yyyy-MM-dd HH:mm:ss', timezone: 'GMT+8', locale: 'zh_CN' }
}

const CalendarOffsetConfigOptions = () => {
  return { arg: '', reference: '', offset: 0, value: 0, method: '', field: '0', timezone: 'GMT+8', locale: 'zh_CN' }
}

const NumberGenerateConfigOptions = () => {
  return { arg: '', start: 0, step: 0, end: 0, divisor: 0 }
}

const JSONParseTransformOptions = () => {
  return { arg: '', reference: '' }
}

const JSONStringifyTransformOptions = () => {
  return { arg: '', reference: '' }
}

const SQLTransformOptions = () => {
  return { sql: '' }
}

const RegularTransformOptions = () => {
  return { arg: '', reference: '', pattern: '' }
}

const DateParseTransformOptions = () => {
  return { arg: '', reference: '', pattern: 'yyyy-MM-dd HH:mm:ss', timezone: 'GMT+8', locale: 'zh_CN' }
}

const DateFormatTransformOptions = () => {
  return { arg: '', reference: '', pattern: 'yyyy-MM-dd HH:mm:ss', timezone: 'GMT+8', locale: 'zh_CN' }
}

const ConvertTransformOptions = () => {
  return { mode: 'KEEP_SOURCE', items: [] }
}

const ScriptTransformOptions = () => {
  return { jarURI: '', pkgClass: '' }
}

const ElasticsearchSourceOptions = () => {
  return { cluster: 'elasticsearch', servers: '127.0.0.1:9200', username: '', password: '', collection: '', query: '{}' }
}

const FileSourceOptions = () => {
  return { filepath: '', charset: 'UTF-8' }
}

const KafkaSourceOptions = () => {
  return { bootstrap: '127.0.0.1:9092', zookeeper: '127.0.0.1:2181/kafka', offset: 'earliest', group: 'fs-bi', topic: '', commitInterval: 1000 }
}

const JDBCSourceOptions = () => {
  return {
    driver: 'com.mysql.jdbc.Driver',
    url: 'jdbc:mysql://127.0.0.1:3306/db_name?characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true',
    username: 'root',
    password: '',
    iterable: false,
    partitionColumn: '',
    lowerBound: '',
    upperBound: '',
    numPartitions: 0,
    fetchSize: 0,
    sql: 'select * from '
  }
}

const ConsoleSinkOptions = () => {
  return { echoConfig: false, mode: '' }
}

const ElasticsearchSinkOptions = () => {
  return { servers: '127.0.0.1:9200', username: '', password: '', collection: '', batchSize: 1, flushInterval: -1, idField: '_id', tableField: '_table' }
}

const MongoSinkOptions = () => {
  return { hosts: '127.0.0.1:27017', database: 'admin', username: '', password: '', collection: '', batchSize: 0, replaceDocument: true, forceInsert: false }
}

export default Object.assign(config, {
  canvas: {
    options: CanvasOptions, property: () => import('./CanvasProperty')
  },
  widgetTransientMap: null,
  widgetByType (type) {
    if (this.widgetTransientMap === null) {
      const map = {}
      this.widgets.forEach(widget => {
        widget.children.forEach(item => {
          map[item.type] = item
        })
      })
      this.widgetTransientMap = map
    }
    return this.widgetTransientMap[type]
  },
  widgetDefaults (type) {
    return this.widgetByType(type).options()
  },
  widgets: [{
    name: '配置参数',
    children: [Object.assign({
      supports: [[ENGINE_SPARK, ENGINE_FLINK, MODEL_BATCH, MODEL_STREAM]]
    }, {
      type: 'ImportConfig', label: 'Import', title: '接入子图规则', icon: 'dagConfig', options: ImportConfigOptions, property: () => import('./ImportConfigProperty')
    }), Object.assign({
      supports: [[ENGINE_SPARK, ENGINE_FLINK, MODEL_BATCH, MODEL_STREAM]]
    }, {
      type: 'ExportConfig', label: 'Export', title: '导出当前规则', icon: 'dagConfig', options: DefaultOptions, property: () => import('./DefaultProperty')
    }), Object.assign({
      supports: [[ENGINE_SPARK, ENGINE_FLINK, MODEL_BATCH, MODEL_STREAM]]
    }, {
      type: 'JSONConfig', label: 'JSON', title: 'JSON参数', icon: 'dagConfig', options: JSONConfigOptions, property: () => import('./JSONConfigProperty')
    }), Object.assign({
      supports: [[ENGINE_SPARK, ENGINE_FLINK, MODEL_BATCH, MODEL_STREAM]]
    }, {
      type: 'MergeConfig', label: 'Merge', title: '合并执行参数', icon: 'dagConfig', options: MergeConfigOptions, property: () => import('./MergeConfigProperty')
    }), Object.assign({
      supports: [[ENGINE_SPARK, ENGINE_FLINK, MODEL_BATCH, MODEL_STREAM]]
    }, {
      type: 'APIConfig', label: 'API', title: '远端接口参数', icon: 'dagConfig', options: APIConfigOptions, property: () => import('./APIConfigProperty')
    }), Object.assign({
      supports: [[ENGINE_SPARK, ENGINE_FLINK, MODEL_BATCH, MODEL_STREAM]]
    }, {
      type: 'ConsulConfig', label: 'Consul', title: 'Consul配置中心参数', icon: 'dagConfig', options: ConsulConfigOptions, property: () => import('./ConsulConfigProperty')
    }), Object.assign({
      supports: [[ENGINE_SPARK, ENGINE_FLINK, MODEL_BATCH, MODEL_STREAM]]
    }, {
      type: 'DateGenerateConfig', label: 'DateGenerate', title: '生成日期参数', icon: 'dagConfig', options: DateGenerateConfigOptions, property: () => import('./DateGenerateConfigProperty')
    }), Object.assign({
      supports: [[ENGINE_SPARK, ENGINE_FLINK, MODEL_BATCH, MODEL_STREAM]]
    }, {
      type: 'DateFormatConfig', label: 'DateFormat', title: '格式化日期参数', icon: 'dagConfig', options: DateFormatConfigOptions, property: () => import('./DateFormatConfigProperty')
    }), Object.assign({
      supports: [[ENGINE_SPARK, ENGINE_FLINK, MODEL_BATCH, MODEL_STREAM]]
    }, {
      type: 'CalendarOffsetConfig', label: 'CalendarOffset', title: '日期偏移参数', icon: 'dagConfig', options: CalendarOffsetConfigOptions, property: () => import('./CalendarOffsetConfigProperty')
    }), Object.assign({
      supports: [[ENGINE_SPARK, ENGINE_FLINK, MODEL_BATCH, MODEL_STREAM]]
    }, {
      type: 'NumberGenerateConfig', label: 'NumberGenerate', title: '生成数值参数', icon: 'dagConfig', options: NumberGenerateConfigOptions, property: () => import('./NumberGenerateConfigProperty')
    })]
  }, {
    name: '数据输入',
    children: [Object.assign({
      supports: [[ENGINE_SPARK, MODEL_BATCH]]
    }, {
      type: 'JDBCSource', label: 'JDBC', title: 'JDBC输入', icon: 'dagSource', options: JDBCSourceOptions, property: () => import('./JDBCSourceProperty')
    }), Object.assign({
      supports: []
    }, {
      type: 'MongoSource', label: 'Mongo', title: 'Mongo输入', icon: 'dagSource', options: DefaultOptions, property: () => import('./DefaultProperty')
    }), Object.assign({
      supports: []
    }, {
      type: 'FileSource', label: 'File', title: '文件输入', icon: 'dagSource', options: FileSourceOptions, property: () => import('./FileSourceProperty')
    }), Object.assign({
      supports: [[ENGINE_FLINK, MODEL_STREAM]]
    }, {
      type: 'KafkaSource', label: 'Kafka', title: 'Kafka输入', icon: 'dagSource', options: KafkaSourceOptions, property: () => import('./KafkaSourceProperty')
    }), Object.assign({
      supports: []
    }, {
      type: 'HBaseSource', label: 'HBase', title: 'HBase输入', icon: 'dagSource', options: DefaultOptions, property: () => import('./DefaultProperty')
    }), Object.assign({
      supports: []
    }, {
      type: 'ElasticsearchSource', label: 'Elasticsearch', title: '搜索引擎输入', icon: 'dagSource', options: ElasticsearchSourceOptions, property: () => import('./ElasticsearchSourceProperty')
    })]
  }, {
    name: '数据处理',
    children: [Object.assign({
      supports: []
    }, {
      type: 'JSONParseTransform', label: 'JSONParse', title: 'JSON格式化', icon: 'dagTransform', options: JSONParseTransformOptions, property: () => import('./JSONParseTransformProperty')
    }), Object.assign({
      supports: []
    }, {
      type: 'JSONStringifyTransform', label: 'JSONStringify', title: 'JSON序列化', icon: 'dagTransform', options: JSONStringifyTransformOptions, property: () => import('./JSONStringifyTransformProperty')
    }), Object.assign({
      supports: []
    }, {
      type: 'UnionTransform', label: 'Union', title: '数据合并', icon: 'dagTransform', options: DefaultOptions, property: () => import('./DefaultProperty')
    }), Object.assign({
      supports: [[ENGINE_SPARK, MODEL_BATCH]]
    }, {
      type: 'SQLTransform', label: 'SQL', title: 'SQL查询', icon: 'dagTransform', options: SQLTransformOptions, property: () => import('./SQLTransformProperty')
    }), Object.assign({
      supports: []
    }, {
      type: 'RegularTransform', label: 'Regular', title: '正则匹配', icon: 'dagTransform', options: RegularTransformOptions, property: () => import('./RegularTransformProperty')
    }), Object.assign({
      supports: [[ENGINE_SPARK, MODEL_BATCH]]
    }, {
      type: 'ConvertTransform', label: 'Convert', title: '字段转换', icon: 'dagTransform', options: ConvertTransformOptions, property: () => import('./ConvertTransformProperty')
    }), Object.assign({
      supports: []
    }, {
      type: 'DateParseTransform', label: 'DateParse', title: '日期解析', icon: 'dagTransform', options: DateParseTransformOptions, property: () => import('./DateParseTransformProperty')
    }), Object.assign({
      supports: []
    }, {
      type: 'DateFormatTransform', label: 'DateFormat', title: '日期格式化', icon: 'dagTransform', options: DateFormatTransformOptions, property: () => import('./DateFormatTransformProperty')
    }), Object.assign({
      supports: [[ENGINE_SPARK, MODEL_BATCH, ENGINE_FLINK, MODEL_STREAM]]
    }, {
      type: 'ScriptTransform', label: 'Script', title: '逻辑脚本', icon: 'dagScript', options: ScriptTransformOptions, property: () => import('./ScriptTransformProperty')
    })]
  }, {
    name: '数据输出',
    children: [Object.assign({
      supports: [[ENGINE_SPARK, MODEL_BATCH, ENGINE_FLINK, MODEL_STREAM]]
    }, {
      type: 'ConsoleSink', label: 'Console', title: 'Console输出', icon: 'dagSink', options: ConsoleSinkOptions, property: () => import('./ConsoleSinkProperty')
    }), Object.assign({
      supports: []
    }, {
      type: 'JDBCSink', label: 'JDBC', title: 'JDBC输出', icon: 'dagSink', options: DefaultOptions, property: () => import('./DefaultProperty')
    }), Object.assign({
      supports: [[ENGINE_SPARK, MODEL_BATCH]]
    }, {
      type: 'MongoSink', label: 'Mongo', title: 'Mongo输出', icon: 'dagSink', options: MongoSinkOptions, property: () => import('./MongoSinkProperty')
    }), Object.assign({
      supports: []
    }, {
      type: 'KafkaSink', label: 'Kafka', title: 'Kafka输出', icon: 'dagSink', options: DefaultOptions, property: () => import('./DefaultProperty')
    }), Object.assign({
      supports: []
    }, {
      type: 'HBaseSink', label: 'HBase', title: 'HBase输出', icon: 'dagSink', options: DefaultOptions, property: () => import('./DefaultProperty')
    }), Object.assign({
      supports: [[ENGINE_FLINK, MODEL_STREAM]]
    }, {
      type: 'ElasticsearchSink', label: 'Elasticsearch', title: 'Elasticsearch输出', icon: 'dagSink', options: ElasticsearchSinkOptions, property: () => import('./ElasticsearchSinkProperty')
    })]
  }]
})
