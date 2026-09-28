import DesignUtil from '@/utils/DesignUtil'
import SwitchLayout from '@/designer/X6/switch'
import AgenticUtil from '@/utils/AgenticUtil'

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
    query: { enabled: true, maxLength: 25600, description: '用户输入内容' },
    // 文件列表为文件信息数组：每项是文件存储服务返回的文件ID、文件名、文件类型等，不是前端的 File 对象
    files: { enabled: true, maxCount: 3, fileTypes: [], description: '用户上传的文件列表，含文件存储服务返回的文件ID、文件名、文件类型等信息' },
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
 * 开始节点的输入清单：实现放在 AgenticUtil，与对话页「参数配置」表单共用同一份口径
 */
const StartInputs = (data: any) => AgenticUtil.startInputs(data)

/**
 * 兼容历史数据：早期版本把用户输入与文件列表混在 variables 数组中，
 * 现拆分为固定输入 query、files，variables 仅保留自定义参数；
 * 文件列表由 fileIds（文件标识列表）改为 files（文件存储服务返回的文件信息数组），原配置沿用。
 */
const StartRepair = (data: any) => {
  if (!data) return data
  const defaults = StartOptions()
  const variables: any[] = Array.isArray(data.variables) ? data.variables : []
  let legacyQuery: any = null
  let legacyFiles: any = null
  const rest: any[] = []
  variables.forEach((item: any) => {
    if (!item?.name) return
    if ('query' === item.name) {
      legacyQuery = item
      return
    }
    if (['file', 'files', 'fileIds'].indexOf(item.name) !== -1) {
      legacyFiles = item
      return
    }
    rest.push(item)
  })
  data.query = mergeOptions(defaults.query, data.query, {
    maxLength: legacyQuery?.maxLength,
    description: legacyQuery?.description,
  })
  // 旧字段 fileIds 与 variables 中拆出的文件参数都并入 files
  data.files = mergeOptions(defaults.files, data.files ?? data.fileIds, {
    maxCount: legacyFiles?.maxCount,
    fileTypes: legacyFiles?.fileTypes,
    description: legacyFiles?.description,
  })
  delete data.fileIds
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
    // 温度、思考模式、思考强度各自可开关，默认不启用，关闭时该参数不参与请求
    temperatureEnabled: false,
    temperature: 0.7,
    // 思考模式与思考强度（思考强度仅在开启思考时可配）
    thinkModeEnabled: false,
    thinkMode: 'auto',
    thinkEffortEnabled: false,
    thinkEffort: 'medium',
    // AGENT 策略：无 / ReAct / FunctionCalling
    agentStrategy: 'none',
    // ReAct 的最大迭代次数：模型返回工具调用就执行并回填上下文继续推理，
    // 直到给出非工具调用的回复或达到该上限（达到上限按节点异常处理）；FunctionCalling 只执行一轮
    maxIterations: 5,
    // 系统指令与用户输入
    systemPrompt: '',
    prompt: '',
    /**
     * 工具列表：{ id, toolId, toolName, method, enabled, args }
     * - method 为工具暴露的方法名（工具与方法由后端解析落库，见 tool.ts）
     * - args 为该方法的执行变量绑定：`{ auto: true }` 由模型决定；
     *   `{ auto: false, value }` 手工指定，内容可混排固定字符串与变量占位符，由后端按内容解析
     * - 对象参数（如 body）按字段绑定：`{ source: 'fields', fields: { 字段名: 绑定 } }`，未配置字段仍由模型决定
     */
    tools: [],
    // 记忆：范围 conversation=完整对话（多轮对话需要）/ user=仅用户提问；工具链开启后历史轮次的工具调用加入上下文
    memory: { enabled: true, window: 10, scope: 'conversation', toolchain: false },
    // 多模态输入参数：{ name, type, variable }
    multimodalEnabled: false,
    multimodal: [],
  }
}

const ChartOptions = () => {
  return {
    model: '',
    // 温度、思考模式与思考强度各自可开关，默认不启用，关闭时该参数不参与请求
    temperatureEnabled: false,
    temperature: 0.7,
    thinkModeEnabled: false,
    thinkMode: 'auto',
    thinkEffortEnabled: false,
    thinkEffort: 'medium',
    // 图表类型、展示数据与图表说明都由模型按用户问题与大语言模型本轮记录自动决定，节点不需要额外配置；
    // 默认读取开始节点的用户输入与上游大语言模型本轮的回复、工具调用结果，其余上游节点仅在缺少模型节点时兜底；
    // 这里只保留系统提示词（补充统计口径等要求）与多模态输入参数，与其它模型节点保持一致
    systemPrompt: '',
    // 记忆：图表只根据用户问题和本轮数据归纳，默认只带历史里的用户提问
    memory: { enabled: true, window: 10, scope: 'user', toolchain: false },
    multimodalEnabled: false,
    multimodal: [],
  }
}

