import base from './Api'

export default {
  info(id: any, tips = {}) {
    return base.post('/visualize/info', { id }, tips)
  },
  list(params: any = {}, tips = {}) {
    return base.post('/visualize/list', params, tips)
  },
  save(data: any, tips = {}) {
    return base.post('/visualize/save', data, tips)
  },
  delete(ids: any, tips = {}) {
    return base.post('/visualize/delete', { ids }, tips)
  },
  config(tips = {}) {
    return base.post('/visualize/config', {}, tips)
  },
  search(data: any = {}, tips = {}) {
    return base.post('/visualize/search', data, tips)
  },
}
