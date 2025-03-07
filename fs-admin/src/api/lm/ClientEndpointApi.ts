import base from './Api'

export default {
  list (param: any, tips = {}) {
    return base.post('/clientEndpoint/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/clientEndpoint/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/clientEndpoint/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/clientEndpoint/save', param, tips)
  },
}
