import base from './Api'

export default {
  info(id: any, tips = {}) {
    return base.post('/datasource/info', { id }, tips)
  },
  list(params: any = {}, tips = {}) {
    return base.post('/datasource/list', params, tips)
  },
  save(data: any, tips = {}) {
    return base.post('/datasource/save', data, tips)
  },
  delete(ids: any, tips = {}) {
    return base.post('/datasource/delete', { ids }, tips)
  },
  config(tips = {}) {
    return base.post('/datasource/config', {}, tips)
  },
  test(data: any, tips = {}) {
    return base.post('/datasource/test', data, tips)
  },
}
