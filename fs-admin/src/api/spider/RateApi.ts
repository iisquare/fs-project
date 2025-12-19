import base from './Api'

export default {
  info (param: any, tips = {}) {
      return base.post('/rate/info', param, tips)
    },
  list (param: any, tips = {}) {
    return base.post('/rate/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/rate/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/rate/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/rate/save', param, tips)
  },
  publish (param: any, tips = {}) {
    return base.post('/rate/publish', param, tips)
  },
  clear (param: any, tips = {}) {
    return base.post('/rate/clear', param, tips)
  },
}
