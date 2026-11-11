import base from './Api'

export default {
  list (param: any, tips = {}) {
    return base.post('/apiKey/list', param, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/apiKey/save', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/apiKey/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/apiKey/config', {}, tips)
  },
}
