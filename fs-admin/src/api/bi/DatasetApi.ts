import base from './Api'

export default {
  info(id: any, tips = {}) {
    return base.post('/dataset/info', { id }, tips)
  },
  byName(name: any, tips = {}) {
    return base.post('/dataset/byName', { name }, tips)
  },
  byNames(names: any[], tips = {}) {
    return base.post('/dataset/byNames', { names }, tips)
  },
  trigger(id: any, tips = {}) {
    return base.post('/dataset/trigger', { id }, tips)
  },
  list(params: any = {}, tips = {}) {
    return base.post('/dataset/list', params, tips)
  },
  save(data: any, tips = {}) {
    return base.post('/dataset/save', data, tips)
  },
  delete(ids: any, tips = {}) {
    return base.post('/dataset/delete', { ids }, tips)
  },
  config(tips = {}) {
    return base.post('/dataset/config', {}, tips)
  },
  columns(id: any, tips = {}) {
    return base.post('/dataset/columns', { id }, tips)
  },
  query(params: any = {}, tips = {}) {
    return base.form('/dataset/query', params, tips)
  },
}
