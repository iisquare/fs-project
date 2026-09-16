import DesignUtil from '@/utils/DesignUtil'
import SwitchLayout from '@/designer/X6/switch'

const config: any = {}

/* ------------------------------- 画布配置 ------------------------------- */

const CanvasOptions = () => {
  return {
    id: null,
    name: '',
    mode: 'workflow',
    icon: '',
    tags: [],
    status: '1',
    sort: 0,
    description: '',
  }
}

const EdgeOptions = () => {
  return { name: '', description: '' }
}

const DefaultOptions = () => {
  return {}
}

/* ------------------------------- 节点配置 ------------------------------- */

const StartOptions = () => {
  return {
    // 固定输入：用户输入与文件列表，默认启用（勾选），不可删除
    query: { enabled: true, maxLength: 256, description: '用户输入内容' },
    fileIds: { enabled: true, maxCount: 3, fileTypes: [], description: '用户上传的文件标识列表' },
    // 自定义参数：类型见 inputTypes（文本、段落、数值等）
    variables: [],
  }
}

/**
 * 合并配置对象，忽略来源中的 undefined/null，避免历史数据缺字段时覆盖默认值
 */
const mergeOptions = (...sources: any[]) => {
  const result: any = {}
  sources.forEach((source: any) => {
    if (!source || 'object' !== typeof source) return
    Object.keys(source).forEach((key: string) => {
      if (undefined === source[key] || null === source[key]) return
      result[key] = source[key]
    })
  })
  return result
}

/**
 * 开始节点的输入清单：固定输入（query 用户输入、fileIds 文件列表）默认启用，
 * 自定义参数来自 variables，调试运行与下游变量引用共用该清单。
 */
const StartInputs = (data: any) => {
  const items: any[] = []
  const query = data?.query ?? {}
  if (false !== query.enabled) {
    items.push({
      name: 'query',
      label: '用户输入',
      type: 'String',
      description: query.description ?? '用户输入内容',
      maxLength: query.maxLength ?? 256,
    })
  }
  const fileIds = data?.fileIds ?? {}
  if (false !== fileIds.enabled) {
    items.push({
      name: 'fileIds',
      label: '文件列表',
      type: 'Array<File>',
      description: fileIds.description ?? '用户上传的文件标识列表',
      maxCount: fileIds.maxCount ?? 3,
      fileTypes: fileIds.fileTypes ?? [],
    })
  }
  ;(data?.variables ?? []).forEach((item: any) => {
    if (!item?.name) return
    items.push({
      name: item.name,
      // 标题名称仅用于展示，为空时回落到变量名称
      label: item.label || item.name,
      type: item.type ?? 'String',
      description: item.description,
      required: true === item.required,
    })
  })
  return items
}

/**
 * 兼容历史数据：早期版本把用户输入与文件列表混在 variables 数组中，
 * 现拆分为固定输入 query、fileIds，variables 仅保留自定义参数。
 */
const StartRepair = (data: any) => {
  if (!data) return data
  const defaults = StartOptions()
  const variables: any[] = Array.isArray(data.variables) ? data.variables : []
  let legacyQuery: any = null
  let legacyFileIds: any = null
  const rest: any[] = []
  variables.forEach((item: any) => {
    if (!item?.name) return
    if ('query' === item.name) {
      legacyQuery = item
      return
    }
    if (['file', 'files', 'fileIds'].indexOf(item.name) !== -1) {
      legacyFileIds = item
      return
    }
    rest.push(item)
  })
  data.query = mergeOptions(defaults.query, data.query, {
    maxLength: legacyQuery?.maxLength,
    description: legacyQuery?.description,
  })
  data.fileIds = mergeOptions(defaults.fileIds, data.fileIds, {
    maxCount: legacyFileIds?.maxCount,
    fileTypes: legacyFileIds?.fileTypes,
    description: legacyFileIds?.description,
  })
  data.variables = rest
  return data
}

const EndOptions = () => {
  return {
    mode: config.mode || 'workflow',
    // 回复内容与输出变量均可为空：仅需返回回复文本时不必配置输出变量
    outputs: [],
    template: '',
  }
}

const LLMOptions = () => {
  return {
    model: '',
    temperature: 0.7,
    maxTokens: 0,
    topP: 1,
    systemPrompt: '',
    prompt: '',
    context: { enabled: false, variable: '' },
    memory: { enabled: false, window: 10 },
    vision: { enabled: false, variable: '' },
    structured: { enabled: false, schema: '' },
  }
}

