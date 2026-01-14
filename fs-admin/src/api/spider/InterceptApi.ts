import base from './Api'

export default {
  info (param: any, tips = {}) {
      return base.post('/intercept/info', param, tips)
    },
  list (param: any, tips = {}) {
    return base.post('/intercept/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/intercept/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/intercept/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/intercept/save', param, tips)
  },
}
