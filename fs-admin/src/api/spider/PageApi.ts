import base from './Api'

export default {
  info (param: any, tips = {}) {
      return base.post('/page/info', param, tips)
    },
  list (param: any, tips = {}) {
    return base.post('/page/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/page/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/page/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/page/save', param, tips)
  },
}
