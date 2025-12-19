import base from './Api'

export default {
  info (param: any, tips = {}) {
      return base.post('/template/info', param, tips)
    },
  list (param: any, tips = {}) {
    return base.post('/template/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/template/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/template/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/template/save', param, tips)
  },
  publish (param: any, tips = {}) {
    return base.post('/template/publish', param, tips)
  },
  clear (param: any, tips = {}) {
    return base.post('/template/clear', param, tips)
  },
}