const KnowledgeOptions = () => {
  return {
    query: '',
    knowledgeIds: [],
    strategy: 'semantic',
    topK: 3,
    score: 0.5,
    rerank: { enabled: false, model: '', topN: 3 },
    metadata: {},
  }
}

const ClassifierOptions = () => {
  return {
    model: '',
    query: '',
    instruction: '',
    memory: { enabled: false, window: 10 },
    classes: [
      { id: DesignUtil.uuid(), name: '分类一', description: '当用户的问题与分类一相关时命中' },
      { id: DesignUtil.uuid(), name: '分类二', description: '当用户的问题与分类二相关时命中' },
    ],
  }
}

const SwitchOptions = () => {
  return {
    defaultName: '否则（默认）',
    cases: [{
      id: DesignUtil.uuid(),
      name: '条件一',
      logic: 'and',
      conditions: [{ variable: '', operator: 'eq', value: '' }],
    }],
  }
}

const IterationOptions = () => {
  return {
    input: '',
    itemName: 'item',
    indexName: 'index',
    outputName: 'output',
    maxIterations: 10,
    parallel: false,
    parallelCount: 1,
    errorMode: 'terminated',
  }
}

const LoopOptions = () => {
  return {
    variables: [{ name: 'index', type: 'Integer', value: '0' }],
    condition: { logic: 'and', conditions: [{ variable: '', operator: 'lt', value: '10' }] },
    maxIterations: 100,
    outputName: 'output',
  }
}

const CodeOptions = () => {
  return {
    language: 'python3',
    code: CodeSamples.python3,
    inputs: [{ name: 'arg1', variable: '' }],
    outputs: [{ name: 'result', type: 'String' }],
  }
}

const TemplateOptions = () => {
  return { template: '', outputName: 'output', outputType: 'String' }
}

const AggregatorOptions = () => {
  return {
    strategy: 'first',
    variables: [{ variable: '' }, { variable: '' }],
    outputName: 'output',
    outputType: 'String',
  }
}

const DocumentOptions = () => {
  return { input: '', outputName: 'text', keepImages: true }
}

const AssignerOptions = () => {
  return {
    assignments: [
      { target: '', operation: 'set', source: 'variable', variable: '', value: '' },
    ],
  }
}

const ParameterOptions = () => {
  return {
    model: '',
    query: '',
    instruction: '',
    memory: { enabled: false, window: 10 },
    parameters: [{ name: 'language', type: 'String', required: true, description: '编程语言名称' }],
  }
}

const HttpOptions = () => {
  return {
    method: 'GET',
    url: '',
    authorization: { type: 'none', apiKey: '', header: '', username: '', password: '' },
    headers: {},
    params: {},
    body: { type: 'none', json: '', form: {} },
    timeout: 30,
    sslVerify: true,
  }
}

const ListOptions = () => {
  return {
    input: '',
    filter: { logic: 'and', conditions: [] },
    sorts: [{ variable: '', order: 'asc' }],
    limit: { type: 'all', size: 10 },
    outputName: 'result',
  }
}

const TimeOptions = () => {
  return {
    operation: 'current',
    timezone: 'Asia/Shanghai',
    variable: '',
    variable2: '',
    datetime: '',
    format: 'YYYY-MM-DD HH:mm:ss',
    targetTimezone: 'Asia/Shanghai',
    amount: 1,
    unit: 'day',
    outputName: 'output',
    outputType: 'String',
  }
}

/* ------------------------------- 输出变量 ------------------------------- */

