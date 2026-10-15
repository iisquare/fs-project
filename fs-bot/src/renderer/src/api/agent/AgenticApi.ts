import base from './Api'

/** 编排应用接口：桌面端只用来取「可作为工具引用」的已发布应用清单 */
export default {
  /** 当前用户可用的已发布应用（授权角色命中）：项目里把编排应用作为一种工具引用时选择 */
  authorized(tips = {}) {
    return base.post('/agentic/authorized', {}, tips)
  }
}
