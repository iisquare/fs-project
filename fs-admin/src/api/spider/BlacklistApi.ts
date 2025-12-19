import base from './Api'

export default {
  info (param: any, tips = {}) {
      return base.post('/blacklist/info', param, tips)
    },
  list (param: any, tips = {}) {
    return base.post('/blacklist/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/blacklist/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/blacklist/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/blacklist/save', param, tips)
  },
}