const outputs: any = {
  // 开始节点：固定输入（用户输入、文件列表）与自定义参数共同作为下游可引用变量
  Start: (data: any) => StartInputs(data).map((item: any) => ({
    name: item.name, label: item.label, type: item.type, description: item.description,
  })),
  // 结束节点：回复内容与输出变量在对话流与工作流下均可配置
  End: (data: any) => [{
    name: 'answer', label: '回复内容', type: 'String', description: '回复内容',
  }].concat((data.outputs ?? []).filter((item: any) => item?.name).map((item: any) => ({
    name: item.name, label: item.label || item.name, type: item.type,
  }))),
  LLM: () => [
    { name: 'text', label: '回复文本', type: 'String', description: '模型回复内容' },
    { name: 'reasoning', label: '思考过程', type: 'String', description: '模型思考过程' },
    { name: 'usage', label: '令牌用量', type: 'Object', description: '令牌用量信息' },
  ],
  Knowledge: () => [
    { name: 'result', label: '召回结果', type: 'Array<Object>', description: '召回结果列表' },
    { name: 'text', label: '召回内容', type: 'String', description: '召回内容拼接文本' },
  ],
  QuestionClassifier: () => [
    { name: 'classId', label: '命中分类标识', type: 'String', description: '命中分类的标识' },
    { name: 'className', label: '命中分类名称', type: 'String', description: '命中分类的名称' },
  ],
  SwitchCase: () => [],
  // 迭代/循环容器不再有固定入口节点：元素、索引与循环变量由容器节点自身提供，
  // 标记 scope 的条目只对容器内的节点可见（见 variable.ts）
  Iteration: (data: any) => [
    { name: data.outputName || 'output', label: '迭代结果', type: 'Array<Object>', description: '各次迭代的输出集合' },
    { name: data.itemName || 'item', label: '当前元素', type: 'Object', description: '当前迭代的元素', scope: true },
    { name: data.indexName || 'index', label: '当前索引', type: 'Integer', description: '当前迭代的索引', scope: true },
  ],
  Loop: (data: any) => [
    { name: data.outputName || 'output', label: '循环结果', type: 'Array<Object>', description: '各次循环的输出集合' },
  ].concat((data.variables ?? []).filter((item: any) => item?.name).map((item: any) => ({
    name: item.name, label: item.label || item.name, type: item.type, description: '循环变量的当前取值', scope: true,
  }))),
  Code: (data: any) => (data.outputs ?? []).filter((item: any) => item?.name).map((item: any) => ({
    name: item.name, label: item.label || item.name, type: item.type,
  })),
  Template: (data: any) => [
    { name: data.outputName || 'output', label: '模板结果', type: data.outputType || 'String', description: '模板渲染结果' },
  ],
  VariableAggregator: (data: any) => [
    { name: data.outputName || 'output', label: '聚合结果', type: data.outputType || 'String', description: '聚合后的变量' },
  ],
  DocumentExtractor: (data: any) => [
    { name: data.outputName || 'text', label: '解析文本', type: 'String', description: '文档解析出的文本' },
  ],
  VariableAssigner: (data: any) => (data.assignments ?? []).filter((item: any) => item?.target).map((item: any) => ({
    name: item.target, label: item.target, type: 'String', description: '赋值后的变量',
  })),
  ParameterExtractor: (data: any) => [{
    name: '__isSuccess', label: '是否提取成功', type: 'Boolean', description: '是否提取成功',
  }, {
    name: '__reason', label: '提取说明', type: 'String', description: '提取结果说明',
  }].concat((data.parameters ?? []).filter((item: any) => item?.name).map((item: any) => ({
    name: item.name, label: item.label || item.name, type: item.type, description: item.description,
  }))),
  HTTP: () => [
    { name: 'body', label: '响应内容', type: 'String', description: '响应内容' },
    { name: 'statusCode', label: '响应状态码', type: 'Integer', description: '响应状态码' },
    { name: 'headers', label: '响应头', type: 'Object', description: '响应头信息' },
    { name: 'files', label: '响应文件', type: 'Array<File>', description: '响应文件列表' },
  ],
  ListOperator: (data: any) => [
    { name: data.outputName || 'result', label: '处理结果', type: 'Array<Object>', description: '处理后的列表' },
    { name: 'first', label: '首项', type: 'Object', description: '处理后的首项' },
    { name: 'total', label: '条目总数', type: 'Integer', description: '处理后列表长度' },
  ],
  Time: (data: any) => [
    { name: data.outputName || 'output', label: '时间结果', type: data.outputType || 'String', description: '时间处理结果' },
  ],
}

/* ------------------------------- 字典数据 ------------------------------- */

const CodeSamples: any = {
  python3: [
    'def main(arg1: str) -> dict:',
    '    return {',
    '        "result": arg1,',
    '    }',
  ].join('\n'),
  nodejs: [
    'async function main({ arg1 }) {',
    '  return {',
    '    result: arg1,',
    '  }',
    '}',
  ].join('\n'),
}

const types = [
  { label: '文本', value: 'String' },
  { label: '整数', value: 'Integer' },
  { label: '小数', value: 'Float' },
  { label: '布尔', value: 'Boolean' },
  { label: '对象', value: 'Object' },
  { label: '对象数组', value: 'Array<Object>' },
  { label: '文本数组', value: 'Array<String>' },
  { label: '文件', value: 'File' },
  { label: '文件数组', value: 'Array<File>' },
]

