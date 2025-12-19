import base from './Api'

export default {
  info (param: any, tips = {}) {
      return base.post('/whitelist/info', param, tips)
    },
  list (param: any, tips = {}) {
    return base.post('/whitelist/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/whitelist/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/whitelist/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/whitelist/save', param, tips)
  },
}
