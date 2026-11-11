import base from './Api'

export default {
  info(id: any, tips = {}) {
    return base.post('/entry/info', { id }, tips)
  },
  list(params: any = {}, tips = {}) {
    return base.post('/entry/list', params, tips)
  },
  save(data: any, tips = {}) {
    return base.post('/entry/save', data, tips)
  },
  delete(ids: any, tips = {}) {
    return base.post('/entry/delete', { ids }, tips)
  },
  config(tips = {}) {
    return base.post('/entry/config', {}, tips)
  },
  domains(tips = {}) {
    return base.post('/entry/domains', {}, tips)
  },
}
