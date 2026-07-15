import base from './Api'

export default {
  info(param: any, tips = {}) {
    return base.post('/print/info', param, tips)
  },
  list(param: any, tips = {}) {
    return base.post('/print/list', param, tips)
  },
  delete(ids: any, tips = {}) {
    return base.post('/print/delete', { ids }, tips)
  },
  config(tips = {}) {
    return base.post('/print/config', {}, tips)
  },
  save(param: any, tips = {}) {
    return base.post('/print/save', param, tips)
  },
}