// 开始节点自定义参数类型：文本单行输入，段落多行输入，数值/整数为数字输入
const inputTypes = [
  { label: '文本', value: 'String' },
  { label: '段落', value: 'Paragraph' },
  { label: '数值', value: 'Number' },
  { label: '整数', value: 'Integer' },
  { label: '布尔', value: 'Boolean' },
]

// 开始节点文件列表可上传的文件类型，未选择表示不限
const fileTypes = [
  { label: '图片', value: 'image' },
  { label: '文档', value: 'document' },
  { label: '表格', value: 'spreadsheet' },
  { label: '演示文稿', value: 'presentation' },
  { label: 'PDF', value: 'pdf' },
  { label: '文本', value: 'text' },
  { label: '压缩包', value: 'archive' },
]

const operators = [
  { label: '存在', value: 'exists' },
  { label: '为空', value: 'empty' },
  { label: '等于', value: 'eq' },
  { label: '不等于', value: 'ne' },
  { label: '包含', value: 'contains' },
  { label: '不包含', value: 'notContains' },
  { label: '以...开头', value: 'startsWith' },
  { label: '以...结尾', value: 'endsWith' },
  { label: '匹配正则', value: 'regex' },
  { label: '大于', value: 'gt' },
  { label: '大于等于', value: 'gte' },
  { label: '小于', value: 'lt' },
  { label: '小于等于', value: 'lte' },
  { label: '属于', value: 'in' },
  { label: '不属于', value: 'notIn' },
]

const modes = [
  { label: '工作流', value: 'workflow' },
  { label: '对话流', value: 'chat' },
]

const knowledgeStrategies = [
  { label: '向量检索', value: 'semantic' },
  { label: '全文检索', value: 'fulltext' },
  { label: '混合检索', value: 'hybrid' },
]

const codeLanguages = [
  { label: 'Python', value: 'python3' },
  { label: 'NodeJS', value: 'nodejs' },
]

const aggregateStrategies = [
  { label: '优先取第一个不为空的值', value: 'first' },
  { label: '返回全部变量', value: 'all' },
]

const loopErrorModes = [
  { label: '终止循环', value: 'terminated' },
  { label: '继续执行', value: 'continue' },
]

const listLimitTypes = [
  { label: '全部', value: 'all' },
  { label: '取前 N 项', value: 'first' },
  { label: '取后 N 项', value: 'last' },
]

const httpMethods = [
  { label: 'GET', value: 'GET' },
  { label: 'POST', value: 'POST' },
  { label: 'PUT', value: 'PUT' },
  { label: 'PATCH', value: 'PATCH' },
  { label: 'DELETE', value: 'DELETE' },
  { label: 'HEAD', value: 'HEAD' },
]

const httpBodyTypes = [
  { label: '无', value: 'none' },
  { label: 'JSON', value: 'json' },
  { label: '表单', value: 'form' },
]

const httpAuthTypes = [
  { label: '无', value: 'none' },
  { label: 'API Key', value: 'apiKey' },
  { label: 'Bearer', value: 'bearer' },
  { label: 'Basic', value: 'basic' },
]

const timeOperations = [
  { label: '获取当前时间', value: 'current' },
  { label: '获取时间戳', value: 'now2timestamp' },
  { label: '时间戳转时间', value: 'timestamp2time' },
  { label: '时间转时间戳', value: 'time2timestamp' },
  { label: '时区转换', value: 'timezone' },
  { label: '时间格式转换', value: 'format' },
  { label: '时间加减', value: 'add' },
  { label: '时间差计算', value: 'diff' },
  { label: '星期几计算器', value: 'weekday' },
]

const timeUnits = [
  { label: '秒', value: 'second' },
  { label: '分钟', value: 'minute' },
  { label: '小时', value: 'hour' },
  { label: '天', value: 'day' },
  { label: '周', value: 'week' },
  { label: '月', value: 'month' },
  { label: '年', value: 'year' },
]

