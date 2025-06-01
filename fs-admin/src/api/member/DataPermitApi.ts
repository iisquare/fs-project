import base from './Api'

export default {
  info (id: any, tips = {}) {
    return base.get('/dataPermit/info', { id }, tips)
  },
  list (param: any, tips = {}) {
    return base.post('/dataPermit/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/dataPermit/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/dataPermit/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/dataPermit/save', param, tips)
  }
}