const KnowledgeOptions = () => {
  return {
    query: '',
    knowledgeIds: [],
    // 元数据过滤条件
    metadata: {},
  }
}

const ClassifierOptions = () => {
  return {
    model: '',
    // 温度、思考模式、思考强度各自可开关
    temperatureEnabled: false,
    temperature: 0.7,
    thinkModeEnabled: false,
    thinkMode: 'auto',
    thinkEffortEnabled: false,
    thinkEffort: 'medium',
    // 输入变量与系统指令
    query: '',
    systemPrompt: '',
    // 记忆：分类只关心用户问题，默认只带历史里的用户提问
    memory: { enabled: true, window: 10, scope: 'user', toolchain: false },
    // 多模态输入参数：{ name, type, variable }
    multimodalEnabled: false,
    multimodal: [],
    classes: [
      { id: DesignUtil.uuid(), name: '分类一', description: '当用户的问题与分类一相关时命中' },
      { id: DesignUtil.uuid(), name: '分类二', description: '当用户的问题与分类二相关时命中' },
    ],
  }
}

/** 兼容历史数据：已配置多模态输入参数时默认启用（多模态开关是后加的，默认关闭） */
const MultimodalRepair = (data: any) => {
  if (!data) return data
  if (Array.isArray(data.multimodal) && data.multimodal.length && undefined === data.multimodalEnabled) {
    data.multimodalEnabled = true
  }
  return MemoryRepair(data)
}

/**
 * 兼容历史数据：记忆范围（memory.scope）是后加字段，缺省保持原有行为（完整对话）；
 * 没有记忆配置的模型节点（如早期的参数提取器）补一份默认配置，默认只带用户提问。
 */
const MemoryRepair = (data: any) => {
  if (!data) return data
  const memory = data.memory
  if (!memory || 'object' !== typeof memory) {
    data.memory = { enabled: true, window: 10, scope: 'user', toolchain: false }
    return data
  }
  if (undefined === memory.enabled) memory.enabled = true
  if (undefined === memory.window) memory.window = 10
  if (undefined === memory.toolchain) memory.toolchain = false
  if (undefined === memory.scope) memory.scope = 'conversation'
  return data
}

/** 兼容历史数据：分类指令（instruction）改名为系统指令（systemPrompt） */
const ClassifierRepair = (data: any) => {
  if (!data) return data
  if (undefined !== data.instruction) {
    if (undefined === data.systemPrompt || '' === data.systemPrompt) {
      data.systemPrompt = data.instruction
    }
    delete data.instruction
  }
  return MultimodalRepair(data)
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
    // 输出变量：容器内被收集的变量，各次迭代的取值组成输出变量名的值数组
    outputVariable: '',
    outputName: 'output',
    parallel: false,
    parallelCount: 1,
    // 错误处理：错误时终止 / 忽略错误并继续 / 移除错误输出（见 iterationErrorModes）
    errorMode: 'terminated',
  }
}

const LoopOptions = () => {
  return {
    // 循环变量：初始值来源可为固定值或引用变量，容器内的节点可通过变量赋值覆盖其取值；
    // 标题名称仅用于展示，为空时展示变量名
    variables: [{ name: 'index', label: '循环变量', type: 'Integer', source: 'constant', variable: '', value: '' }],
    condition: { logic: 'and', conditions: [{ variable: '', operator: 'lt', value: '10' }] },
    maxIterations: 100,
  }
}

/** 兼容历史数据：循环变量的初始值来源（固定值/引用变量）是后加字段，缺省按固定值处理 */
const LoopRepair = (data: any) => {
  if (!data) return data
  const variables: any[] = Array.isArray(data.variables) ? data.variables : []
  variables.forEach((item: any) => {
    if (undefined === item.source) item.source = 'constant'
    if (undefined === item.variable) item.variable = ''
  })
  return data
}

const CodeOptions = () => {
  return {
    // 仅支持 NodeJS：语言固定，界面不再提供语言选择
    language: 'nodejs',
    code: CodeSamples.nodejs,
    inputs: [{ name: 'arg1', variable: '' }],
    outputs: [{ name: 'result', type: 'String' }],
  }
}