const timezones = [
  { label: 'UTC (UTC+00:00)', value: 'UTC' },
  { label: 'Asia/Shanghai (UTC+08:00)', value: 'Asia/Shanghai' },
  { label: 'Asia/Hong_Kong (UTC+08:00)', value: 'Asia/Hong_Kong' },
  { label: 'Asia/Taipei (UTC+08:00)', value: 'Asia/Taipei' },
  { label: 'Asia/Singapore (UTC+08:00)', value: 'Asia/Singapore' },
  { label: 'Asia/Tokyo (UTC+09:00)', value: 'Asia/Tokyo' },
  { label: 'Asia/Seoul (UTC+09:00)', value: 'Asia/Seoul' },
  { label: 'Asia/Bangkok (UTC+07:00)', value: 'Asia/Bangkok' },
  { label: 'Asia/Jakarta (UTC+07:00)', value: 'Asia/Jakarta' },
  { label: 'Asia/Kolkata (UTC+05:30)', value: 'Asia/Kolkata' },
  { label: 'Asia/Dubai (UTC+04:00)', value: 'Asia/Dubai' },
  { label: 'Europe/Moscow (UTC+03:00)', value: 'Europe/Moscow' },
  { label: 'Europe/Paris (UTC+01:00)', value: 'Europe/Paris' },
  { label: 'Europe/London (UTC+00:00)', value: 'Europe/London' },
  { label: 'America/Sao_Paulo (UTC-03:00)', value: 'America/Sao_Paulo' },
  { label: 'America/New_York (UTC-05:00)', value: 'America/New_York' },
  { label: 'America/Chicago (UTC-06:00)', value: 'America/Chicago' },
  { label: 'America/Denver (UTC-07:00)', value: 'America/Denver' },
  { label: 'America/Los_Angeles (UTC-08:00)', value: 'America/Los_Angeles' },
  { label: 'Africa/Cairo (UTC+02:00)', value: 'Africa/Cairo' },
  { label: 'Australia/Sydney (UTC+10:00)', value: 'Australia/Sydney' },
  { label: 'Pacific/Auckland (UTC+12:00)', value: 'Pacific/Auckland' },
]

/* ------------------------------- 组件分组 ------------------------------- */

const widgets = DesignUtil.widgets([{
  name: '基础节点',
  children: [{
    type: 'Start', label: '开始', title: '工作流的入口，定义输入变量', icon: 'flow.startEvent',
    shape: 'agent-node', options: StartOptions, repair: StartRepair, property: () => import('./StartProperty.vue')
  }, {
    type: 'End', label: '结束', title: '流程出口：工作流输出变量，对话流输出回复内容', icon: 'flow.endEvent',
    shape: 'agent-node', options: EndOptions, property: () => import('./EndProperty.vue')
  }]
}, {
  name: '模型能力',
  children: [{
    type: 'LLM', label: '大语言模型', title: '调用大语言模型回答问题或处理自然语言', icon: 'ai.model',
    shape: 'agent-node', options: LLMOptions, property: () => import('./LLMProperty.vue')
  }, {
    type: 'Knowledge', label: '知识检索', title: '从知识库中查询与用户问题相关的文本内容', icon: 'algorithm.retrieval',
    shape: 'agent-node', options: KnowledgeOptions, property: () => import('./KnowledgeProperty.vue')
  }, {
    type: 'QuestionClassifier', label: '问题分类器', title: '按分类描述定义对话的进展方式', icon: 'Guide',
    shape: 'agent-switch', options: ClassifierOptions, property: () => import('./ClassifierProperty.vue'),
    size: (data: any) => ({ width: SwitchLayout.width, height: SwitchLayout.height(data) }),
    ports: (data: any) => SwitchLayout.ports(data),
  }, {
    type: 'ParameterExtractor', label: '参数提取器', title: '从自然语言中推理提取结构化参数', icon: 'algorithm.extraction',
    shape: 'agent-node', options: ParameterOptions, property: () => import('./ParameterProperty.vue')
  }]
}, {
  name: '逻辑处理',
  children: [{
    type: 'SwitchCase', label: '条件分支', title: '按 switch/case 匹配分支，每个 case 与 default 均可连线',
    icon: 'flow.exclusiveGateway', shape: 'agent-switch', options: SwitchOptions, property: () => import('./SwitchProperty.vue'),
    size: (data: any) => ({ width: SwitchLayout.width, height: SwitchLayout.height(data) }),
    ports: (data: any) => SwitchLayout.ports(data),
  }, {
    type: 'Iteration', label: '迭代', title: '对列表对象执行多次步骤直至输出所有结果', icon: 'RefreshRight',
    shape: 'flow-subprocess', options: IterationOptions, property: () => import('./IterationProperty.vue')
  }, {
    type: 'Loop', label: '循环', title: '循环执行一段逻辑直到满足结束条件或到达上限', icon: 'RefreshLeft',
    shape: 'flow-subprocess', options: LoopOptions, property: () => import('./LoopProperty.vue')
  }, {
    type: 'VariableAggregator', label: '变量聚合器', title: '将多路分支的变量聚合为一个变量', icon: 'Share',
    shape: 'agent-node', options: AggregatorOptions, property: () => import('./AggregatorProperty.vue')
  }, {
    type: 'VariableAssigner', label: '变量赋值', title: '向会话变量等可写入变量进行赋值', icon: 'flow.config',
    shape: 'agent-node', options: AssignerOptions, property: () => import('./AssignerProperty.vue')
  }]
}, {
  name: '数据处理',
  children: [{
    type: 'Code', label: '代码执行', title: '执行一段 Python 或 NodeJS 代码实现自定义逻辑', icon: 'flow.script',
    shape: 'agent-node', options: CodeOptions, property: () => import('./CodeProperty.vue')
  }, {
    type: 'Template', label: '模板转换', title: '使用 Jinja 模板语法将数据转换为字符串', icon: 'flow.transform',
    shape: 'agent-node', options: TemplateOptions, property: () => import('./TemplateProperty.vue')
  }, {
    type: 'DocumentExtractor', label: '文档提取器', title: '将用户上传的文档解析为便于理解的文本', icon: 'Document',
    shape: 'agent-node', options: DocumentOptions, property: () => import('./DocumentProperty.vue')
  }, {
    type: 'ListOperator', label: '列表操作', title: '用于过滤或排序数组内容', icon: 'Operation',
    shape: 'agent-node', options: ListOptions, property: () => import('./ListProperty.vue')
  }, {
    type: 'Time', label: '时间', title: '时间戳转换、时区转换、获取当前时间等', icon: 'Timer',
    shape: 'agent-node', options: TimeOptions, property: () => import('./TimeProperty.vue')
  }]
}, {
  name: '集成调用',
  children: [{
    type: 'HTTP', label: 'HTTP请求', title: '通过 HTTP 协议发送服务器请求', icon: 'Link',
    shape: 'agent-node', options: HttpOptions, property: () => import('./HttpProperty.vue')
  }]
}])

