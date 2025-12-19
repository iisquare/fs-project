import base from './Api'

export default {
  info (param: any, tips = {}) {
      return base.post('/site/info', param, tips)
    },
  list (param: any, tips = {}) {
    return base.post('/site/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/site/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/site/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/site/save', param, tips)
  },
}