/** 旧版默认的 Python 示例代码：Python 已不再支持，仅用于历史数据迁移时识别没改过的默认示例 */
const LegacyPythonSample = [
  'def main(arg1: str) -> dict:',
  '    return {',
  '        "result": arg1,',
  '    }',
].join('\n')

/** 旧版默认示例：async 函数与解构参数在服务端的 JavaScript 引擎里不可用，仅用于历史数据迁移 */
const LegacyNodeSample = [
  'async function main({ arg1 }) {',
  '  return {',
  '    result: arg1,',
  '  }',
  '}',
].join('\n')

/**
 * 兼容历史数据：代码节点只支持 NodeJS。
 * 旧数据统一改回 NodeJS；代码为空或仍是默认的 Python 示例时替换为 NodeJS 示例，自己写过的代码保留不动
 */
const CodeRepair = (data: any) => {
  if (!data) return data
  if ('nodejs' !== data.language) {
    if (!data.code || LegacyPythonSample === data.code) data.code = CodeSamples.nodejs
    data.language = 'nodejs'
  }
  // 早期默认示例用了 async 与解构参数，服务端按 JavaScript 引擎执行时不兼容，替换为新示例
  if (LegacyNodeSample === data.code) data.code = CodeSamples.nodejs
  return data
}

const TemplateOptions = () => {
  return {
    // 输入变量：{ name, variable }，模板里按 name 引用，不再往模板里插入占位符
    inputs: [{ name: 'input', variable: '' }],
    template: '',
    outputName: 'output',
    outputType: 'String',
  }
}

const AggregatorOptions = () => {
  return {
    // 分组：每个分组产出一个变量，变量名取分组名称，组内变量按清单顺序聚合
    groups: [{
      id: DesignUtil.uuid(),
      name: 'output',
      outputType: 'String',
      variables: [{ variable: '' }, { variable: '' }],
    }],
  }
}

/** 兼容历史数据：聚合器由「单个输出变量」改为「分组，分组名称即输出变量名」 */
const AggregatorRepair = (data: any) => {
  if (!data) return data
  if (!Array.isArray(data.groups)) {
    const variables: any[] = Array.isArray(data.variables) ? data.variables : []
    data.groups = [{
      id: DesignUtil.uuid(),
      name: data.outputName || 'output',
      outputType: data.outputType || 'String',
      variables: variables.length ? variables : [{ variable: '' }, { variable: '' }],
    }]
  }
  // 分组上后加的字段：缺类型按文本处理，变量清单兜底为空数组
  data.groups.forEach((group: any) => {
    if (!group) return
    if (undefined === group.outputType) group.outputType = 'String'
    if (!Array.isArray(group.variables)) group.variables = []
  })
  delete data.strategy
  delete data.outputName
  delete data.outputType
  delete data.variables
  return data
}

const DocumentOptions = () => {
  return { input: '', outputName: 'text' }
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
    // 与其它模型节点保持一致：温度、思考模式、思考强度各自可开关
    temperatureEnabled: false,
    temperature: 0.7,
    thinkModeEnabled: false,
    thinkMode: 'auto',
    thinkEffortEnabled: false,
    thinkEffort: 'medium',
    // 输入变量与系统指令
    query: '',
    systemPrompt: '',
    // 记忆：提取器只按本轮输入提取，默认只带历史里的用户提问
    memory: { enabled: true, window: 10, scope: 'user', toolchain: false },
    // 多模态输入参数：{ name, type, variable }
    multimodalEnabled: false,
    multimodal: [],
    // 参数名是模型返回值的键（英文标识），标题名称用于输出变量等处的展示
    parameters: [{ name: 'language', label: '编程语言', type: 'String', required: true, description: '编程语言名称' }],
  }
}

/** 兼容历史数据：提取指令（instruction）改名为系统提示词（systemPrompt），与其它模型节点一致 */
const ParameterRepair = (data: any) => {
  if (!data) return data
  if (undefined !== data.instruction) {
    if (undefined === data.systemPrompt || '' === data.systemPrompt) {
      data.systemPrompt = data.instruction
    }
    delete data.instruction
  }
  return MultimodalRepair(data)
}

const HttpOptions = () => {
  return {
    method: 'GET',
    url: '',
    // 认证不单独配置：需要的令牌/密钥按请求头填写（见 HttpProperty 的「请求头」）
    headers: {},
    // 请求参数：none / form-data / x-www-form-urlencoded / json / raw；
    // json 与 raw 共用 content 字段（与数据接口配置的 payloadBody 一致）
    body: { type: 'none', content: '', form: {} },
    timeout: 30,
    sslVerify: true,
  }
}

