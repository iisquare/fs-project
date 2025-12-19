import base from './Api'

export default {
  list (param: any, tips = {}) {
    return base.post('/knowledge/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/knowledge/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/knowledge/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/knowledge/save', param, tips)
  },
  embedding (param: any, tips = {}) {
    return base.post('/knowledge/embedding', param, tips)
  },
}
