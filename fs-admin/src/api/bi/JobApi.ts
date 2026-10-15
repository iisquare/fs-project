import base from './Api'

export default {
  info(id: any, tips = {}) {
    return base.post('/job/info', { id }, tips)
  },
  list(params: any = {}, tips = {}) {
    return base.post('/job/list', params, tips)
  },
  delete(ids: any, tips = {}) {
    return base.post('/job/delete', { ids }, tips)
  },
  config(tips = {}) {
    return base.post('/job/config', {}, tips)
  },
  run(data: any, tips = {}) {
    return base.post('/job/run', data, tips)
  },
  cancel(data: any, tips = {}) {
    return base.post('/job/cancel', data, tips)
  },
  restart(data: any, tips = {}) {
    return base.post('/job/restart', data, tips)
  },
}