/**
 * 兼容历史数据：
 * 1. 不再单独配置授权认证，旧认证信息转成请求头，避免升级后请求丢掉鉴权；
 * 2. 请求参数类型与数据接口配置对齐（form → form-data，json 与 raw 共用 content 字段）；
 * 3. 不再有单独的查询参数，旧参数拼到请求地址上。
 */
const HttpRepair = (data: any) => {
  if (!data) return data
  // 授权认证 → 请求头
  if (data.authorization) {
    const auth: any = data.authorization
    const headers: any = Object.assign({}, data.headers)
    try {
      if ('apiKey' === auth.type && auth.apiKey) {
        headers[auth.header || 'X-API-Key'] = auth.apiKey
      } else if ('bearer' === auth.type && auth.apiKey) {
        headers['Authorization'] = 'Bearer ' + auth.apiKey
      } else if ('basic' === auth.type) {
        headers['Authorization'] = 'Basic ' + window.btoa(`${auth.username ?? ''}:${auth.password ?? ''}`)
      }
      data.headers = headers
    } catch (error) {
      // 账号含非 ASCII 字符时 btoa 会失败，此时保留原请求头，认证信息由使用者自行补录
    }
    delete data.authorization
  }
  // 请求体字段对齐
  if (data.body) {
    if ('form' === data.body.type) data.body.type = 'form-data'
    if (undefined !== data.body.json) {
      if (undefined === data.body.content) data.body.content = data.body.json
      delete data.body.json
    }
    if (!data.body.form || 'object' !== typeof data.body.form) data.body.form = {}
  }
  // 查询参数 → 请求地址
  const params: any = data.params
  if (params && 'object' === typeof params) {
    const query: string = Object.keys(params)
      .filter((key: string) => key)
      .map((key: string) => `${key}=${params[key]}`)
      .join('&')
    if (query) {
      const url = String(data.url ?? '')
      data.url = `${url}${url.indexOf('?') >= 0 ? '&' : '?'}${query}`
    }
  }
  delete data.params
  return data
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
    format: 'yyyy-MM-dd HH:mm:ss',
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
    { name: 'calls', label: '工具调用', type: 'Array<Object>', description: '本轮工具调用明细（方法名、参数与返回结果），可交给「输出图表」节点归纳成图表数据' },
  ],
  // 输出图表：模型按内置提示词归纳出的图表定义，下游节点可引用其中任意字段
  Chart: () => [
    { name: 'hasChart', label: '是否有图表', type: 'Boolean', description: '本轮上下文是否产出了图表：模型判定无需绘图时为 false，可在条件分支里引用' },
    { name: 'placeholder', label: '图表占位符', type: 'String', description: '把它插入到结束节点的回复内容里（如 正文 + {{#n1.placeholder#}} + 结尾），图表就渲染在这个位置；不引用则追加在回复末尾。它只是正文里的一行标记，不是画布位置' },
    { name: 'type', label: '图表类型', type: 'String', description: 'bar 柱状 / line 折线 / pie 饼图 / table 表格' },
    { name: 'title', label: '图表标题', type: 'String', description: '图表标题（统计口径）' },
    { name: 'source', label: '数据来源', type: 'String', description: '数据来源说明' },
    { name: 'categories', label: '分类取值', type: 'Array<Object>', description: '分类轴取值' },
    { name: 'series', label: '系列数据', type: 'Array<Object>', description: '数值系列：{ name, data }' },
    { name: 'text', label: '模型输出', type: 'String', description: '模型返回的原始图表定义' },
  ],
  Knowledge: () => [
    { name: 'result', label: '召回结果', type: 'Array<Object>', description: '召回结果列表' },
    { name: 'text', label: '召回内容', type: 'String', description: '召回内容拼接文本' },
    { name: 'documents', label: '召回文档', type: 'Array<Object>', description: '本次召回涉及的文档（含文档标识、名称与元数据），按文档去重' },
  ],
  QuestionClassifier: () => [
    { name: 'classId', label: '命中分类标识', type: 'String', description: '命中分类的标识' },
    { name: 'className', label: '命中分类名称', type: 'String', description: '命中分类的名称' },
    { name: 'text', label: '模型输出', type: 'String', description: '分类模型返回的原始内容' },
  ],
  SwitchCase: () => [],
  // 迭代/循环容器不再有固定入口节点：元素、索引与循环变量由容器节点自身提供，
  // 标记 scope 的条目只对容器内的节点可见（见 variable.ts）
  Iteration: (data: any) => [
    { name: data.outputName || 'output', label: '迭代结果', type: 'Array<Object>', description: '各次迭代收集的输出变量取值集合' },
    { name: data.itemName || 'item', label: '当前元素', type: 'Object', description: '当前迭代的元素', scope: true },
    { name: data.indexName || 'index', label: '当前索引', type: 'Integer', description: '当前迭代的索引', scope: true },
  ],
  // 循环变量即循环的状态，只对容器自身与其内部的节点可见，内部节点可通过变量赋值覆盖其取值；
  // 循环结束时容器作用域回收，容器外的节点只能引用 variables（循环变量的取值集合）
  Loop: (data: any) => [{
    name: 'variables', label: '循环变量快照', type: 'Object',
    description: '循环结束时的变量取值集合（容器外引用循环结果用）',
  }].concat((data.variables ?? []).filter((item: any) => item?.name).map((item: any) => ({
    name: item.name, label: item.label || item.name, type: item.type, description: '循环变量的当前取值', scope: true,
  }))),
  Code: (data: any) => (data.outputs ?? []).filter((item: any) => item?.name).map((item: any) => ({
    name: item.name, label: item.label || item.name, type: item.type,
  })),
  Template: (data: any) => [
    { name: data.outputName || 'output', label: '模板结果', type: data.outputType || 'String', description: '模板渲染结果' },
  ],
  // 每个分组产出一个变量，变量名取分组名称，类型取分组上配置的输出类型
  VariableAggregator: (data: any) => (data.groups ?? []).filter((group: any) => group?.name).map((group: any) => ({
    name: group.name, label: group.label || group.name, type: group.outputType || 'String', description: '聚合后的变量',
  })),
  DocumentExtractor: (data: any) => [
    { name: data.outputName || 'text', label: '解析文本', type: 'String', description: '文档解析出的文本' },
  ],
  // 变量赋值写入的是既有变量（容器变量 / 会话变量），自身没有输出变量：
  // 这里把赋值目标按统一规范的引用列出供下游引用，引用里的容器标识由 variable.ts 还原为节点名称展示
  VariableAssigner: (data: any) => {
    const references: string[] = []
    ;(data.assignments ?? []).forEach((item: any) => {
      const matched = String(item?.target ?? '').trim().match(/^\{\{#([^#{}]+)#\}\}$/)
      const reference = matched ? matched[1].trim() : ''
      if (reference && references.indexOf(reference) < 0) references.push(reference)
    })
    return references.map((reference: string) => ({
      name: reference, label: reference, type: 'String', description: '赋值后的变量', reference: true,
    }))
  },
  ParameterExtractor: (data: any) => [{
    name: '__isSuccess', label: '是否提取成功', type: 'Boolean', description: '是否提取成功',
  }, {
    name: '__reason', label: '提取说明', type: 'String', description: '提取结果说明',
  }, {
    name: 'text', label: '模型输出', type: 'String', description: '参数提取模型返回的原始内容',
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
  nodejs: [
    'function main(args) {',
    '  return {',
    '    result: args.arg1,',
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

// 大语言模型：思考模式、思考强度、AGENT 策略与多模态输入类型
const thinkModes = [
  { label: '自动', value: 'auto' },
  { label: '开启', value: 'on' },
  { label: '关闭', value: 'off' },
]

const thinkEfforts = [
  { label: '低', value: 'low' },
  { label: '中', value: 'medium' },
  { label: '高', value: 'high' },
  { label: '最高', value: 'max' },
]

const agentStrategies = [
  { label: '无', value: 'none' },
  { label: 'ReAct', value: 'react' },
  { label: 'FunctionCalling', value: 'functionCalling' },
]

const multimodalTypes = [
  { label: '图片', value: 'image' },
  { label: '音频', value: 'audio' },
  { label: '视频', value: 'video' },
  { label: '文件', value: 'file' },
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

const loopErrorModes = [
  { label: '终止循环', value: 'terminated' },
  { label: '继续执行', value: 'continue' },
]

// 循环变量的初始值来源：固定值（默认）或引用变量
const variableSources = [
  { label: '固定值', value: 'constant' },
  { label: '引用变量', value: 'variable' },
]

// 迭代节点的错误处理：出错即终止、忽略错误继续后续迭代、把出错的迭代结果从输出数组中移除
const iterationErrorModes = [
  { label: '错误时终止', value: 'terminated' },
  { label: '忽略错误并继续', value: 'continue' },
  { label: '移除错误输出', value: 'removed' },
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
  // 选项直接展示请求参数类型本身，不做中文翻译
  { label: 'none', value: 'none' },
  { label: 'form-data', value: 'form-data' },
  { label: 'x-www-form-urlencoded', value: 'x-www-form-urlencoded' },
  { label: 'json', value: 'json' },
  { label: 'raw', value: 'raw' },
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
    shape: 'agent-node', options: LLMOptions, repair: MultimodalRepair,
    property: () => import('./LLMProperty.vue')
  }, {
    type: 'Chart', label: '输出图表', title: '把上游数据交给模型归纳成图表定义，随回复一起展示', icon: 'PieChart',
    shape: 'agent-node', options: ChartOptions, repair: MultimodalRepair,
    property: () => import('./ChartProperty.vue')
  }, {
    type: 'Knowledge', label: '知识检索', title: '从知识库中查询与用户问题相关的文本内容', icon: 'algorithm.retrieval',
    shape: 'agent-node', options: KnowledgeOptions, property: () => import('./KnowledgeProperty.vue')
  }, {
    type: 'QuestionClassifier', label: '问题分类器', title: '按分类描述定义对话的进展方式', icon: 'Guide',
    shape: 'agent-switch', options: ClassifierOptions, repair: ClassifierRepair,
    property: () => import('./ClassifierProperty.vue'),
    size: (data: any) => ({ width: SwitchLayout.width, height: SwitchLayout.height(data) }),
    ports: (data: any) => SwitchLayout.ports(data),
  }, {
    type: 'ParameterExtractor', label: '参数提取器', title: '从自然语言中推理提取结构化参数', icon: 'algorithm.extraction',
    shape: 'agent-node', options: ParameterOptions, repair: ParameterRepair,
    property: () => import('./ParameterProperty.vue')
  }]
}, {
  name: '逻辑处理',
  children: [{
    type: 'SwitchCase', label: '条件分支', title: '按 switch/case 匹配分支，每个 case 与 default 均可连线',
    icon: 'flow.exclusiveGateway', shape: 'agent-switch', options: SwitchOptions, property: () => import('./SwitchProperty.vue'),
    size: (data: any) => ({ width: SwitchLayout.width, height: SwitchLayout.height(data) }),
    ports: (data: any) => SwitchLayout.ports(data),
  }, {
    type: 'Iteration', label: '迭代', title: '对列表对象执行多次步骤直至输出所有结果', icon: 'flow.iteration',
    shape: 'flow-subprocess', options: IterationOptions, property: () => import('./IterationProperty.vue')
  }, {
    type: 'Loop', label: '循环', title: '循环执行一段逻辑直到满足结束条件或到达上限', icon: 'flow.loop',
    shape: 'flow-subprocess', options: LoopOptions, repair: LoopRepair, property: () => import('./LoopProperty.vue')
  }, {
    type: 'VariableAggregator', label: '变量聚合器', title: '将多路分支的变量按分组聚合，分组名称即输出变量名', icon: 'Share',
    shape: 'agent-node', options: AggregatorOptions, repair: AggregatorRepair,
    property: () => import('./AggregatorProperty.vue')
  }, {
    type: 'VariableAssigner', label: '变量赋值', title: '向会话变量等可写入变量进行赋值', icon: 'flow.config',
    shape: 'agent-node', options: AssignerOptions, property: () => import('./AssignerProperty.vue')
  }]
}, {
  name: '数据处理',
  children: [{
    type: 'Code', label: '代码执行', title: '执行一段 JavaScript 代码实现自定义逻辑', icon: 'flow.script',
    shape: 'agent-node', options: CodeOptions, repair: CodeRepair, property: () => import('./CodeProperty.vue')
  }, {
    type: 'Template', label: '模板转换', title: '使用 Jinja2 模板语法将数据转换为字符串', icon: 'flow.transform',
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
    shape: 'agent-node', options: HttpOptions, repair: HttpRepair, property: () => import('./HttpProperty.vue')
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
  thinkModes,
  thinkEfforts,
  agentStrategies,
  multimodalTypes,
  startInputs: StartInputs,
  startRepair: StartRepair,
  operators,
  modes,
  codeSamples: CodeSamples,
  loopErrorModes,
  iterationErrorModes,
  variableSources,
  listLimitTypes,
  httpMethods,
  httpBodyTypes,
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
