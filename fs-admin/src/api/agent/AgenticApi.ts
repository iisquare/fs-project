import base from './Api'

export default {
  list (param: any, tips = {}) {
    return base.post('/agentic/list', param, tips)
  },
  info (id: any, tips = {}) {
    return base.get('/agentic/info', { id }, tips)
  },
  /**
   * 用户对话页可用的编排应用：已发布、状态启用，且授权角色命中当前用户
   */
  authorized (tips = {}) {
    return base.post('/agentic/authorized', {}, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/agentic/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/agentic/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/agentic/save', param, tips)
  },
  publish (param: any, tips = {}) {
    return base.post('/agentic/publish', param, tips)
  },
  run (param: any, tips = {}) {
    return base.post('/agentic/run', param, tips)
  },
  invoke (param: any, tips = {}) {
    return base.post('/agentic/invoke', param, tips)
  },
  /**
   * 调试运行的文件上传：走文件服务存储，返回文件标识、原始名称、类型等信息
   */
  upload (file: File, tips = {}) {
    return base.form('/agentic/upload', { file }, tips)
  },
  /**
   * 运行日志列表（不返回入参、输出与步骤等大字段）
   */
  logList (param: any, tips = {}) {
    return base.post('/agentic/logList', param, tips)
  },
  /**
   * 运行日志详情：含入参、输出与逐节点执行步骤
   */
  logInfo (id: any, tips = {}) {
    return base.post('/agentic/logInfo', { id }, tips)
  },
  /**
   * 删除运行日志
   */
  logDelete (ids: any, tips = {}) {
    return base.post('/agentic/logDelete', { ids }, tips)
  },
  /**
   * 会话列表：调试运行与发布应用的对话历史
   */
  chatList (param: any, tips = {}) {
    return base.post('/agentic/chatList', param, tips)
  },
  /**
   * 会话详情：消息列表与每轮运行日志
   */
  chatInfo (id: any, tips = {}) {
    return base.post('/agentic/chatInfo', { id }, tips)
  },
  /**
   * 删除会话（连同消息与运行日志）
   */
  chatDelete (ids: any, tips = {}) {
    return base.post('/agentic/chatDelete', { ids }, tips)
  },
  /**
   * 消息反馈：对助手回复点赞/点踩（可附标签与说明），再次提交同一情绪表示取消
   */
  chatFeedback (param: any, tips = {}) {
    return base.post('/agentic/chatFeedback', param, tips)
  },
  /**
   * 流程统计：会话数量与对话轮次按时间轴聚合，另出流程排名与用户排名；
   * 同时返回对话类型分布与执行状态分布（状态分布不带状态筛选）；
   * 支持时间、流程、用户、对话类型、对话状态、执行状态（成功 / 失败）与时间聚合层级筛选
   */
  statistic (param: any = {}, tips = {}) {
    return base.post('/agentic/statistic', param, tips)
  },
}
