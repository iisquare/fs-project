import base from './Api'

export default {
  info(id: any, tips = {}) {
    return base.post('/rule/info', { id }, tips)
  },
  list(params: any = {}, tips = {}) {
    return base.post('/rule/list', params, tips)
  },
  save(data: any, tips = {}) {
    return base.post('/rule/save', data, tips)
  },
  delete(ids: any, tips = {}) {
    return base.post('/rule/delete', { ids }, tips)
  },
  config(tips = {}) {
    return base.post('/rule/config', {}, tips)
  },
}