export default Object.assign(config, {
  canvas: { options: CanvasOptions, property: () => import('./CanvasProperty.vue') },
  edge: { options: EdgeOptions, property: () => import('./EdgeProperty.vue') },
  widgets,
  outputs,
  mode: 'workflow', // 当前编排的应用类型，由设计器同步
  types,
  inputTypes,
  fileTypes,
  startInputs: StartInputs,
  startRepair: StartRepair,
  operators,
  modes,
  knowledgeStrategies,
  codeLanguages,
  codeSamples: CodeSamples,
  aggregateStrategies,
  loopErrorModes,
  listLimitTypes,
  httpMethods,
  httpBodyTypes,
  httpAuthTypes,
  timeOperations,
  timeUnits,
  timezones,
  logicOperators: [{ label: '全部', value: 'and' }, { label: '任一', value: 'or' }],
  noValueOperators: ['exists', 'empty'],
  sortOrders: [{ label: '升序', value: 'asc' }, { label: '降序', value: 'desc' }],
  assignOperations: [
    { label: '设置', value: 'set' },
    { label: '追加', value: 'append' },
    { label: '累加', value: 'increment' },
    { label: '累减', value: 'decrement' },
    { label: '清空', value: 'clear' },
  ],
  assignSources: [
    { label: '引用变量', value: 'variable' },
    { label: '固定值', value: 'constant' },
  ],
  status: [], // 由后台服务补齐
  toolbars: [{
    type: 'hand', label: '拖动', icon: 'action.hand', selectable: true, selected: true, callback (toolbar: any, instance: any) { instance.flow.panning() }
  }, {
    type: 'lasso', label: '框选', icon: 'action.lasso', selectable: true, callback (toolbar: any, instance: any) { instance.flow.selecting() }
  }, {
    type: 'fit', label: '适合', icon: 'action.fit', callback (toolbar: any, instance: any) { instance.flow.fitting() }
  }, {
    type: 'divider'
  }, {
    type: 'clean', label: '清空', icon: 'action.clean', callback (toolbar: any, instance: any) {
      instance.flow.fromJSON()
      instance.flow.options.onBlankClick()
    }
  }],
})
