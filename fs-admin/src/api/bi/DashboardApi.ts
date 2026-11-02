import base from './Api'

export default {
  info(id: any, tips = {}) {
    return base.post('/dashboard/info', { id }, tips)
  },
  list(params: any = {}, tips = {}) {
    return base.post('/dashboard/list', params, tips)
  },
  save(data: any, tips = {}) {
    return base.post('/dashboard/save', data, tips)
  },
  delete(ids: any, tips = {}) {
    return base.post('/dashboard/delete', { ids }, tips)
  },
  config(tips = {}) {
    return base.post('/dashboard/config', {}, tips)
  },
  search(data: any = {}, tips = {}) {
    return base.post('/dashboard/search', data, tips)
  },
}
