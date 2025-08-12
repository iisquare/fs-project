import base from './Api'

export default {
  info (param: any, tips = {}) {
    return base.post('/formData/info', param, tips)
  },
  list (param: any, tips = {}) {
    return base.post('/formData/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/formData/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/formData/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/formData/save', param, tips)
  }
}
