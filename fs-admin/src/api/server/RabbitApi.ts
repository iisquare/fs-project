import api from '@/core/Api'

export default {
  get (url: string, params = {}, tips = {}, config = {}) {
    return api.get('/worker' + url, params, tips, config)
  },
  post (url: string, data = {}, tips = {}, config = {}) {
    return api.post('/worker' + url, data, tips, config)
  },
  taskNodes (param: any, tips = {}) {
    return this.get('/task/nodes', param, tips)
  },
  taskSubmit (param: any, tips = {}) {
    return this.post('/task/submit', param, tips)
  },
  taskList (param: any, tips = {}) {
    return this.post('/task/list', param, tips)
  },
  taskRemove (param: any, tips = {}) {
    return this.post('/task/remove', param, tips)
  },
  taskStart (param: any, tips = {}) {
    return this.post('/task/start', param, tips)
  },
  taskStop (param: any, tips = {}) {
    return this.post('/task/stop', param, tips)
  },
  taskRebalance (param: any, tips = {}) {
    return this.post('/task/rebalance', param, tips)
  },
  taskStartAll (param: any, tips = {}) {
    return this.post('/task/startAll', param, tips)
  },
  taskStopAll (param: any, tips = {}) {
    return this.post('/task/stopAll', param, tips)
  }
}
