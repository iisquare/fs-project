import base from './Api'

/**
 * 模型对比调试的接口：除流式推理外都走这里。
 *
 * 流式推理单列在 `@/api/agent/streams` 的 `chatCompare`（/compare/stream）：
 * 一次请求只跑一个模型，参数逐模型独立，所以由页面并发发起、逐卡展示。
 */
export default {
  /** 可调试的模型清单：返回 `{ rows: [{ name, ownedBy, contextLength }] }` */
  models (tips = {}) {
    return base.post('/compare/models', {}, tips)
  },
  /** 可见的工作区列表：我的 + 他人共享的，每行带 mine 标记 */
  workspaceList (tips = {}) {
    return base.post('/compare/workspaceList', {}, tips)
  },
  /** 打开某个工作区：含 models 与 global */
  workspaceInfo (id: any, tips = {}) {
    return base.post('/compare/workspaceInfo', { id }, tips)
  },
  /** 保存工作区：带 id 是更新，不带是新建；只能保存自己的 */
  workspaceSave (param: any, tips = {}) {
    return base.post('/compare/workspaceSave', param, tips)
  },
  /** 共享 / 收回共享：只有属主能动 */
  workspaceShare (ids: any, shared: boolean, tips = {}) {
    return base.post('/compare/workspaceShare', { ids, shared }, tips)
  },
  /** 删除工作区：只作用于自己的，对比记录不受影响 */
  workspaceDelete (ids: any, tips = {}) {
    return base.post('/compare/workspaceDelete', { ids }, tips)
  },
  /** 运行记录列表：按标题检索、分页；不含设置、模型参数与输出等大字段 */
  runList (param: any, tips = {}) {
    return base.post('/compare/runList', param, tips)
  },
  /** 运行记录详情：含各模型的参数与输出，用于整条恢复 */
  runInfo (id: any, tips = {}) {
    return base.post('/compare/runInfo', { id }, tips)
  },
  /** 保存一次运行记录：{ title, global, models, results } */
  runSave (param: any, tips = {}) {
    return base.post('/compare/runSave', param, tips)
  },
  /** 删除运行记录 */
  runDelete (ids: any, tips = {}) {
    return base.post('/compare/runDelete', { ids }, tips)
  },
  /** 清空我的全部运行记录（列表有分页，页面拿不到全部 id） */
  runClear (tips = {}) {
    return base.post('/compare/runClear', {}, tips)
  },
  /** 模型输出反馈：赞 / 踩，按 callId 定位某一次输出 */
  feedback (param: any, tips = {}) {
    return base.post('/compare/feedback', param, tips)
  },
  /** 模型统计：支持时间 / 模型 / 用户筛选；调用量按模型、按用户各一张，评价与评分来自反馈 */
  statistic (param: any = {}, tips = {}) {
    return base.post('/compare/statistic', param, tips)
  },
}
