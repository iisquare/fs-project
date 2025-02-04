import base from './Api'

export default {
  list (param: any, tips = {}) {
    return base.post('/serverEndpoint/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/serverEndpoint/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/serverEndpoint/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/serverEndpoint/save', param, tips)
  },
}
