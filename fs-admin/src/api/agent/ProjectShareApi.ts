import base from './Api'

/**
 * 分享的项目（管理端）：所有用户从客户端发布上来的项目，
 * 一个项目下挂着若干版本记录（见 ProjectVersionApi）。
 */
export default {
  info (id: any, tips = {}) {
    return base.get('/projectShare/info', { id }, tips)
  },
  list (param: any, tips = {}) {
    return base.post('/projectShare/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/projectShare/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/projectShare/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/projectShare/save', param, tips)
  },
}
