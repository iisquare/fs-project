/**
 * 模型对比调试的类型契约：
 * - 工作区：当前这份配置（多模型 + 全局设置），随改随存
 * - 运行记录：一次提交（扇出到多个模型）的结果快照，用于回溯
 * - 结果：单个模型这一轮的输出，流式过程中实时写入
 */

/**
 * 思考模式：与设计器 config.ts 的 thinkModes 一致。
 * 接口未下发字典时用作默认值。
 */
export const COMPARE_THINK_MODES = [
  { label: '自动', value: 'auto' },
  { label: '开启', value: 'on' },
  { label: '关闭', value: 'off' },
]

/** 思考强度：同上，对应 thinkEfforts */
export const COMPARE_THINK_EFFORTS = [
  { label: '低', value: 'low' },
  { label: '中', value: 'medium' },
  { label: '高', value: 'high' },
  { label: '最高', value: 'max' },
]

/**
 * 单个模型的调试配置。
 *
 * 后四项各带一个启用开关：不开就不随请求下发，由服务端取自己的默认值。
 * 这样「没配过」与「显式配成 0 / 关闭」在语义上才分得开。
 */
export interface CompareModelConfig {
  /** 本地唯一键：同一个模型可以加两次，用于对照不同参数 */
  key: string
  /** 模型名称，与上游网关一致 */
  model: string
  temperatureEnabled: boolean
  temperature: number
  maxTokensEnabled: boolean
  maxTokens: number
  thinkModeEnabled: boolean
  thinkMode: string
  thinkEffortEnabled: boolean
  thinkEffort: string
}

/** 全局设置：对所有模型生效 */
export interface CompareGlobalConfig {
  systemPrompt: string
  input: string
  /** 流式输出 */
  stream: boolean
  /** 启用思考 */
  think: boolean
  /** 结果区每行列数 */
  column: number
  /**
   * 是否显示原文：true 时输出不做 Markdown 解析，直接给原始文本。
   * 纯展示偏好，不随请求下发；跟着工作区一起存。
   */
  raw: boolean
}

/** 本轮结果的状态 */
export type CompareResultState = 'idle' | 'connect' | 'streaming' | 'finish' | 'abort' | 'error'

/** 单个模型这一轮的输出（流式过程中只改字段，不换对象） */
export interface CompareResult {
  /** 调用明细标识：反馈按它定位；明细由服务端随流写库，本轮收尾回读后才有 */
  id?: any
  key: string
  model: string
  state: CompareResultState
  content: string
  reasoning: string
  finishReason: string
  error: string
  /** 实际发出的参数快照：回溯以它为准，而不是去关联可变的配置 */
  payload: Record<string, any>
  /** 发起时间 */
  createdTime: number
  /** 首个增量到达的时间，用于算首字耗时 */
  firstTokenTime: number
  /** 结束时间（成功、失败、中断都会写） */
  finishedTime: number
  /** 我给的反馈：positive-赞，negative-踩，空表示未反馈 */
  feedbackEmotion?: string
  feedbackTag?: string
  feedbackContent?: string
}

/**
 * 一次调试运行：一次提交扇出到多个模型，结果整体留档。
 *
 * 建档在前、明细在后，都是服务端写的：点发送时先建这一条（status=0 进行中），
 * 各模型的输出随各自的流落成明细，每写完一条重算一次摘要，全部到齐才算收口。
 */
export interface CompareRun {
  id: any
  title: string
  /** 0-进行中（还有模型的流没结束），1-全部完成，2-存在失败或中断 */
  status: number
  modelCount: number
  duration: number
  /** 结果摘要：几个完成、几个失败、几个中断（服务端写入时算好） */
  summary: string
  createdTime: number
  /**
   * 服务端此刻还有几条流在跑（详情接口才有，内存态）：
   * 页面据此分辨「进行中」是真在跑，还是只剩一条待收口的记录
   */
  active?: number
  /** 列表接口不返回，详情接口才有 */
  global?: CompareGlobalConfig
  models?: CompareModelConfig[]
  results?: CompareResult[]
}

/** 工作区：编辑器里当前这份配置（打开 / 保存时在前后端之间往来） */
export interface CompareWorkspace {
  models: CompareModelConfig[]
  global: CompareGlobalConfig
}

/**
 * 工作区摘要：列表行与保存后的回填。
 * `mine` 决定页面上能做什么——他人的共享工作区只能打开查看，保存要走「另存为」。
 */
export interface CompareWorkspaceSummary {
  id: any
  name: string
  modelCount: number
  /** 0-私有，1-已共享 */
  shared: number
  sharedTime: number
  createdTime: number
  updatedTime: number
  createdUid: number
  /** 是否本人所有 */
  mine: boolean
  createdUserInfo?: { name?: string }
}
