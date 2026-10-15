import base from './Api'

/**
 * 项目分享（客户端）：把本地项目配置发布到服务端，也浏览别人分享的项目。
 *
 * 接口挂在服务端 agent 模块的 bot 下（权限键 `agent:bot:`，与模型清单、模型对话同一套权限），
 * 项目配置由客户端序列化成 JSON 提交，服务端按「项目 + 版本」存放并只负责权限与存储。
 */
export default {
  /** 我分享过的项目：含版本数量与最新版本，再次分享时从这里选目标 */
  mine(param: any = {}, tips = {}) {
    return base.post('/bot/project/mine', param, tips)
  },
  /** 浏览分享的项目：只返回已启用且授权命中的项目（带 mine 标记） */
  browse(param: any = {}, tips = {}) {
    return base.post('/bot/project/browse', param, tips)
  },
  /**
   * 某个分享项目的版本清单：不含配置原文，按页取（服务端默认每页 20、上限 100），
   * `name` 按版本号模糊检索（下拉面板的检索框用它，版本再多也不必把全部取回前端过滤）
   */
  versions(
    projectId: number,
    param: { page?: number; pageSize?: number; name?: string } = {},
    tips = {}
  ) {
    return base.post('/bot/project/versions', Object.assign({ projectId }, param), tips)
  },
  /** 单个版本详情：带项目配置原文，导出与创建本地项目都按它落地 */
  version(id: number, tips = {}) {
    return base.post('/bot/project/version', { id }, tips)
  },
  /** 发布一个版本：projectId 为 0 时按名称新建分享项目，否则追加到已有项目 */
  publish(param: any, tips = {}) {
    return base.post('/bot/project/publish', param, tips)
  }
}
