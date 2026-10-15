import base from './Api'

/** 分享项目的版本记录（管理端）：版本号、版本说明与配置原文 */
export default {
  info (id: any, tips = {}) {
    return base.get('/projectVersion/info', { id }, tips)
  },
  list (param: any, tips = {}) {
    return base.post('/projectVersion/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/projectVersion/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/projectVersion/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/projectVersion/save', param, tips)
  },
}
