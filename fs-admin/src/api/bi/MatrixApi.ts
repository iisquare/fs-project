import base from './Api'

export default {
  info(id: any, tips = {}) {
    return base.post('/matrix/info', { id }, tips)
  },
  list(params: any = {}, tips = {}) {
    return base.post('/matrix/list', params, tips)
  },
  save(data: any, tips = {}) {
    return base.post('/matrix/save', data, tips)
  },
  delete(ids: any, tips = {}) {
    return base.post('/matrix/delete', { ids }, tips)
  },
  config(tips = {}) {
    return base.post('/matrix/config', {}, tips)
  },
  search(data: any = {}, tips = {}) {
    return base.post('/matrix/search', data, tips)
  },
}
