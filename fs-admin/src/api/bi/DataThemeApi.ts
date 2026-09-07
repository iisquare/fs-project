import base from './Api'

export default {
  info(id: any, tips = {}) {
    return base.post('/dataTheme/info', { id }, tips)
  },
  list(params: any = {}, tips = {}) {
    return base.post('/dataTheme/list', params, tips)
  },
  save(data: any, tips = {}) {
    return base.post('/dataTheme/save', data, tips)
  },
  delete(ids: any, tips = {}) {
    return base.post('/dataTheme/delete', { ids }, tips)
  },
  config(tips = {}) {
    return base.post('/dataTheme/config', {}, tips)
  },
}
