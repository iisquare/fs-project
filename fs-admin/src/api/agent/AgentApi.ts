import base from './Api'

export default {
  list (param: any, tips = {}) {
    return base.post('/agent/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/agent/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/agent/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/agent/save', param, tips)
  },
}
