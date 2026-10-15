import base from './Api'

/**
 * 知识库：文档、分段与检索配置都在服务端维护，桌面端只做检索与引用，
 * 不在本地新建知识库。
 */
export default {
  /** 知识库列表：项目里选择引用时取用，仅已启用的参与召回 */
  list(param: any, tips = {}) {
    return base.post('/knowledge/list', param, tips)
  }
}
