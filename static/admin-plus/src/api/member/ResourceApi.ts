import base from './Api'

export default {
  list (param: any, tips = {}) {
    return base.post('/resource/list', param, tips)
  },
  tree (param: any, tips = {}) {
    return base.post('/resource/tree', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/resource/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/resource/config', {})
  },
  save (param: any, tips = {}) {
    return base.post('/resource/save', param, tips)
  }
}
